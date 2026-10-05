package com.ootruffle.simplerewardclaim;

import net.minecraft.client.Minecraft;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Formatting;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

/**
 * Chat helpers built on the game's own text classes (Feather names), so click and hover events are
 * kept exactly as vanilla 1.8.9 expects. Going through OneConfig's Adventure layer dropped them.
 */
public final class Utils {
    private Utils() {
    }

    /** Shows a component in the local chat window (client side only, nothing is sent to the server). */
    public static void chat(Text component) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.gui == null) {
            return;
        }
        mc.gui.getChat().addMessage(component);
    }

    public static void chat(String message) {
        chat(new LiteralText(message));
    }

    public static void error(String message) {
        chat(Formatting.RED + message);
    }

    public static void info(String message) {
        chat(Formatting.AQUA + message);
    }

    /** An underlined url that opens in the browser when clicked. */
    public static Text link(String url) {
        Text text = new LiteralText(Formatting.AQUA.toString() + Formatting.UNDERLINE + url);
        text.getStyle()
                .setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("Click to open in your browser")));
        return text;
    }

    /** A text with a click (run command) and hover (show text) event attached. */
    public static Text button(String label, String command, String hover) {
        Text text = new LiteralText(label);
        text.getStyle()
                .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText(hover)));
        return text;
    }
}
