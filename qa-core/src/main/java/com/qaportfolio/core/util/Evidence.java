package com.qaportfolio.core.util;

import com.qaportfolio.core.driver.DriverManager;
import io.qameta.allure.Allure;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Deliberate (non-failure) screenshots, e.g. visual evidence per viewport in cross-browser runs. */
public final class Evidence {

    private Evidence() {
    }

    public static Path screenshot(String name) {
        byte[] png = ((TakesScreenshot) DriverManager.get()).getScreenshotAs(OutputType.BYTES);
        String spec = DriverManager.spec() == null ? "" : "_" + DriverManager.spec().label();
        String file = (name + spec).replaceAll("[^A-Za-z0-9_.@-]", "_") + ".png";
        Path out = Path.of("target", "screenshots", file);
        try {
            Files.createDirectories(out.getParent());
            Files.write(out, png);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Allure.addAttachment(name, "image/png", new ByteArrayInputStream(png), "png");
        return out;
    }
}
