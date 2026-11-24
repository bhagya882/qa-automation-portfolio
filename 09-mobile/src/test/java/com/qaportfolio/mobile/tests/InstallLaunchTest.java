package com.qaportfolio.mobile.tests;

import com.qaportfolio.mobile.screens.CatalogScreen;
import com.qaportfolio.mobile.support.BaseMobileTest;
import com.qaportfolio.mobile.support.Device;
import com.qaportfolio.mobile.support.Mobile;
import io.appium.java_client.appmanagement.ApplicationState;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class InstallLaunchTest extends BaseMobileTest {

    @Test(description = "MOB-01 App installs and launches to the catalog (main activity)")
    public void appInstalledAndLaunches() {
        assertThat(Mobile.android().isAppInstalled(Device.pkg())).as("app installed").isTrue();
        CatalogScreen catalog = new CatalogScreen().waitLoaded();
        assertThat(Mobile.android().currentActivity()).contains("MainActivity");
        assertThat(catalog.isDisplayed()).isTrue();
        assertThat(catalog.visibleProductTitles()).isNotEmpty().first().asString().contains("Sauce Labs");
        assertThat(Device.appState()).isEqualTo(ApplicationState.RUNNING_IN_FOREGROUND);
    }

    @Test(description = "MOB-02 App can be terminated and relaunched cleanly")
    public void terminateAndRelaunch() {
        new CatalogScreen().waitLoaded();
        assertThat(Mobile.android().terminateApp(Device.pkg())).isTrue();
        assertThat(Device.appState()).isIn(ApplicationState.NOT_RUNNING, ApplicationState.RUNNING_IN_BACKGROUND);
        Mobile.android().activateApp(Device.pkg());
        assertThat(new CatalogScreen().waitLoaded().isDisplayed()).isTrue();
    }
}
