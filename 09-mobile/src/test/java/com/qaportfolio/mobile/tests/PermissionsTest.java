package com.qaportfolio.mobile.tests;

import com.qaportfolio.mobile.screens.BaseScreen;
import com.qaportfolio.mobile.screens.CatalogScreen;
import com.qaportfolio.mobile.support.BaseMobileTest;
import com.qaportfolio.mobile.support.Device;
import com.qaportfolio.mobile.support.MobileEvidenceListener;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.appmanagement.ApplicationState;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/** Runtime permissions with autoGrantPermissions=false so the real system dialog appears. */
public class PermissionsTest extends BaseMobileTest {

    private static final By ALLOW = AppiumBy.id("com.android.permissioncontroller:id/permission_allow_foreground_only_button");
    private static final By ALLOW_ONE = AppiumBy.id("com.android.permissioncontroller:id/permission_allow_button");
    /** "Deny" on first request; "Deny & don't ask again" variant once the user has denied before. */
    private static final By DENY = By.xpath("//*[starts-with(@resource-id,"
            + "'com.android.permissioncontroller:id/permission_deny')]");
    private static final By DIALOG = AppiumBy.id("com.android.permissioncontroller:id/grant_dialog");

    @Override
    protected boolean autoGrantPermissions() {
        return false;
    }

    @Test(groups = "known-defect",
            description = "MOB-40 Denying camera permission on QR scanner is handled gracefully (see DEF-MOB-01)")
    public void denyCameraPermission() {
        new CatalogScreen().waitLoaded().openMenu().select("QR Code Scanner");
        new WebDriverWait(driver(), Duration.ofSeconds(10)).until(ExpectedConditions.presenceOfElementLocated(DIALOG));
        MobileEvidenceListener.capture("MOB-40_camera_permission_dialog");
        Device.pause(800); // dialog entry animation: taps during it are dropped
        new WebDriverWait(driver(), Duration.ofSeconds(5)).until(ExpectedConditions.elementToBeClickable(DENY)).click();
        Device.pause(3000);
        boolean promptBack = !driver().findElements(DIALOG).isEmpty();
        MobileEvidenceListener.capture("MOB-40_after_deny");
        assertThat(promptBack).as("camera permission prompt re-appears after Deny (re-request loop)").isFalse();
        assertThat(Device.appState()).as("no crash after deny").isEqualTo(ApplicationState.RUNNING_IN_FOREGROUND);
        assertThat(String.valueOf(Device.shell("dumpsys", "package", Device.pkg())))
                .contains("android.permission.CAMERA: granted=false");
    }

    @Test(description = "MOB-41 Granting location permission enables the Geo Location screen")
    public void grantLocationPermission() {
        new CatalogScreen().waitLoaded().openMenu().select("Geo Location");
        new WebDriverWait(driver(), Duration.ofSeconds(10)).until(ExpectedConditions.presenceOfElementLocated(DIALOG));
        By allow = driver().findElements(ALLOW).isEmpty() ? ALLOW_ONE : ALLOW;
        driver().findElement(allow).click();
        Device.pause(1500);
        assertThat(Device.appState()).isEqualTo(ApplicationState.RUNNING_IN_FOREGROUND);
        assertThat(String.valueOf(Device.shell("dumpsys", "package", Device.pkg())))
                .contains("android.permission.ACCESS_FINE_LOCATION: granted=true");
        MobileEvidenceListener.capture("MOB-41_geo_location_granted");
        assertThat(driver().findElements(BaseScreen.id("latitudeTV"))).as("latitude shown").isNotEmpty();
    }
}
