package Qwevum.television;

import Qwevum.television.music.MusicButtons;
import Qwevum.television.music.lavaplayer.PlayerManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class Listener extends ListenerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(Listener.class);
    private final CommandManager manager = new CommandManager();

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        LOGGER.info("{} is ready in {} servers", event.getJDA().getSelfUser().getName(), event.getGuildTotalCount());
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        sendToGeneral(event.getGuild(), String.format("Welcome %s to %s",
                event.getUser().getName(), event.getGuild().getName()));
    }

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        sendToGeneral(event.getGuild(), String.format("Goodbye %s", event.getUser().getName()));
    }

    // TODO: let each server pick its own welcome channel instead of looking for "general"
    private void sendToGeneral(Guild guild, String message) {
        final List<TextChannel> channels = guild.getTextChannelsByName("general", true);

        if (channels.isEmpty() || !channels.get(0).canTalk()) {
            return;
        }

        channels.get(0).sendMessage(message).queue();
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        MusicButtons.handle(event);
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        User user = event.getAuthor();

        if (user.isBot() || event.isWebhookMessage() || !event.isFromGuild()) {
            return;
        }

        String prefix = Config.prefix();
        String raw = event.getMessage().getContentRaw();

        if (raw.equalsIgnoreCase(prefix + "shutdown")
                && user.getId().equals(Config.get("owner_id"))) {
            LOGGER.info("Shutdown requested by owner");
            event.getJDA().shutdown();
            PlayerManager.getInstance().shutdown();
            return;
        }

        manager.handle(event, prefix);
    }
}
