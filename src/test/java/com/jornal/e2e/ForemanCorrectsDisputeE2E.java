package com.jornal.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.jornal.config.DemoDataSeeder;
import com.jornal.e2e.pages.DashboardPage;
import com.jornal.e2e.pages.LoginPage;
import com.jornal.e2e.pages.WorkDayFormPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.htmlunit.webdriver.MockMvcHtmlUnitDriverBuilder;

/**
 * Prueba de punta a punta con un navegador (HtmlUnit con la API de Selenium) y
 * Page Objects: el maestro ve la objecion en su panel, corrige la jornada y el
 * trabajador la confirma. Corre sobre MockMvc, sin levantar un servidor.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:e2e-${random.uuid};MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
    "app.security.bcrypt-strength=4",
    "spring.application.name=jornal-e2e"
})
class ForemanCorrectsDisputeE2E {

    @Autowired
    private MockMvc mvc;

    private WebDriver browser;

    @BeforeEach
    void openBrowser() {
        browser = MockMvcHtmlUnitDriverBuilder.mockMvcSetup(mvc).javascriptEnabled(true).build();
    }

    @AfterEach
    void closeBrowser() {
        browser.quit();
    }

    @Test
    void corregirJornadaObjetada_desdeElNavegador_elTrabajadorVeLaCorreccionYLaConfirma() {
        LoginPage login = LoginPage.open(browser);
        login.loginAs("maestro@jornal.co", DemoDataSeeder.DEMO_PASSWORD);
        assertThat(login.currentPath()).isEqualTo("/maestro");

        DashboardPage dashboard = new DashboardPage(browser);
        assertThat(dashboard.heading()).isEqualTo("Hola, Carlos Andr\u00e9s");
        assertThat(dashboard.pendingItems()).anyMatch(item -> item.contains("objet\u00f3 la jornada"));

        dashboard.openFirstUrgentItem();
        WorkDayFormPage form = new WorkDayFormPage(browser);
        assertThat(form.heading()).isEqualTo("Corregir jornada");
        form.checkOutAt("17:00").save();
        assertThat(browser.getCurrentUrl()).as("errores: %s", form.errors()).contains("/maestro/asignaciones/");
        assertThat(form.notice()).contains("Jornada corregida");

        browser.findElement(By.cssSelector(".logout-form button")).click();
        LoginPage.open(browser).loginAs("trabajador@jornal.co", DemoDataSeeder.DEMO_PASSWORD);
        DashboardPage workerHome = new DashboardPage(browser);
        assertThat(workerHome.heading()).isEqualTo("Hola, Yeison Fabi\u00e1n");
        assertThat(workerHome.balance()).startsWith("$");
        assertThat(workerHome.pendingItems()).anyMatch(item -> item.contains("por confirmar"));
    }
}
