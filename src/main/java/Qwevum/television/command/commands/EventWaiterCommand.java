package Qwevum.television.command.commands;

import Qwevum.television.command.CommandContext;
import Qwevum.television.command.ICommand;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;

import java.time.Duration;

public class EventWaiterCommand implements ICommand {
    private static final Emoji EMOTE = Emoji.fromUnicode("✔");

    @Override
    public void handle(CommandContext ctx) {
        final MessageChannel channel = ctx.getChannel();

        channel.sendMessage("react with " + EMOTE.getFormatted()).queue(message -> {
            message.addReaction(EMOTE).queue();

            // JDA's built-in replacement for JDA-Utilities' EventWaiter
            ctx.getJDA().listenOnce(MessageReactionAddEvent.class)
                    .filter((e) -> e.getMessageIdLong() == message.getIdLong()
                            && e.getMember() != null && !e.getMember().getUser().isBot())
                    .timeout(Duration.ofSeconds(5), () -> channel.sendMessage("You waited too long.").queue())
                    .subscribe((e) -> channel.sendMessageFormat("%s was the first to react", e.getMember().getAsMention()).queue());
        });
    }

    @Override
    public String getName() {
        return "eventwaiter";
    }

    @Override
    public String getHelp() {
        return "Just an eventwaiter example.";
    }
}
