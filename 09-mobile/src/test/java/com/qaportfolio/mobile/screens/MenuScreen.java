package com.qaportfolio.mobile.screens;

import org.openqa.selenium.By;

public class MenuScreen extends BaseScreen {

    public static final By MENU_LIST = id("menuRV");

    public MenuScreen waitLoaded() {
        visible(MENU_LIST);
        return this;
    }

    public void select(String itemText) {
        tap(text(itemText));
    }

    public LoginScreen openLogin() {
        select("Log In");
        return new LoginScreen().waitLoaded();
    }

    public CatalogScreen openCatalog() {
        select("Catalog");
        return new CatalogScreen().waitLoaded();
    }
}
