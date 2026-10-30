package com.jornal.e2e.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/** Formulario para registrar o corregir una jornada. */
public class WorkDayFormPage {

    private final WebDriver driver;

    public WorkDayFormPage(WebDriver driver) {
        this.driver = driver;
    }

    public String heading() {
        return driver.findElement(By.tagName("h1")).getText();
    }

    public WorkDayFormPage checkOutAt(String time) {
        // HtmlUnit 4 interpreta los campos type=time con reloj de 12 horas y descarta "17:00".
        // Solo en la prueba se cambia el campo a texto para enviar la hora tal como la envia un navegador real.
        ((JavascriptExecutor) driver).executeScript(
                "var f = document.getElementById('checkOutTime'); f.setAttribute('type', 'text');"
                        + "document.getElementById('checkOutTime').value = arguments[0];", time);
        return this;
    }

    public void save() {
        driver.findElement(By.cssSelector("form.panel button[type=submit]")).click();
    }

    public String notice() {
        return driver.findElement(By.cssSelector(".notice-success")).getText();
    }

    public String errors() {
        return driver.findElements(By.cssSelector(".error-summary, .field-invalid")).stream()
                .map(WebElement::getText).toList().toString();
    }
}
