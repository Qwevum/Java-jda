package Qwevum.television.util;

import net.dv8tion.jda.api.EmbedBuilder;

public final class Embeds {
    private Embeds() {}

    public static EmbedBuilder defaultEmbed() {
        return new EmbedBuilder().setColor(0x0ee30e).setFooter("Television by Qwevum");
    }
}
