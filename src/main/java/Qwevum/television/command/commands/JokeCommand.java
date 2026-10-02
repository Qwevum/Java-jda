package Qwevum.television.command.commands;

import Qwevum.television.command.CommandContext;
import Qwevum.television.command.ICommand;
import Qwevum.television.util.Embeds;
import Qwevum.television.util.Http;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JokeCommand implements ICommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(JokeCommand.class);

    @Override
    public void handle(CommandContext ctx) {
        final MessageChannel channel = ctx.getChannel();

        Http.getJson("https://apis.duncte123.me/joke").thenAccept((json) -> {
            if (!json.getBoolean("success", false)) {
                throw new IllegalStateException("API returned " + json);
            }

            final DataObject data = json.getObject("data");
            final EmbedBuilder embed = Embeds.defaultEmbed()
                    .setTitle(data.getString("title"), data.getString("url", null))
                    .setDescription(data.getString("body"));

            channel.sendMessageEmbeds(embed.build()).queue();
        }).exceptionally((error) -> {
            LOGGER.warn("Joke API request failed: {}", error.toString());
            channel.sendMessage("Something went wrong, try again later").queue();
            return null;
        });
    }

    @Override
    public String getName() {
        return "joke";
    }

    @Override
    public String getHelp() {
        return "Shows a random joke";
    }
}
