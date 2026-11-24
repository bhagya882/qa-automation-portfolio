package com.qaportfolio.saucedemo.tests;

import com.qaportfolio.saucedemo.model.Product;
import com.qaportfolio.saucedemo.model.SortOption;
import com.qaportfolio.saucedemo.model.Users;
import com.qaportfolio.saucedemo.pages.InventoryPage;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Feature("Product listing")
public class InventoryTest extends BaseSauceTest {

    @Test(groups = {"smoke", "regression"}, description = "Catalogue shows 6 products with name, description and price")
    @Story("Listing")
    @Severity(SeverityLevel.CRITICAL)
    public void catalogueShowsAllProducts() {
        List<Product> products = loginAsStandard().products();
        assertThat(products).hasSize(6);
        assertThat(products).allSatisfy(p -> {
            assertThat(p.name()).isNotBlank();
            assertThat(p.description()).isNotBlank();
            assertThat(p.price()).isPositive();
        });
    }

    @Test(groups = "regression", description = "Every product has its own image")
    @Story("Listing")
    @Severity(SeverityLevel.NORMAL)
    public void productImagesAreDistinct() {
        List<String> srcs = loginAsStandard().imageSources();
        assertThat(srcs).doesNotHaveDuplicates().noneMatch(s -> s.contains("sl-404"));
    }

    @Test(groups = "regression", description = "Default sort is Name (A to Z)")
    @Story("Sorting")
    @Severity(SeverityLevel.MINOR)
    public void defaultSortIsNameAscending() {
        InventoryPage inv = loginAsStandard();
        assertThat(inv.activeSortLabel()).isEqualTo(SortOption.NAME_ASC.label());
        assertThat(inv.productNames()).isSorted();
    }

    @Test(groups = "regression", description = "Sort by name Z to A")
    @Story("Sorting")
    @Severity(SeverityLevel.NORMAL)
    public void sortByNameDescending() {
        List<String> names = loginAsStandard().sortBy(SortOption.NAME_DESC).productNames();
        assertThat(names).isSortedAccordingTo(Comparator.reverseOrder());
    }

    @Test(groups = "regression", description = "Sort by price low to high")
    @Story("Sorting")
    @Severity(SeverityLevel.NORMAL)
    public void sortByPriceAscending() {
        List<BigDecimal> prices = loginAsStandard().sortBy(SortOption.PRICE_ASC).prices();
        assertThat(prices).isSorted();
    }

    @Test(groups = "regression", description = "Sort by price high to low")
    @Story("Sorting")
    @Severity(SeverityLevel.NORMAL)
    public void sortByPriceDescending() {
        List<BigDecimal> prices = loginAsStandard().sortBy(SortOption.PRICE_DESC).prices();
        assertThat(prices).isSortedAccordingTo(Comparator.reverseOrder());
    }

    // ---- Known defects (seeded in SauceDemo demo users). See docs/defects.md ----

    @Test(groups = "known-defect", description = "DEF-04-001 problem_user: all product images are the same placeholder")
    @Issue("DEF-04-001")
    @Story("Listing")
    @Severity(SeverityLevel.NORMAL)
    public void problemUserImagesAreDistinct() {
        List<String> srcs = loginAs(Users.problem()).imageSources();
        assertThat(srcs).as("product image sources").doesNotHaveDuplicates();
    }

    @Test(groups = "known-defect", description = "DEF-04-002 problem_user: sorting by price has no effect")
    @Issue("DEF-04-002")
    @Story("Sorting")
    @Severity(SeverityLevel.NORMAL)
    public void problemUserCanSortByPrice() {
        List<BigDecimal> prices = loginAs(Users.problem()).sortBy(SortOption.PRICE_ASC).prices();
        assertThat(prices).as("prices after 'low to high'").isSorted();
    }

    @Test(groups = "known-defect", description = "DEF-04-003 visual_user: listed prices differ from the catalogue")
    @Issue("DEF-04-003")
    @Story("Listing")
    @Severity(SeverityLevel.CRITICAL)
    public void visualUserSeesCorrectPrices() {
        List<Product> expected = loginAsStandard().products();
        driver().manage().deleteAllCookies();
        List<Product> actual = loginAs(Users.visual()).products();
        assertThat(actual).as("visual_user product list").containsExactlyElementsOf(expected);
    }
}
