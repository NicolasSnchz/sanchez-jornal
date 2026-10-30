package com.jornal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jornal.config.LaborProperties;
import com.jornal.dto.form.FrontForm;
import com.jornal.dto.form.SiteForm;
import com.jornal.dto.view.SiteDetail;
import com.jornal.entity.Assignment;
import com.jornal.entity.ConstructionSite;
import com.jornal.entity.Foreman;
import com.jornal.entity.Trade;
import com.jornal.exception.ResourceNotFoundException;
import com.jornal.repository.ConstructionSiteRepository;
import com.jornal.repository.ForemanRepository;
import com.jornal.repository.WorkFrontRepository;
import com.jornal.service.impl.SiteServiceImpl;
import com.jornal.support.Clocks;
import com.jornal.support.TestData;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SiteServiceImplTest {

    @Mock
    private ConstructionSiteRepository siteRepository;
    @Mock
    private WorkFrontRepository frontRepository;
    @Mock
    private ForemanRepository foremanRepository;

    private SiteServiceImpl service;
    private Foreman foreman;

    @BeforeEach
    void setUp() {
        Clock clock = Clocks.fixedAtTestNow();
        service = new SiteServiceImpl(siteRepository, frontRepository, foremanRepository, new ViewMapper(clock),
                new WagePolicy(new LaborProperties(new BigDecimal("1750905"), 210)), clock);
        foreman = TestData.foreman();
    }

    @Test
    void createSite_fechaDeFinYaPasada_esRechazada() {
        when(foremanRepository.findById(foreman.getId())).thenReturn(Optional.of(foreman));
        SiteForm form = siteForm(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 28));

        assertThatThrownBy(() -> service.createSite(foreman.getId(), form)).extracting("code").isEqualTo("obra.fin.pasado");
        verify(siteRepository, never()).save(any());
    }

    @Test
    void createSite_valida_quedaPlaneadaYSeGuarda() {
        when(foremanRepository.findById(foreman.getId())).thenReturn(Optional.of(foreman));

        UUID id = service.createSite(foreman.getId(), siteForm(TestData.TODAY, LocalDate.of(2027, 2, 26)));

        assertThat(id).isNotNull();
        verify(siteRepository).save(any(ConstructionSite.class));
    }

    @Test
    void openFront_jornalBajoElMinimoLegal_noSeGuarda() {
        ConstructionSite site = TestData.siteInProgress(foreman);
        when(siteRepository.findByIdAndForemanId(site.getId(), foreman.getId())).thenReturn(Optional.of(site));
        FrontForm form = frontForm("60000");

        assertThatThrownBy(() -> service.openFront(foreman.getId(), site.getId(), form))
                .extracting("code").isEqualTo("frente.jornal.bajo.minimo");
        verify(frontRepository, never()).save(any());
    }

    @Test
    void getSite_obraDeOtroMaestro_respondeNoEncontrada() {
        UUID otherForeman = UUID.randomUUID();
        UUID siteId = UUID.randomUUID();
        when(siteRepository.findByIdAndForemanId(siteId, otherForeman)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSite(otherForeman, siteId))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("code").isEqualTo("obra.no.encontrada");
    }

    @Test
    void getSite_obraEnCursoSinPersonal_permiteFinalizarla() {
        ConstructionSite site = TestData.siteInProgress(foreman);
        when(siteRepository.findByIdAndForemanId(site.getId(), foreman.getId())).thenReturn(Optional.of(site));

        SiteDetail detail = service.getSite(foreman.getId(), site.getId());

        assertThat(detail.canFinish()).isTrue();
        assertThat(detail.canStart()).isFalse();
        assertThat(detail.canSuspend()).isTrue();
    }

    @Test
    void changeStatus_finalizarConInvitacionAbierta_esRechazado() {
        ConstructionSite site = TestData.siteInProgress(foreman);
        Assignment.invite(TestData.front(site), TestData.worker(), TestData.WAGE, TestData.TODAY,
                TestData.NOW);
        when(siteRepository.findByIdAndForemanId(site.getId(), foreman.getId())).thenReturn(Optional.of(site));

        assertThatThrownBy(() -> service.changeStatus(foreman.getId(), site.getId(), SiteAction.FINISH))
                .extracting("code").isEqualTo("obra.finalizar.asignaciones.activas");
    }

    private static SiteForm siteForm(LocalDate start, LocalDate end) {
        SiteForm form = new SiteForm();
        form.setName("Casa Duarte");
        form.setAddress("Calle 12 # 2E-45");
        form.setNeighborhood("Caobos");
        form.setCity("C\u00facuta");
        form.setStartDate(start);
        form.setEstimatedEndDate(end);
        return form;
    }

    private static FrontForm frontForm(String wage) {
        FrontForm form = new FrontForm();
        form.setTask("Fundida");
        form.setRequiredTrade(Trade.HELPER);
        form.setOpenings(2);
        form.setOfferedDailyWage(new BigDecimal(wage));
        form.setStartDate(TestData.TODAY);
        form.setEndDate(LocalDate.of(2026, 12, 18));
        return form;
    }
}
