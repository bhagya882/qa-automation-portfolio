package com.qaportfolio.mobile.tests;

import com.qaportfolio.mobile.screens.CatalogScreen;
import com.qaportfolio.mobile.screens.ProductDetailsScreen;
import com.qaportfolio.mobile.support.BaseMobileTest;
import com.qaportfolio.mobile.support.Device;
import com.qaportfolio.mobile.support.Mobile;
import com.qaportfolio.mobile.support.MobileEvidenceListener;
import io.appium.java_client.appmanagement.ApplicationState;
import org.openqa.selenium.ScreenOrientation;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/** Orientation, background/foreground and network interruption. */
public class DeviceConditionsTest extends BaseMobileTest {

    @Test(description = "MOB-30 Device rotation (system setting) does not crash or lose screen state")
    public void deviceRotationKeepsState() {
        ProductDetailsScreen details = new CatalogScreen().waitLoaded().openProduct(0);
        String name = details.name();
        details.increaseQuantity();
        // Rotate the DEVICE (like a user turning the phone), independent of what the app allows
        Device.shell("settings", "put", "system", "accelerometer_rotation", "0");
        Device.shell("settings", "put", "system", "user_rotation", "1");
        Device.pause(2000);
        MobileEvidenceListener.capture("MOB-30_device_rotated_landscape");
        assertThat(Device.appState()).isEqualTo(ApplicationState.RUNNING_IN_FOREGROUND);
        ProductDetailsScreen afterRotate = new ProductDetailsScreen().waitLoaded();
        assertThat(afterRotate.name()).isEqualTo(name);
        assertThat(afterRotate.quantity()).as("quantity survives rotation").isEqualTo("2");
        Device.shell("settings", "put", "system", "user_rotation", "0");
    }

    @Test(groups = "known-defect", description = "MOB-33 App supports landscape orientation")
    public void appSupportsLandscape() {
        new CatalogScreen().waitLoaded();
        try {
            Mobile.android().rotate(ScreenOrientation.LANDSCAPE);
        } catch (RuntimeException e) {
            MobileEvidenceListener.capture("MOB-33_rotation_refused");
            throw new AssertionError("App refuses landscape (portrait-locked activity): "
                    + e.getMessage().lines().findFirst().orElse(""), e);
        }
        assertThat(Mobile.android().getOrientation()).isEqualTo(ScreenOrientation.LANDSCAPE);
    }

    @Test(description = "MOB-31 Sending the app to background and back keeps the cart")
    public void backgroundAndForeground() {
        ProductDetailsScreen details = new CatalogScreen().waitLoaded().openProduct(0);
        String name = details.name();
        details.addToCart();
        assertThat(details.cartCount()).isEqualTo("1");
        Mobile.android().runAppInBackground(Duration.ofSeconds(3));
        assertThat(Device.appState()).isEqualTo(ApplicationState.RUNNING_IN_FOREGROUND);
        ProductDetailsScreen resumed = new ProductDetailsScreen().waitLoaded();
        assertThat(resumed.name()).as("same screen after resume").isEqualTo(name);
        assertThat(resumed.cartCount()).as("cart kept after resume").isEqualTo("1");
    }

    @Test(description = "MOB-32 Network loss: app stays usable offline and recovers when network returns")
    public void networkInterruptionAndRecovery() {
        CatalogScreen catalog = new CatalogScreen().waitLoaded();
        Device.setNetwork(false);
        assertThat(Device.waitForNetwork(false, 15_000)).as("device went offline").isTrue();
        // Offline: local catalog/cart flows must keep working without crash
        ProductDetailsScreen details = catalog.openProduct(0).addToCart();
        assertThat(details.cartCount()).isEqualTo("1");
        new CatalogScreen().openMenu().select("WebView");
        Device.pause(1500);
        MobileEvidenceListener.capture("MOB-32_offline_webview_screen");
        assertThat(Device.appState()).as("app still running while offline").isEqualTo(ApplicationState.RUNNING_IN_FOREGROUND);

        Device.setNetwork(true);
        assertThat(Device.waitForNetwork(true, 30_000)).as("device network restored within 30 s").isTrue();
        assertThat(Device.appState()).isEqualTo(ApplicationState.RUNNING_IN_FOREGROUND);
    }

    @AfterMethod(alwaysRun = true)
    public void restoreDevice() {
        if (Mobile.active()) {
            try {
                Device.setNetwork(true);
                Device.shell("settings", "put", "system", "user_rotation", "0");
            } catch (RuntimeException ignored) {
                // best effort; session may be gone
            }
        }
    }
}
