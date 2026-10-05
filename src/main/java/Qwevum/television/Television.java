package Qwevum.television;

import Qwevum.television.music.DaveSupport;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.api.utils.messages.MessageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;

public class Television {
    private static final Logger LOGGER = LoggerFactory.getLogger(Television.class);

    private Television() {}

    public static void main(String[] args) {
        String token = Config.get("TOKEN");

        if (token == null || token.isBlank()) {
            LOGGER.error("No TOKEN found. Put TOKEN=your-bot-token in a .env file next to the jar.");
            System.exit(1);
        }

        // Never let the bot ping @everyone, roles or users unless a message explicitly allows it
        MessageRequest.setDefaultMentions(EnumSet.noneOf(Message.MentionType.class));

        try {
            start(token);
        } catch (InvalidTokenException e) {
            LOGGER.error("Discord rejected the TOKEN in your .env file. Copy a fresh one from the Developer Portal.");
            System.exit(1);
        }
    }

    private static void start(String token) {
        // createLight keeps memory use low; only voice states (needed for music) are cached
        JDABuilder.createLight(token,
                        GatewayIntent.GUILD_MEMBERS,
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.MESSAGE_CONTENT,
                        GatewayIntent.GUILD_MESSAGE_REACTIONS,
                        GatewayIntent.GUILD_VOICE_STATES)
                .enableCache(CacheFlag.VOICE_STATE)
                .setMemberCachePolicy(MemberCachePolicy.VOICE)
                .setAudioModuleConfig(DaveSupport.audioModuleConfig())
                .addEventListeners(new Listener())
                .setActivity(Activity.watching("Breaking Bad"))
                .build();
    }
}
