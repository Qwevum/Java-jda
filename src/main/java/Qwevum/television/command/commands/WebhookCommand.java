package Qwevum.television.command.commands;

import Qwevum.television.Config;
import Qwevum.television.command.CommandContext;
import Qwevum.television.command.ICommand;
import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

import java.util.List;

public class WebhookCommand implements ICommand {

    private final WebhookClient client;

    public WebhookCommand() {
        WebhookClientBuilder builder = new WebhookClientBuilder(Config.get("webhook_url"));
        builder.setThreadFactory((job) -> {
            Thread thread = new Thread(job);
            thread.setName("Webhook-Thread");
            thread.setDaemon(true);
            return thread;
        });

        this.client = builder.build();
    }

    @Override
    public void handle(CommandContext ctx) {
        final List<String> args = ctx.getArgs();
        final TextChannel channel = ctx.getEvent().getTextChannel();

        if (args.isEmpty()){
            channel.sendMessage("Missing arguments").queue();
            return;
        }

        final User user = ctx.getAuthor();

        WebhookMessageBuilder builder = new WebhookMessageBuilder()
                .setUsername(ctx.getMember().getEffectiveName())
                .setAvatarUrl(user.getEffectiveAvatarUrl().replaceFirst("gif", "png") + "?size=512")
                .setContent(String.join(" ", args));

        client.send(builder.build());
    }

    @Override
    public String getName() {
        return "webhook";
    }

    @Override
    public String getHelp() {
        return "Send a webhook message as your name\n" +
                "Usage: `!webhook [message]`";
    }
}
