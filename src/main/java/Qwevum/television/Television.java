package Qwevum.television;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import javax.security.auth.login.LoginException;
import java.util.EnumSet;

public class Television {

    private Television() throws LoginException {
        EmbedUtils.setEmbedBuilder(()-> new EmbedBuilder().setColor(0x0ee30e).setFooter("Television by Qwevum"));


        EventWaiter waiter = new EventWaiter();

        JDABuilder.createDefault(Config.get("TOKEN"),
                        GatewayIntent.GUILD_MEMBERS,
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.GUILD_MESSAGE_REACTIONS,
                        GatewayIntent.GUILD_VOICE_STATES)
                .disableCache(EnumSet.of(
                        CacheFlag.CLIENT_STATUS,
                        CacheFlag.ACTIVITY,
                        CacheFlag.EMOTE
                ))
                .enableCache(CacheFlag.VOICE_STATE)
                .addEventListeners(new Listener(waiter), waiter)
            .setActivity(Activity.watching("Breaking Bad")).build();
    }

    public static void main(String[] args) throws LoginException {
        new Television();
    }

}