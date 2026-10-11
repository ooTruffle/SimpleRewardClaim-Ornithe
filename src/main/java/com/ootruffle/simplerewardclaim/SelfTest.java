package com.ootruffle.simplerewardclaim;

import com.ootruffle.simplerewardclaim.types.Data;
import net.minecraft.text.Formatting;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;

/**
 * /claim selftest [net]
 *
 * Runs each stage of the mod on fake data and prints PASS / FAIL for it:
 *   1. link detection (the chat regexes and the book NBT)
 *   2. book screen detection (class name matching)
 *   3. reward page parsing (the same code the real fetch uses)
 *   4. showing the clickable buttons, with a debug session so claiming sends nothing
 *   5. (only with "net") a real request to rewards.hypixel.net, to check it is reachable
 *
 * What it cannot do is fake Hypixel's real chat message or book packet. For those, use
 * /claim screens and a real /daily.
 */
public final class SelfTest {
    private static int passed;
    private static int failed;

    private SelfTest() {
    }

    public static void run(boolean network) {
        passed = 0;
        failed = 0;
        Utils.chat(Formatting.YELLOW + "[RewardClaim] Self test starting...");

        testLinkDetection();
        testBookDetection();
        testConfig();
        Data data = testPageParsing();
        testButtons(data);
        if (network) {
            testNetwork();
        } else {
            Utils.chat(Formatting.GRAY + "[RewardClaim] (skipped network check, run /claim selftest net to include it)");
        }

        String summary = "[RewardClaim] Self test finished: " + passed + " passed, " + failed + " failed.";
        Utils.chat((failed == 0 ? Formatting.GREEN : Formatting.RED) + summary);
    }

    private static void step(String name, boolean ok, String detail) {
        if (ok) {
            passed++;
            Utils.chat(Formatting.GREEN + "[PASS] " + Formatting.GRAY + name);
        } else {
            failed++;
            Utils.chat(Formatting.RED + "[FAIL] " + Formatting.GRAY + name + (detail.isEmpty() ? "" : " - " + detail));
        }
        SimpleRewardClaim.LOGGER.info("selftest " + (ok ? "PASS" : "FAIL") + ": " + name + (detail.isEmpty() ? "" : " - " + detail));
    }

    // ---- 1 ----
    private static void testLinkDetection() {
        String normal = "Click the link to visit our website and claim your reward: https://rewards.hypixel.net/claim-reward/AbCd1234";
        String id = SimpleRewardClaim.matchRewardId(normal);
        step("Detects the normal reward link message", "AbCd1234".equals(id), "got " + id);

        String missed = "We noticed you haven't claimed your free Daily Reward yet!\n"
                + "To choose your reward you have to click the link to visit our website! "
                + "As a reminder, here's your link for today:  https://rewards.hypixel.net/claim-reward/Zz9Yy8Xx";
        id = SimpleRewardClaim.matchRewardId(missed);
        step("Detects the missed-reward reminder message", "Zz9Yy8Xx".equals(id), "got " + id);

        id = SimpleRewardClaim.matchRewardId("[VIP] someone: salmon smells good bro");
        step("Ignores ordinary chat messages", id == null, "got " + id);

        // What the book's NBT looks like as text: each page is a JSON string, so its quotes are escaped.
        String book = "{pages:[0:\"{\\\"text\\\":\\\"Claim\\\",\\\"clickEvent\\\":{\\\"action\\\":\\\"open_url\\\","
                + "\\\"value\\\":\\\"https://rewards.hypixel.net/claim-reward/Qw3Er5Ty\\\"}}\"],title:\"Daily Reward\"}";
        id = SimpleRewardClaim.matchBookRewardId(book);
        step("Detects the reward link inside the book", "Qw3Er5Ty".equals(id), "got " + id);

        id = SimpleRewardClaim.matchBookRewardId("{value:\"https:\\/\\/rewards.hypixel.net\\/claim-reward\\/Ab12Cd34\"}");
        step("Detects a book link with escaped slashes", "Ab12Cd34".equals(id), "got " + id);

        // The format Hypixel actually sends (October 2026): no "rewards." subdomain, escaped slashes.
        id = SimpleRewardClaim.matchBookRewardId("\\\"clickEvent\\\":{\\\"action\\\":\\\"open_url\\\","
                + "\\\"value\\\":\\\"https:\\/\\/hypixel.net\\/claim-reward\\/e831c683\\\"}");
        step("Detects the current hypixel.net book link", "e831c683".equals(id), "got " + id);
    }

    // ---- 2 ----
    private static void testBookDetection() {
        step("Book check matches the Ornithe intermediary name",
                SimpleRewardClaim.isBookClassName("net.minecraft.unmapped.C_88380901"), "");
        step("Book check matches a Feather-style name",
                SimpleRewardClaim.isBookClassName("net.minecraft.client.gui.screen.game.BookEditScreen"), "");
        step("Book check ignores other screens",
                !SimpleRewardClaim.isBookClassName("net.minecraft.client.gui.screen.ChatScreen"), "");
    }

