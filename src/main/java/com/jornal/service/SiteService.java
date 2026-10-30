package com.jornal.service;

import com.jornal.dto.form.FrontForm;
import com.jornal.dto.form.SiteForm;
import com.jornal.dto.view.FrontDetail;
import com.jornal.dto.view.SiteDetail;
import com.jornal.dto.view.SiteSummary;
import com.jornal.security.Policies;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Obras y frentes del maestro. Cada metodo recibe el id del maestro autenticado
 * y solo encuentra recursos suyos: una obra ajena responde "no encontrada".
 */
public interface SiteService {

    @PreAuthorize(Policies.FOREMAN_SELF)
    List<SiteSummary> listSites(UUID foremanId);

    @PreAuthorize(Policies.FOREMAN_SELF)
    SiteDetail getSite(UUID foremanId, UUID siteId);

    @PreAuthorize(Policies.FOREMAN_SELF)
    SiteForm getSiteForm(UUID foremanId, UUID siteId);

    @PreAuthorize(Policies.FOREMAN_SELF)
    UUID createSite(UUID foremanId, SiteForm form);

    @PreAuthorize(Policies.FOREMAN_SELF)
    void updateSite(UUID foremanId, UUID siteId, SiteForm form);

    @PreAuthorize(Policies.FOREMAN_SELF)
    void changeStatus(UUID foremanId, UUID siteId, SiteAction action);

    @PreAuthorize(Policies.FOREMAN_SELF)
    UUID openFront(UUID foremanId, UUID siteId, FrontForm form);

    @PreAuthorize(Policies.FOREMAN_SELF)
    FrontDetail getFront(UUID foremanId, UUID frontId);
}
