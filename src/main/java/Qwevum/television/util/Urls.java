package Qwevum.television.util;

import java.net.URI;
import java.net.URISyntaxException;

public final class Urls {
    private Urls() {}

    /** True only for real web links, so plain words like "despacito" are treated as a search. */
    public static boolean isHttpUrl(String text) {
        try {
            URI uri = new URI(text);
            String scheme = uri.getScheme();

            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) && uri.getHost() != null;
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
