package com.qaportfolio.a11y.tests;

import com.qaportfolio.a11y.support.Keyboard;
import com.qaportfolio.a11y.support.SauceDemo;
import com.qaportfolio.a11y.support.SauceDemo.Page;
import com.qaportfolio.core.BaseUiTest;
import com.qaportfolio.core.util.Evidence;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Labels, names, language, titles, alt text, headings, reflow. */
@Feature("Accessibility – semantics and structure")
public class SemanticStructureTest extends BaseUiTest {

    @Test(description = "A11Y-SEM-01 <html> declares a valid lang attribute")
    @Description("WCAG 3.1.1 Language of Page")
    public void htmlLangPresent() {
        SauceDemo.openLogin();
        String lang = (String) Keyboard.js("return document.documentElement.getAttribute('lang');");
        assertThat(lang).as("html[lang]").isNotBlank().matches("[a-zA-Z]{2,3}(-[a-zA-Z0-9]+)*");
    }

    @Test(description = "A11Y-SEM-02 Every page has a non-empty <title>")
    @Description("WCAG 2.4.2 Page Titled (presence)")
    public void pagesHaveTitle() {
        SauceDemo.login();
        assertThat(driver().getTitle()).isNotBlank();
    }

    @Test(groups = "known-defect",
            description = "A11Y-SEM-03 Page titles are unique and describe each page's purpose")
    @Description("WCAG 2.4.2 Page Titled — see DEF-A11Y-03")
    public void pageTitlesAreDescriptive() {
        Map<Page, String> titles = new LinkedHashMap<>();
        for (Page p : List.of(Page.LOGIN, Page.INVENTORY, Page.CART, Page.CHECKOUT_STEP_ONE)) {
            SauceDemo.goTo(p);
            titles.put(p, driver().getTitle());
        }
        assertThat(titles.values().stream().distinct().count())
                .as("distinct titles across pages %s", titles).isEqualTo(titles.size());
    }

    @Test(description = "A11Y-SEM-04 Login inputs expose an accessible name")
    @Description("WCAG 4.1.2 Name, Role, Value")
    public void loginInputsHaveAccessibleName() {
        SauceDemo.openLogin();
        for (By by : List.of(SauceDemo.USERNAME, SauceDemo.PASSWORD)) {
            WebElement el = driver().findElement(by);
            assertThat(el.getAccessibleName()).as("accessible name of %s", by).isNotBlank();
        }
        assertThat(driver().findElement(SauceDemo.LOGIN_BUTTON).getAccessibleName()).isEqualToIgnoringCase("Login");
    }

    @Test(groups = "known-defect",
            description = "A11Y-SEM-05 Form fields have a persistent visible <label> (not placeholder only)")
    @Description("WCAG 3.3.2 Labels or Instructions, 1.3.1 Info and Relationships — see DEF-A11Y-04")
    public void formFieldsHaveVisibleLabels() {
        List<String> unlabeled = new ArrayList<>();
        SauceDemo.openLogin();
        unlabeled.addAll(unlabeledInputs("login"));
        SauceDemo.goTo(Page.CHECKOUT_STEP_ONE);
        unlabeled.addAll(unlabeledInputs("checkout-step-one"));
        if (!unlabeled.isEmpty()) {
            Evidence.screenshot("A11Y-SEM-05_placeholder_only_fields");
        }
        assertThat(unlabeled).as("inputs relying on placeholder only").isEmpty();
    }

    @SuppressWarnings("unchecked")
    private List<String> unlabeledInputs(String page) {
        List<String> ids = (List<String>) Keyboard.js("""
                return [...document.querySelectorAll('input:not([type=hidden]):not([type=submit])')]
                  .filter(i => !(i.labels && i.labels.length) && !i.closest('label'))
                  .map(i => i.id || i.name);""");
        return ids.stream().map(id -> page + "#" + id).toList();
    }

    @Test(description = "A11Y-SEM-06 Product images have non-empty alt text matching the product")
    @Description("WCAG 1.1.1 Non-text Content")
    public void productImagesHaveAltText() {
        SauceDemo.login();
        List<WebElement> imgs = driver().findElements(By.cssSelector(".inventory_item img"));
        assertThat(imgs).isNotEmpty();
        for (WebElement img : imgs) {
            assertThat(img.getAttribute("alt")).as("alt of %s", img.getAttribute("src")).isNotBlank();
        }
    }

    @Test(description = "A11Y-SEM-07 Buttons and links have accessible names")
    @Description("WCAG 4.1.2 Name, Role, Value / 2.4.4 Link Purpose")
    @SuppressWarnings("unchecked")
    public void interactiveElementsNamed() {
        SauceDemo.login();
        List<WebElement> controls = driver().findElements(By.cssSelector("button, a[href], [role=button]"));
        List<String> unnamed = new ArrayList<>();
        for (WebElement c : controls) {
            if (c.isDisplayed() && c.getAccessibleName().isBlank()) {
                unnamed.add(c.getTagName() + "#" + c.getAttribute("id") + "." + c.getAttribute("class"));
            }
        }
        assertThat(unnamed).as("visible controls without accessible name").isEmpty();
    }

    @Test(description = "A11Y-SEM-08 Content reflows at 320 CSS px without horizontal scrolling")
    @Description("WCAG 1.4.10 Reflow")
    public void reflowAt320px() {
        driver().manage().window().setSize(new Dimension(320, 800));
        Map<String, Long> overflow = new LinkedHashMap<>();
        SauceDemo.openLogin();
        overflow.put("login", horizontalOverflow());
        SauceDemo.login();
        overflow.put("inventory", horizontalOverflow());
        Evidence.screenshot("A11Y-SEM-08_inventory_at_320px");
        assertThat(overflow.values()).as("horizontal overflow px per page %s (viewport %s)", overflow,
                Keyboard.js("return window.innerWidth;")).allMatch(px -> px <= 1);
    }

    private long horizontalOverflow() {
        return ((Number) Keyboard.js("return document.documentElement.scrollWidth - window.innerWidth;")).longValue();
    }
}
