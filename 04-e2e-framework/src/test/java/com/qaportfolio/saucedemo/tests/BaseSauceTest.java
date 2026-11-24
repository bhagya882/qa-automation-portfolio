package com.qaportfolio.saucedemo.tests;

import com.qaportfolio.core.BaseUiTest;
import com.qaportfolio.saucedemo.model.User;
import com.qaportfolio.saucedemo.model.Users;
import com.qaportfolio.saucedemo.pages.InventoryPage;
import com.qaportfolio.saucedemo.pages.SauceDemo;
import io.qameta.allure.Epic;

@Epic("SauceDemo web shop")
public abstract class BaseSauceTest extends BaseUiTest {

    protected InventoryPage loginAs(User user) {
        return SauceDemo.open().loginAs(user);
    }

    protected InventoryPage loginAsStandard() {
        return loginAs(Users.standard());
    }
}
