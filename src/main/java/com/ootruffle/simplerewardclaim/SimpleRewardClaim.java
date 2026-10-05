package com.ootruffle.simplerewardclaim;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.text.Formatting;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.ChatEvent;
import org.polyfrost.oneconfig.api.event.v1.events.ScreenOpenEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleRewardClaim implements ClientModInitializer {
    public static final Logger LOGGER = LogManager.getLogger(Constants.ID);

    /** How long after a reward link a book screen is treated as the reward book and cancelled. */
    private static final long BOOK_WINDOW_MS = 3000;
    private volatile long lastLinkMillis;

    private final Set<String> ids = new HashSet<>();
    private static final Pattern REWARD_REGEX = Pattern.compile(
            "Click the link to visit our website and claim your reward: https://rewards.hypixel.net/claim-reward/(?<id>[A-Za-z0-9]{8})");
    private static final Pattern MISSED_REWARD_REGEX = Pattern.compile(
            "We noticed you haven't claimed your free Daily Reward yet!\nTo choose your reward you have to click the link to visit our website! As a reminder, here's your link for today:  https://rewards.hypixel.net/claim-reward/(?<id>[A-Za-z0-9]{8})");

    @Override
    public void onInitializeClient() {
        // Registers the settings with OneConfig so they appear in its menu.
        RewardClaimConfig.INSTANCE.preload();

        // Forge: MinecraftForge.EVENT_BUS.register(this) + ClientChatReceivedEvent
        // OneConfig: ChatEvent.Receive fires for every message added to the chat window
        // (the Forge `event.type != 0` check is not needed, action bar text never reaches the chat window).
        EventManager.register(ChatEvent.Receive.class, (ChatEvent.Receive event) -> onChatReceive(event));

        // Hypixel opens a book alongside the reward link. Cancelling the screen open keeps it from showing.
        EventManager.register(ScreenOpenEvent.class, (ScreenOpenEvent event) -> onScreenOpen(event));

        // Forge: ClientCommandHandler.instance.registerCommand(new Command())
        Command.register();
    }

    private void onChatReceive(ChatEvent.Receive event) {
        String message = event.getFullyUnformattedMessage();

        // Matching is cheap, so do it here on the chat thread: that lets us cancel the event, which hides the
        // link from the chat window while the mod still gets the id. The network work happens on a new thread.
        String id = matchRewardId(message);
        if (id == null) {
            return;
        }

        if (RewardClaimConfig.INSTANCE.hideLinkMessage) {
            event.cancelled = true;
        }
        this.lastLinkMillis = System.currentTimeMillis();
        new Thread(() -> {
            if (Constants.DEBUG) {
                Debug.linkDetected(id);
            }
            this.rewardMessageFound(id);
        }).start();
    }

    /** Returns the reward id if the chat message is Hypixel's reward link (or its reminder), otherwise null. */
    static String matchRewardId(String message) {
        Matcher matcher = REWARD_REGEX.matcher(message);
        if (matcher.find() && matcher.group("id") != null) {
            return matcher.group("id");
        }
        matcher = MISSED_REWARD_REGEX.matcher(message);
        if (matcher.find() && matcher.group("id") != null) {
            return matcher.group("id");
        }
        return null;
    }

    private void onScreenOpen(ScreenOpenEvent event) {
        Object screen = event.getScreen();
        long sinceLink = this.lastLinkMillis == 0 ? -1 : System.currentTimeMillis() - this.lastLinkMillis;

        if (Constants.DEBUG && screen != null) {
            String where = sinceLink < 0 ? "no reward link seen yet" : sinceLink + " ms after the last reward link";
            Debug.screenOpened(screen.getClass().getName(), where);
        }

        if (screen == null || sinceLink < 0 || sinceLink >= BOOK_WINDOW_MS) {
            return;
        }
        if (RewardClaimConfig.INSTANCE.hideBook && isBookScreen(screen)) {
            event.cancelled = true;
        }
    }

    /**
     * Matches the 1.8.9 book screen by class name rather than by a compile-time class, so it keeps working
     * whatever Feather calls it. In production the game runs on Ornithe intermediary names, where the book
     * screen is C_88380901; in a dev environment it carries its Feather name, which contains "Book".
     */
    private static boolean isBookScreen(Object screen) {
        return isBookClassName(screen.getClass().getName());
    }

    static boolean isBookClassName(String name) {
        return name.contains("C_88380901") || name.contains("Book");
    }

    private boolean rewardMessageFound(String id) {
        if (!this.ids.contains(id)) {
            RewardClaim.fetch(id).run();
            this.ids.add(id);
            return true;
        }

        Text message = new LiteralText(Formatting.LIGHT_PURPLE + "[RewardClaim] A request for this reward has already been made. ");
        message.append(Utils.button(Formatting.BLUE + "[Make it again?]", "/claim id " + id,
                "Click here to fetch your daily rewards."));
        Utils.chat(message);
        return false;
    }
}
