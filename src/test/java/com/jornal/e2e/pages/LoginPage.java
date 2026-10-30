package com.jornal.e2e.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

/** Page Object de la pantalla de ingreso: la prueba habla de "ingresar", no de selectores. */
public class LoginPage {

    private final WebDriver driver;

    @FindBy(id = "email")
    private WebElement email;

    @FindBy(id = "password")
    private WebElement password;

    @FindBy(css = "form.form button[type=submit]")
    private WebElement submit;

    public LoginPage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    public static LoginPage open(WebDriver driver) {
        driver.get("http://localhost/login");
        return new LoginPage(driver);
    }

    public void loginAs(String user, String secret) {
        email.sendKeys(user);
        password.sendKeys(secret);
        submit.click();
    }

    public String currentPath() {
        return driver.getCurrentUrl().replace("http://localhost", "");
    }
}
