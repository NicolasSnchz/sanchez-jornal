/*
 * Jornal - comportamiento de la interfaz.
 * La CSP no permite estilos ni scripts en linea, por eso las barras de la
 * planilla traen su geometria en atributos data-* y aqui se aplica por CSSOM.
 * Todo es mejora progresiva: sin JavaScript la informacion sigue en texto.
 */
(function () {
    "use strict";

    function applyGeometry(root) {
        root.querySelectorAll("[data-start]").forEach(function (el) {
            el.style.left = el.dataset.start + "%";
        });
        root.querySelectorAll("[data-size]").forEach(function (el) {
            var size = el.dataset.size + "%";
            if (el.dataset.axis === "y") {
                el.style.height = size;
            } else {
                el.style.width = size;
            }
        });
    }

    /* Acciones que no se pueden deshacer piden confirmacion. */
    function confirmations() {
        document.querySelectorAll("form[data-confirm]").forEach(function (form) {
            form.addEventListener("submit", function (event) {
                if (!window.confirm(form.dataset.confirm)) {
                    event.preventDefault();
                    event.stopImmediatePropagation();
                }
            });
        });
    }

    /* Un doble clic no debe registrar dos veces el mismo pago o jornada. */
    function singleSubmit() {
        document.querySelectorAll("form").forEach(function (form) {
            form.addEventListener("submit", function (event) {
                if (event.defaultPrevented) {
                    return;
                }
                if (form.dataset.submitted === "true") {
                    event.preventDefault();
                    return;
                }
                form.dataset.submitted = "true";
                window.setTimeout(function () { form.dataset.submitted = "false"; }, 4000);
            });
        });
    }

    function closableNotices() {
        document.querySelectorAll("[data-dismiss]").forEach(function (button) {
            button.addEventListener("click", function () {
                var target = button.closest(".notice");
                if (target) {
                    target.remove();
                }
            });
        });
    }

    /* El numero de operacion solo aplica a transferencias y billeteras digitales. */
    function paymentReference() {
        var method = document.querySelector("[data-payment-method]");
        var reference = document.querySelector("[data-payment-reference]");
        if (!method || !reference) {
            return;
        }
        function sync() {
            var cash = method.value === "CASH";
            reference.hidden = cash;
        }
        method.addEventListener("change", sync);
        sync();
    }

    /* Mostrar u ocultar la clave mientras se escribe. */
    function passwordToggles() {
        document.querySelectorAll("[data-toggle-password]").forEach(function (button) {
            var input = document.getElementById(button.dataset.togglePassword);
            if (!input) {
                return;
            }
            button.hidden = false;
            button.addEventListener("click", function () {
                var show = input.type === "password";
                input.type = show ? "text" : "password";
                button.textContent = show ? button.dataset.hideLabel : button.dataset.showLabel;
                button.setAttribute("aria-pressed", String(show));
            });
        });
    }

    /* Imprimir el comprobante (o guardarlo como PDF desde el dialogo del navegador). */
    function printButtons() {
        document.querySelectorAll("[data-print]").forEach(function (button) {
            button.hidden = false;
            button.addEventListener("click", function () { window.print(); });
        });
    }

    document.addEventListener("DOMContentLoaded", function () {
        applyGeometry(document);
        confirmations();
        singleSubmit();
        closableNotices();
        paymentReference();
        passwordToggles();
        printButtons();
    });
})();