    private static void testConfig() {
        RewardClaimConfig c = RewardClaimConfig.INSTANCE;
        step("Config loaded (hide link: " + c.hideLinkMessage + ", hide book: " + c.hideBook + ")", true, "");
        net.minecraft.text.Text link = Utils.link("https://rewards.hypixel.net/claim-reward/AbCd1234");
        Utils.chat(Formatting.GRAY + "[RewardClaim] Fallback link preview, click it to check it opens your browser:");
        Utils.chat(link);
    }

    // ---- 3 ----
    private static Data testPageParsing() {
        String[] translations = {
                "\"type.coins\": \"{$game} Coins\",",
                "\"type.coins.description\": \"some {$game} Coins\",",
                "\"type.tokens\": \"{$game} Tokens\",",
                "\"type.tokens.description\": \"some {$game} Tokens\",",
                "\"type.add_vanity\": \"Vanity item\",",
                "\"type.add_vanity.description\": \"a vanity item\",",
                "\"vanity.suits.description\": \"a suit piece\",",
                "\"vanity.emotes.description\": \"an emote\",",
                "\"vanity.gestures.description\": \"a gesture\",",
                "\"housing.skull.test\": \"Test Skull\",",
                "\"misc.filler.one\": \"filler\",",
                "\"misc.filler.two\": \"it\\'s filler\"",
        };
        // The real page is read line by line and glued together, so entries end up 16 spaces apart.
        String html = "<html><script>window.appData = '{\"id\":\"selftest\",\"dailyStreak\":{\"value\":3},\"rewards\":["
                + "{\"reward\":\"coins\",\"rarity\":\"COMMON\",\"amount\":1500,\"gameType\":\"BEDWARS\"},"
                + "{\"reward\":\"tokens\",\"rarity\":\"EPIC\",\"amount\":500,\"gameType\":\"SKYWARS\"},"
                + "{\"reward\":\"add_vanity\",\"rarity\":\"LEGENDARY\",\"package\":\"selftest_emote_item\"}]}';"
                + "window.i18n = {                " + String.join("                ", translations) + "};"
                + "window.securityToken = \"tok123\";</script></html>";

        RewardClaim.Page page = null;
        try {
            page = RewardClaim.parsePage(html);
        } catch (Exception e) {
            step("Parses a reward page", false, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        if (page == null) {
            return null;
        }

        step("Parses a reward page", true, "");
        step("Reads 3 rewards", page.data.getRewards().size() == 3, "got " + page.data.getRewards().size());
        step("Reads the security token", "tok123".equals(page.data.getCsrfToken()), "got " + page.data.getCsrfToken());
        step("Reads the translations (" + page.translations.size() + ")", page.translations.size() >= 12,
                "expected 12, got " + page.translations.size());

        String name = page.data.getRewards().get(0).getDisplayName(page.translations);
        step("Builds reward names (\"" + name + "\")", "1,500 Bed Wars Coins".equals(name), "expected \"1,500 Bed Wars Coins\"");

        String desc = page.data.getRewards().get(2).getDescription(page.translations);
        step("Picks the emote description for emote packages", "an emote".equals(desc), "got \"" + desc + "\"");

        try {
            RewardClaim.parsePage("{\"error\":\"This link either never existed or has already expired.\"}");
            step("Reports an expired link", false, "no error thrown");
        } catch (RewardClaim.PageException e) {
            step("Reports an expired link", e.getMessage().contains("expired"), e.getMessage());
        }
        return page.data;
    }

    // ---- 4 ----
    private static void testButtons(Data data) {
        if (data == null) {
            step("Shows clickable reward buttons", false, "skipped, the page did not parse");
            return;
        }
        // Debug session: clicking /claim reward N prints a message and sends nothing to Hypixel.
        java.util.HashMap<String, String> t = new java.util.HashMap<>();
        t.put("type.coins", "{$game} Coins");
        t.put("type.coins.description", "some {$game} Coins");
        t.put("type.tokens", "{$game} Tokens");
        t.put("type.tokens.description", "some {$game} Tokens");
        t.put("type.add_vanity", "Vanity item");
        t.put("type.add_vanity.description", "a vanity item");
        RewardClaim.startDebugSession(data);
        RewardClaim.showRewards(data, t);
        Utils.chat(Formatting.YELLOW + "[RewardClaim] Now click one of the buttons above. "
                + "Expected reply: \"DEBUG: click worked!\". If nothing happens, clicking is broken.");
        step("Shows clickable reward buttons (you have to click one to finish this check)", true, "");
    }

    // ---- 5 ----
    private static void testNetwork() {
        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create("https://rewards.hypixel.net/claim-reward/00000000").toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(10_000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            int code = conn.getResponseCode();
            InputStream stream = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            StringBuilder body = new StringBuilder();
            if (stream != null) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(stream))) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        body.append(line);
                    }
                }
            }
            boolean expired = body.toString().contains("This link either never existed or has already expired.");
            step("Reaches rewards.hypixel.net (HTTP " + code + ")", true, "");
            step("Server rejects a fake id as expected", expired || code >= 400,
                    "HTTP " + code + ", expected an \"expired\" error");
        } catch (Exception e) {
            step("Reaches rewards.hypixel.net", false, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
