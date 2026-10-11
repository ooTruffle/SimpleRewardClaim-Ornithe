package com.ootruffle.simplerewardclaim;

import com.ootruffle.simplerewardclaim.types.Data;
import com.google.gson.Gson;
import net.minecraft.text.Formatting;

import java.util.HashMap;

/**
 * Everything behind /claim debug and /claim screens. Only reachable when Constants.DEBUG is true, and the
 * class is left out of the jar entirely for `-Prelease` builds.
 */
final class Debug {
    /** Toggled by /claim screens: prints every screen that opens, to find out what a book is called. */
    static volatile boolean logScreens;

    private Debug() {
    }

    /** /claim debug: behaves as if a reward link was found, without any network request. */
    static Runnable fakeLink() {
        return () -> {
            HashMap<String, String> translations = new HashMap<>();
            translations.put("type.coins", "{$game} Coins");
            translations.put("type.coins.description", "some {$game} Coins (debug)");
            translations.put("type.tokens", "{$game} Tokens");
            translations.put("type.tokens.description", "some {$game} Tokens (debug)");
            translations.put("type.add_vanity", "Vanity item");
            translations.put("type.add_vanity.description", "a vanity item (debug)");

            String json = "{\"id\":\"debug\",\"csrfToken\":\"debug\",\"dailyStreak\":{\"value\":7},\"rewards\":["
                    + "{\"reward\":\"coins\",\"rarity\":\"COMMON\",\"amount\":1500,\"gameType\":\"BEDWARS\"},"
                    + "{\"reward\":\"tokens\",\"rarity\":\"RARE\",\"amount\":500,\"gameType\":\"SKYWARS\"},"
                    + "{\"reward\":\"add_vanity\",\"rarity\":\"LEGENDARY\",\"package\":\"debug_item\"}]}";
            Data data = new Gson().fromJson(json, Data.class);

            Utils.info("[RewardClaim] DEBUG: pretending a reward link was found. Nothing is sent to Hypixel.");
            RewardClaim.showRewards(data, translations);
            Utils.chat(Utils.button(Formatting.BLUE + "[Run debug again]", "/claim debug",
                    "Click here to show the debug rewards again."));
            RewardClaim.startDebugSession(data);
        };
    }

    static boolean toggleScreens() {
        logScreens = !logScreens;
        return logScreens;
    }

    static void screenOpened(String className, String where) {
        SimpleRewardClaim.LOGGER.info("Screen opened: " + className + " (" + where + ")");
        if (logScreens) {
            Utils.chat(Formatting.GRAY + "[RewardClaim] Screen opened: " + className + " (" + where + ")");
        }
    }

    /** Logs the raw NBT of a book being opened, to see where Hypixel puts the claim link. */
    static void bookContents(String source, String contents, String id) {
        SimpleRewardClaim.LOGGER.info("Book (" + source + "): " + contents + " -> id " + id);
        if (logScreens) {
            Utils.chat(Formatting.GRAY + "[RewardClaim] Book (" + source + ") -> id " + id + ", full NBT in the log");
        }
    }

    static void linkDetected(String id) {
        SimpleRewardClaim.LOGGER.info("Reward link detected (id " + id + ")");
        if (logScreens) {
            Utils.chat(Formatting.GRAY + "[RewardClaim] Reward link detected (id " + id + ")");
        }
    }
}
