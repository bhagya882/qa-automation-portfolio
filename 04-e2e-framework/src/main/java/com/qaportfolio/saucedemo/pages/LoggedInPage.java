package com.qaportfolio.saucedemo.pages;

import com.qaportfolio.core.pages.BasePage;
import com.qaportfolio.saucedemo.components.Header;

/** Any page that shows the app header. */
public abstract class LoggedInPage<T extends LoggedInPage<T>> extends BasePage {

    public Header header() {
        return new Header();
    }

    public abstract T waitUntilLoaded();
}
