package com.qaportfolio.mobile.screens;

import org.openqa.selenium.By;

public class LoginScreen extends BaseScreen {

    public static final By TITLE = id("loginTV");
    public static final By USERNAME = id("nameET");
    public static final By PASSWORD = id("passwordET");
    public static final By LOGIN = a11y("Tap to login with given credentials");
    public static final By USERNAME_ERROR = id("nameErrorTV");
    public static final By PASSWORD_ERROR = id("passwordErrorTV");

    public LoginScreen waitLoaded() {
        visible(TITLE);
        return this;
    }

    public void login(String user, String password) {
        type(USERNAME, user);
        type(PASSWORD, password);
        hideKeyboard();
        tap(LOGIN);
    }
}
