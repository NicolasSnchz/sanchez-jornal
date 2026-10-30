package com.jornal.service.impl;

import com.jornal.dto.form.FrontForm;
import com.jornal.dto.form.SiteForm;
import com.jornal.dto.view.FrontDetail;
import com.jornal.dto.view.SiteDetail;
import com.jornal.dto.view.SiteSummary;
import com.jornal.entity.Assignment;
import com.jornal.entity.ConstructionSite;
import com.jornal.entity.Foreman;
import com.jornal.entity.WorkFront;
import com.jornal.exception.BusinessRuleException;
import com.jornal.exception.ResourceNotFoundException;
import com.jornal.repository.ConstructionSiteRepository;
import com.jornal.repository.ForemanRepository;
import com.jornal.repository.WorkFrontRepository;
import com.jornal.service.SiteAction;
import com.jornal.service.SiteService;
import com.jornal.service.ViewMapper;
import com.jornal.service.WagePolicy;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SiteServiceImpl implements SiteService {

    private static final Logger log = LoggerFactory.getLogger(SiteServiceImpl.class);

    private final ConstructionSiteRepository siteRepository;
    private final WorkFrontRepository frontRepository;
    private final ForemanRepository foremanRepository;
    private final ViewMapper mapper;
    private final WagePolicy wagePolicy;
    private final Clock clock;

    public SiteServiceImpl(ConstructionSiteRepository siteRepository, WorkFrontRepository frontRepository,
                           ForemanRepository foremanRepository, ViewMapper mapper, WagePolicy wagePolicy,
                           Clock clock) {
        this.siteRepository = siteRepository;
        this.frontRepository = frontRepository;
        this.foremanRepository = foremanRepository;
        this.mapper = mapper;
        this.wagePolicy = wagePolicy;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SiteSummary> listSites(UUID foremanId) {
        return siteRepository.findByForemanIdOrderByCreatedAtDesc(foremanId).stream()
                .map(mapper::toSummary)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SiteDetail getSite(UUID foremanId, UUID siteId) {
        return mapper.toDetail(findSite(foremanId, siteId));
    }

    @Override
    @Transactional(readOnly = true)
    public SiteForm getSiteForm(UUID foremanId, UUID siteId) {
        return SiteForm.from(findSite(foremanId, siteId));
    }

    @Override
    public UUID createSite(UUID foremanId, SiteForm form) {
        Foreman foreman = foremanRepository.findById(foremanId)
                .orElseThrow(() -> new ResourceNotFoundException("maestro.no.encontrado"));
        if (form.getEstimatedEndDate().isBefore(LocalDate.now(clock))) {
            throw new BusinessRuleException("obra.fin.pasado");
        }
        ConstructionSite site = new ConstructionSite(foreman, form.toData(), LocalDateTime.now(clock));
        siteRepository.save(site);
        log.info("Obra {} creada por el maestro {}", site.getId(), foremanId);
        return site.getId();
    }

    @Override
    public void updateSite(UUID foremanId, UUID siteId, SiteForm form) {
        findSite(foremanId, siteId).update(form.toData());
        log.info("Obra {} actualizada", siteId);
    }

    @Override
    public void changeStatus(UUID foremanId, UUID siteId, SiteAction action) {
        ConstructionSite site = findSite(foremanId, siteId);
        switch (action) {
            case START -> site.start();
            case SUSPEND -> site.suspend();
            case RESUME -> site.resume();
            case FINISH -> site.finish();
        }
        log.info("Obra {} cambia a {}", siteId, site.getStatus());
    }

    @Override
    public UUID openFront(UUID foremanId, UUID siteId, FrontForm form) {
        ConstructionSite site = findSite(foremanId, siteId);
        if (form.getEndDate().isBefore(LocalDate.now(clock))) {
            throw new BusinessRuleException("frente.fin.pasado");
        }
        WorkFront.FrontData data = form.toData();
        // El jornal se valida antes de tocar la obra: si no cumple, la obra queda intacta.
        // Un horario sin minutos lo rechaza la entidad con su propio mensaje.
        if (data.scheduledMinutesPerDay() > 0) {
            wagePolicy.requireLegalWage(data.offeredDailyWage(), data.scheduledMinutesPerDay(),
                    "frente.jornal.bajo.minimo");
        }
        WorkFront front = site.openFront(data);
        frontRepository.save(front);
        log.info("Frente {} abierto en la obra {}", front.getId(), siteId);
        return front.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public FrontDetail getFront(UUID foremanId, UUID frontId) {
        WorkFront front = frontRepository.findByIdAndSiteForemanId(frontId, foremanId)
                .orElseThrow(() -> new ResourceNotFoundException("frente.no.encontrado"));
        ConstructionSite site = front.getSite();
        return new FrontDetail(site.getId(), site.getName(), site.getCity(), site.getStatus(),
                mapper.toSummary(front),
                front.getAssignments().stream()
                        .sorted(Comparator.comparing(Assignment::getInvitedAt).reversed())
                        .map(mapper::toRow)
                        .toList(),
                wagePolicy.minimumDailyWage(front.scheduledMinutesPerDay()));
    }

    private ConstructionSite findSite(UUID foremanId, UUID siteId) {
        return siteRepository.findByIdAndForemanId(siteId, foremanId)
                .orElseThrow(() -> new ResourceNotFoundException("obra.no.encontrada"));
    }
}
