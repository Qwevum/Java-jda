package Qwevum.television.music;

import net.dv8tion.jda.api.audio.AudioModuleConfig;
import net.dv8tion.jda.api.audio.dave.DaveSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Discord only accepts end-to-end encrypted (DAVE) voice connections. The JDAVE library that
 * implements this needs Java 25, while the rest of the bot runs on Java 21. JDAVE is therefore
 * loaded by name only when the bot is running on Java 25 or newer; on older Java versions the
 * bot still starts, but music commands are turned off.
 */
public final class DaveSupport {
    public static final int REQUIRED_JAVA_VERSION = 25;

    private static final Logger LOGGER = LoggerFactory.getLogger(DaveSupport.class);
    private static final String FACTORY_CLASS = "club.minnced.discord.jdave.interop.JDaveSessionFactory";
    private static final DaveSessionFactory FACTORY = loadFactory();

    private DaveSupport() {}

    private static DaveSessionFactory loadFactory() {
        int javaVersion = Runtime.version().feature();

        if (javaVersion < REQUIRED_JAVA_VERSION) {
            LOGGER.warn("Running on Java {}. Music needs Java {} or newer, so music commands are disabled.",
                    javaVersion, REQUIRED_JAVA_VERSION);
            return null;
        }

        try {
            DaveSessionFactory factory = (DaveSessionFactory) Class.forName(FACTORY_CLASS)
                    .getDeclaredConstructor()
                    .newInstance();
            LOGGER.info("Voice encryption (DAVE) enabled, music commands are available.");
            return factory;
        } catch (ReflectiveOperationException | LinkageError | ClassCastException e) {
            LOGGER.error("Could not load JDAVE, music commands are disabled.", e);
            return null;
        }
    }

    public static boolean isAvailable() {
        return FACTORY != null;
    }

    public static AudioModuleConfig audioModuleConfig() {
        AudioModuleConfig config = new AudioModuleConfig();
        return FACTORY == null ? config : config.withDaveSessionFactory(FACTORY);
    }

    public static String unavailableMessage() {
        return "Music is unavailable: Discord voice needs Java " + REQUIRED_JAVA_VERSION
                + " on the bot's server, but it is running Java " + Runtime.version().feature() + ".";
    }
}
