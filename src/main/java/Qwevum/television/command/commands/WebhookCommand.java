package Qwevum.television.command.commands;

import Qwevum.television.Config;
import Qwevum.television.command.CommandContext;
import Qwevum.television.command.ICommand;
import net.dv8tion.jda.api.entities.IncomingWebhookClient;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.WebhookClient;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import java.util.EnumSet;
import java.util.List;

public class WebhookCommand implements ICommand {

    private IncomingWebhookClient client;

    @Override
    public void handle(CommandContext ctx) {
        final List<String> args = ctx.getArgs();
        final MessageChannel channel = ctx.getChannel();

        if (args.isEmpty()) {
            channel.sendMessage("Missing arguments").queue();
            return;
        }

        final IncomingWebhookClient webhook = getClient(ctx);

        if (webhook == null) {
            channel.sendMessage("The webhook isn't set up. Add WEBHOOK_URL to the bot's .env file.").queue();
            return;
        }

        final Member member = ctx.getMember();

        webhook.sendMessage(String.join(" ", args))
                .setUsername(member.getEffectiveName())
                .setAvatarUrl(member.getEffectiveAvatar().getUrl(512).replace(".gif", ".png"))
                // Stops people using the webhook to ping @everyone or roles
                .setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
                .queue();
    }

    private synchronized IncomingWebhookClient getClient(CommandContext ctx) {
        if (client == null) {
            String url = Config.get("webhook_url");

            if (url == null || url.isBlank()) {
                return null;
            }

            client = WebhookClient.createClient(ctx.getJDA(), url);
        }

        return client;
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
