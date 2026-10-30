package com.jornal.controller.web;

import com.jornal.controller.support.MessageArgs;
import com.jornal.controller.support.Notice;
import com.jornal.dto.form.PaymentForm;
import com.jornal.dto.form.ReasonForm;
import com.jornal.dto.view.PaymentRow;
import com.jornal.entity.PaymentMethod;
import com.jornal.entity.PaymentType;
import com.jornal.exception.BusinessRuleException;
import com.jornal.security.JornalUserDetails;
import com.jornal.service.AssignmentService;
import com.jornal.service.PaymentService;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Pagos en dos pasos: primero se calcula (sin registrar nada) y el maestro ve
 * el monto y el periodo; luego confirma. Asi nunca paga una cifra que no vio.
 */
@Controller
@RequestMapping("/maestro")
public class ForemanPaymentController {

    private final PaymentService paymentService;
    private final AssignmentService assignmentService;
    private final Clock clock;

    public ForemanPaymentController(PaymentService paymentService, AssignmentService assignmentService, Clock clock) {
        this.paymentService = paymentService;
        this.assignmentService = assignmentService;
        this.clock = clock;
    }

    @GetMapping("/asignaciones/{id}/pagos/nuevo")
    public String newPayment(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID id,
                             @RequestParam(name = "tipo") PaymentType type,
                             @RequestParam(name = "fin", required = false)
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd,
                             Model model) {
        PaymentForm form = new PaymentForm();
        form.setType(type);
        form.setPeriodEnd(type == PaymentType.SETTLEMENT && periodEnd == null ? LocalDate.now(clock) : periodEnd);
        return paymentView(me, id, form, model, true);
    }

    /** Recalcula el monto cuando el maestro cambia la fecha de corte. */
    @PostMapping("/asignaciones/{id}/pagos/calcular")
    public String recalculate(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID id,
                              @ModelAttribute("form") PaymentForm form, Model model) {
        return paymentView(me, id, form, model, true);
    }

    @PostMapping("/asignaciones/{id}/pagos")
    public String register(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID id,
                           @Valid @ModelAttribute("form") PaymentForm form, BindingResult result, Model model,
                           RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                PaymentRow payment = paymentService.register(me.id(), id, form);
                Notice.success(redirect, "pago.registrado", payment.receiptNumber(), payment.amount());
                return "redirect:/maestro/pagos/" + payment.id() + "/comprobante";
            } catch (BusinessRuleException e) {
                result.reject(e.getCode(), MessageArgs.format(e.getArgs()), null);
            }
        }
        return paymentView(me, id, form, model, false);
    }

    @PostMapping("/pagos/{paymentId}/anular")
    public String cancel(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID paymentId,
                         @Valid @ModelAttribute("reasonForm") ReasonForm form, BindingResult result,
                         RedirectAttributes redirect) {
        if (result.hasErrors()) {
            Notice.error(redirect, "validacion.motivo");
            return "redirect:/maestro/pagos/" + paymentId + "/comprobante";
        }
        UUID assignmentId = paymentService.cancel(me.id(), paymentId, form.getReason());
        Notice.success(redirect, "pago.anulado");
        return "redirect:/maestro/asignaciones/" + assignmentId + "#pagos";
    }

    @GetMapping("/pagos")
    public String month(@AuthenticationPrincipal JornalUserDetails me,
                        @RequestParam(name = "mes", required = false)
                        @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
                        Model model) {
        YearMonth current = YearMonth.now(clock);
        YearMonth selected = month == null || month.isAfter(current) ? current : month;
        model.addAttribute("month", paymentService.listMonth(me.id(), selected));
        return "foreman/payments";
    }

    @GetMapping("/pagos/{paymentId}/comprobante")
    public String receipt(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID paymentId, Model model) {
        model.addAttribute("receipt", paymentService.receiptForForeman(me.id(), paymentId));
        model.addAttribute("reasonForm", new ReasonForm());
        return "shared/receipt";
    }

    /**
     * Muestra el formulario con el calculo del pago. Si la regla de negocio no
     * deja pagar (por ejemplo, jornadas sin confirmar), el motivo se muestra en
     * lugar del monto y el boton de confirmar no aparece.
     */
    private String paymentView(JornalUserDetails me, UUID assignmentId, PaymentForm form, Model model,
                               boolean computeQuote) {
        model.addAttribute("form", form);
        model.addAttribute("a", assignmentService.getForForeman(me.id(), assignmentId, null));
        model.addAttribute("paymentMethods", PaymentMethod.values());
        // El anticipo no se precalcula: su monto lo digita el maestro.
        if (computeQuote && form.getType() != null && form.getType() != PaymentType.ADVANCE) {
            try {
                model.addAttribute("quote", paymentService.quote(me.id(), assignmentId, form));
            } catch (BusinessRuleException e) {
                model.addAttribute("quoteError", e.getCode());
                model.addAttribute("quoteErrorArgs", MessageArgs.format(e.getArgs()));
            }
        }
        return "foreman/payment-form";
    }
}
