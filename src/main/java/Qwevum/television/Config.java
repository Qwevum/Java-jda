package Qwevum.television;

import io.github.cdimascio.dotenv.Dotenv;

public class Config {

    // Reads .env if present, and falls back to real environment variables
    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

    public static String get(String key) {
        return dotenv.get(key.toUpperCase());
    }

    public static String get(String key, String defaultValue) {
        String value = get(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public static String prefix() {
        return get("PREFIX", "!");
    }
}
