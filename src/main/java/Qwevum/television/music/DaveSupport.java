package Qwevum.television.music;

import club.minnced.discord.jdave.interop.JDaveSessionFactory;
import net.dv8tion.jda.api.audio.AudioModuleConfig;
import net.dv8tion.jda.api.audio.dave.DaveSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Discord only accepts end-to-end encrypted (DAVE) voice connections, which the JDAVE library
 * implements. JDAVE uses a native library; if that can't be loaded on this machine, the bot still
 * starts, but music commands are turned off.
 */
public final class DaveSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(DaveSupport.class);
    private static final DaveSessionFactory FACTORY = loadFactory();

    private DaveSupport() {}

    private static DaveSessionFactory loadFactory() {
        try {
            DaveSessionFactory factory = new JDaveSessionFactory();
            LOGGER.info("Voice encryption (DAVE) enabled, music commands are available.");
            return factory;
        } catch (RuntimeException | LinkageError e) {
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
        return "Music is unavailable: Discord's voice encryption (DAVE) couldn't be loaded on the bot's server. "
                + "Check the bot's console for the error.";
    }
}
