package Qwevum.television.music;

import Qwevum.television.command.CommandContext;
import Qwevum.television.command.ICommand;
import Qwevum.television.music.lavaplayer.GuildMusicManager;
import Qwevum.television.music.lavaplayer.PlayerManager;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;

import java.net.URI;
import java.net.URISyntaxException;

public class PlayCommand implements ICommand {
    //private static final String EMOTE = "❌";
    private final EventWaiter waiter;

    public PlayCommand(EventWaiter waiter) {
        this.waiter = waiter;
        
    }


    @SuppressWarnings("ConstantConditions")
    @Override
    public void handle(CommandContext ctx) {
        final TextChannel channel = ctx.getEvent().getTextChannel();

        if (ctx.getArgs().isEmpty()){
            channel.sendMessage("Correct usage is `!play <youtube link>`").queue();
            return;
        }

        final Member self = ctx.getSelfMember();
        final GuildVoiceState selfVoiceState = self.getVoiceState();

        if (!selfVoiceState.inVoiceChannel()) {
            channel.sendMessage("I need to be in a voice channel for this to work.").queue();
            return;
        }

        final Member member = ctx.getMember();
        final GuildVoiceState memberVoiceState = member.getVoiceState();

        if (!memberVoiceState.inVoiceChannel()) {
            channel.sendMessage("You need to be in a voice channel for this command to work.").queue();
            return;
        }

        if (!memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
            channel.sendMessage("You need to be in the same voice channel as me for this to work.").queue();
            return;
        }

        String link = String.join(" ", ctx.getArgs());

        if (!isUrl(link)) {
            link = "soundCloudSearch:" + link;
        }

        PlayerManager.getInstance().loadAndPlay(channel, link);

        //final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(channel.getGuild());
        //musicManager.scheduler.queue(track);

        /*channel.sendMessage("Click ")
                .append(EMOTE)
                .append(" to stop music.")
                .queue(message -> {
                    message.addReaction(EMOTE).queue();

                    this.waiter.waitForEvent(
                            GuildMessageReactionAddEvent.class,
                            (e) -> e.getMessageIdLong() == message.getIdLong() && !e.getUser().isBot(),
                            (e) -> {
                                musicManager.scheduler.player.stopTrack();
                                musicManager.scheduler.queue.clear();
                                channel.sendMessage("The player has been stopped and the queue has been cleared.").queue();
                            }
                    );
                });*/

    }

    @Override
    public String getName() {
        return "play";
    }

    @Override
    public String getHelp() {
        return "Plays a song\n" +
                "Usage: `!play <SoundCloud link>`";
    }

    private boolean isUrl(String url){
        try{
            new URI(url);
            return true;
        } catch (URISyntaxException e){
            return false;
        }
    }
}
