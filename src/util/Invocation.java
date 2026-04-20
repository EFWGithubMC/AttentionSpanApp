package util;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public final class Invocation {

    private Invocation() {
    }

    public static void openResultsSubmissionPage() {
        openBrowser(Const.RESULTS_SUBMISSION_URL);
    }

    public static void openBrowser(String url) {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            throw new RuntimeException("Desktop browsing is not supported on this system.");
        }

        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (IOException e) {
            throw new RuntimeException("Failed to open browser for URL: " + url, e);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid URL: " + url, e);
        }
    }
}
