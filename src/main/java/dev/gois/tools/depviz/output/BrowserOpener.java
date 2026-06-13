package dev.gois.tools.depviz.output;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.Objects;

public class BrowserOpener {
    private final boolean enabled;

    public BrowserOpener() {
        this(true);
    }

    public BrowserOpener(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean open(URI uri) {
        Objects.requireNonNull(uri, "uri is required.");
        if (!enabled) {
            return false;
        }
        try {
            if (!Desktop.isDesktopSupported()) {
                return false;
            }
            Desktop desktop = Desktop.getDesktop();
            if (!desktop.isSupported(Desktop.Action.BROWSE)) {
                return false;
            }
            desktop.browse(uri);
            return true;
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }
}
