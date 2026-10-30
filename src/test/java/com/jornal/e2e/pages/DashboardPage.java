package com.jornal.e2e.pages;

import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/** Panel de inicio (maestro o trabajador) con su lista de pendientes. */
public class DashboardPage {

    private final WebDriver driver;

    public DashboardPage(WebDriver driver) {
        this.driver = driver;
    }

    public String heading() {
        return driver.findElement(By.tagName("h1")).getText();
    }

    public List<String> pendingItems() {
        return driver.findElements(By.cssSelector("ul.todo li .text strong")).stream().map(WebElement::getText).toList();
    }

    /** Abre la primera accion de un pendiente marcado como urgente (jornada objetada). */
    public void openFirstUrgentItem() {
        driver.findElement(By.cssSelector("ul.todo li.k-danger a.btn")).click();
    }

    public String balance() {
        return driver.findElement(By.cssSelector(".balance-hero .money-xl")).getText();
    }
}
