package com.qaportfolio.crossbrowser;

import com.qaportfolio.core.driver.Browser;
import com.qaportfolio.core.driver.BrowserSpec;

/** Human-readable reason recorded when a browser can't be launched on the current machine. */
final class Blocked {

    private Blocked() {
    }

    static String reason(BrowserSpec spec, Throwable e) {
        String hint = switch (spec.browser()) {
            case SAFARI -> "enable Safari remote automation once with `sudo safaridriver --enable` (macOS only)";
            case FIREFOX -> "install Firefox (CI runners have it) or run on a grid with -Dgrid.url";
            case EDGE -> "install Microsoft Edge or run on a grid with -Dgrid.url";
            case CHROME -> "install Google Chrome";
        };
        String first = String.valueOf(e.getMessage()).lines().findFirst().orElse("");
        return "BLOCKED: cannot start " + spec.label() + " on this machine – " + hint + ". Cause: "
                + e.getClass().getSimpleName() + ": " + first;
    }

    static boolean isSafari(BrowserSpec spec) {
        return spec.browser() == Browser.SAFARI;
    }
}
