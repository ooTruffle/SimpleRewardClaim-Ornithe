package com.ootruffle.simplerewardclaim;

import com.ootruffle.simplerewardclaim.types.Data;
import com.ootruffle.simplerewardclaim.types.Reward;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import net.minecraft.text.Formatting;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RewardClaim {
    private static final Pattern DATA_REGEX = Pattern.compile("window\\.appData = '(?<data>\\{.*})';");
    private static final Pattern TRANSLATION_REGEX = Pattern.compile("window.i18n = \\{(?<translations>.*)};", Pattern.DOTALL);
    private static final Pattern TRANSLATION_LINE_REGEX = Pattern.compile("\"(?<key>.*)\": ?\"(?<text>.*)\",?");
    private static final Pattern SECURITY_REGEX = Pattern.compile("window\\.securityToken = \"(?<token>.*)\";");
    private static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";
    private static final Gson GSON = new Gson();

    private static Data SESSION;
    /** True when SESSION came from /claim debug: claiming then sends nothing to Hypixel. */
    private static boolean debugSession;
    private static final int TIMEOUT_MS = 10_000;

    /** A reward page that could not be understood; the message is shown to the player. */
    public static class PageException extends Exception {
        public PageException(String message) {
            super(message);
        }
    }

    /** Everything we read out of a rewards.hypixel.net page. */
    public static class Page {
        public final Data data;
        public final HashMap<String, String> translations;

        Page(Data data, HashMap<String, String> translations) {
            this.data = data;
            this.translations = translations;
        }
    }

    /** Pulls the rewards, translations and security token out of the page html. No network involved. */
    public static Page parsePage(String html) throws PageException {
        if (html.contains("\"error\":\"This link either never existed or has already expired.\"")) {
            throw new PageException("This reward link has either expired or been used.");
        }

        Matcher dataMatcher = DATA_REGEX.matcher(html);
        if (!dataMatcher.find()) {
            throw new PageException("There was an error finding the rewards in the web request. Contact the developer if this persists.");
        }
        String appData = dataMatcher.group("data");

        Matcher translationMatcher = TRANSLATION_REGEX.matcher(html);
        if (!translationMatcher.find()) {
            throw new PageException("There was an error finding the translations in the web request. Contact the developer if this persists.");
        }
        String translationsData = translationMatcher.group("translations");

        HashMap<String, String> translations = new HashMap<>();
        Matcher translationLineMatcher = TRANSLATION_LINE_REGEX.matcher(
                translationsData.replace("                ", "\n").replace("\\'", "'"));
        while (translationLineMatcher.find()) {
            translations.put(translationLineMatcher.group("key"), translationLineMatcher.group("text"));
        }
        if (translations.size() < 10) {
            throw new PageException("There was an error recording the translations in the web request. Contact the developer if this persists.");
        }

        Data data = GSON.fromJson(appData, Data.class);

        Matcher csrfMatcher = SECURITY_REGEX.matcher(html);
        if (!csrfMatcher.find()) {
            throw new PageException("There was an error finding the security code in the web request. Contact the developer if this persists.");
        }
        data.setCsrfToken(csrfMatcher.group("token"));
        return new Page(data, translations);
    }

    /** Marks a session as fake: claiming it prints a message instead of contacting Hypixel. */
    static void startDebugSession(Data data) {
        debugSession = true;
        SESSION = data;
    }

    static void showRewards(Data data, HashMap<String, String> translations) {
        Text base = new LiteralText(Formatting.GREEN + "Daily Rewards: " + Formatting.RESET);
        for (int i = 0; i < data.getRewards().size(); i++) {
            Reward reward = data.getRewards().get(i);
            Text button = Utils.button(
                    reward.getRarityColor() + "[" + reward.getDisplayName(translations) + "]",
                    "/claim reward " + (i + 1),
                    "Click here to claim " + reward.getDescription(translations));
            base.append(button);
            if (i < data.getRewards().size() - 1) {
                base.append(new LiteralText(" "));
            }
        }
        Utils.chat(base);
    }

    public static Runnable fetch(String rewardId) {
        return () -> {
            boolean success = false;
            boolean expired = false;
            try {
                String inputLine;
                CookieManager cookieManager = new CookieManager();
                CookieHandler.setDefault(cookieManager);

                URL url = URI.create("https://rewards.hypixel.net/claim-reward/" + rewardId).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(TIMEOUT_MS);
                conn.setReadTimeout(TIMEOUT_MS);
                conn.setRequestProperty("User-Agent", USER_AGENT);
                conn.setUseCaches(true);
                conn.setDoOutput(true);
                conn.connect();

                if (conn.getResponseCode() != 200) {
                    Utils.error("[RewardClaim] There was an error fetching the rewards. Response Code: " + conn.getResponseCode());
                    throw new Exception("Invalid response code: " + conn.getResponseCode());
                }

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                Page page;
                try {
                    page = parsePage(response.toString());
                } catch (PageException e) {
                    expired = e.getMessage().contains("expired");
                    Utils.error("[RewardClaim] " + e.getMessage());
                    throw e;
                }
                Data data = page.data;
                HashMap<String, String> translations = page.translations;

                SimpleRewardClaim.LOGGER.info("Rewards for id " + rewardId + ": "
                        + String.join(", ", data.getRewards().stream()
                        .map(reward -> reward.getDisplayName(translations))
                        .toArray(String[]::new)));

                showRewards(data, translations);

                debugSession = false;
                SESSION = data;
                success = true;
            } catch (Exception e) {
                if (e instanceof MalformedURLException) {
                    SimpleRewardClaim.LOGGER.error("Invalid reward URL.");
                    return;
                }
                if (e instanceof JsonSyntaxException) {
                    SimpleRewardClaim.LOGGER.error("Failed to parse JSON");
                    Utils.error("[RewardClaim] There was an error reading the rewards from the Hypixel.");
                    return;
                }
                Utils.error("[RewardClaim] There was an error fetching the rewards from the rewards.hypixel.net");
                e.printStackTrace();
                SimpleRewardClaim.LOGGER.error("Exception: " + e);
                SimpleRewardClaim.LOGGER.error("An error occurred fetching rewards selection from rewards.hypixel.net");
            } finally {
                // The link was hidden from chat, so when loading fails give it back to the player to open themselves.
                if (!success && !expired && RewardClaimConfig.INSTANCE.hideLinkMessage) {
                    Text message = new LiteralText(Formatting.RED + "[RewardClaim] Couldn't load your rewards. Open the link yourself: ");
                    message.append(Utils.link("https://rewards.hypixel.net/claim-reward/" + rewardId));
                    Utils.chat(message);
                }
            }
        };
    }

    public static Runnable claim(int selected) {
        return () -> {
            if (debugSession) {
                Utils.info("[RewardClaim] DEBUG: click worked! Would claim reward " + selected + " (nothing was sent).");
                return;
            }
            if (SESSION == null) {
                Utils.error("No Daily Reward Session found. Try: /claim id <id> first");
                throw new IllegalStateException("No session found.");
            }
            try {
                URL submission = URI.create("https://rewards.hypixel.net/claim-reward/claim?option=" + (selected - 1)
                        + "&id=" + SESSION.getId()
                        + "&activeAd=" + 0
                        + "&_csrf=" + SESSION.getCsrfToken()
                        + "&watchedFallback=false").toURL();
                HttpURLConnection submitConn = (HttpURLConnection) submission.openConnection();
                submitConn.setRequestMethod("POST");
                submitConn.setConnectTimeout(TIMEOUT_MS);
                submitConn.setReadTimeout(TIMEOUT_MS);
                submitConn.setRequestProperty("User-Agent", USER_AGENT);
                submitConn.setUseCaches(true);
                submitConn.setDoOutput(true);
                CookieManager.setDefault(null);

                Utils.info("[RewardClaim] Claiming reward " + selected + ".");
                submitConn.connect();

                if (submitConn.getResponseCode() != 200) {
                    String inputLine;
                    BufferedReader in = new BufferedReader(new InputStreamReader(
                            submitConn.getErrorStream() != null ? submitConn.getErrorStream() : submitConn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    Utils.error("[RewardClaim] There was an error claiming the rewards. Response Code: " + submitConn.getResponseCode());
                    throw new Exception("Invalid response code: " + submitConn.getResponseCode() + "\n" + response);
                }
            } catch (Exception e) {
                Utils.error("[RewardClaim] There was an error fetching the rewards from the rewards.hypixel.net");
                SimpleRewardClaim.LOGGER.error("User selection: " + selected + " (API: " + (selected - 1) + ").");
                e.printStackTrace();
                SimpleRewardClaim.LOGGER.error("Exception: " + e);
                SimpleRewardClaim.LOGGER.error("An error occurred submitting daily reward selection to rewards.hypixel.net");
            }
        };
    }
}
