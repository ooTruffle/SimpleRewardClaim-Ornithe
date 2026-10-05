package com.ootruffle.simplerewardclaim.types;

import com.google.gson.annotations.SerializedName;
import net.minecraft.text.Formatting;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Reward {
    @SerializedName("reward")
    private String rewardKey;
    private Rarity rarity;
    /** May be absent from the JSON, in which case it is null. */
    private Integer amount;
    /** May be absent from the JSON, in which case it is null. */
    @SerializedName("package")
    private String packageKey;
    @SerializedName("gameType")
    private GameMode gameMode;

    private final transient Pattern ARMOR_REGEX = Pattern.compile("(^[a-z0-9_]+)_([a-z]+)$", Pattern.CASE_INSENSITIVE);
    private final transient DecimalFormat decimalFormat = new DecimalFormat("#,###");

    public String getRewardKey() {
        return this.rewardKey;
    }

    public String getPackageKey() {
        return this.packageKey;
    }

    private Integer getAmount() {
        return this.amount;
    }

    private String getAmountFormatted() {
        return this.decimalFormat.format(this.getAmount());
    }

    public Rarity getRarity() {
        return this.rarity;
    }

    public String getDisplayName(HashMap<String, String> t) {
        if (this.getRewardKey().equals("housing_package")) {
            return t.getOrDefault("type." + this.getRewardKey(), "Unknown housing item") + ": "
                    + t.getOrDefault("housing.skull." + (this.getPackageKey() != null
                    ? this.getPackageKey().replace("specialoccasion_reward_card_skull_", "")
                    : ""), "Unknown housing package");
        }
        if (this.getRewardKey().equals("add_vanity")) {
            Matcher armorMatcher = this.ARMOR_REGEX.matcher(this.getPackageKey() != null ? this.getPackageKey() : "");
            if (armorMatcher.find() && this.getPackageKey().contains("suit")) {
                return t.getOrDefault("vanity." + armorMatcher.group(1), "Unknown Armor Item") + " "
                        + t.getOrDefault("vanity." + armorMatcher.group(2), "");
            }
            if (this.getPackageKey() != null
                    && (this.getPackageKey().contains("emote") || this.getPackageKey().contains("taunt"))) {
                return t.getOrDefault("vanity." + this.getRewardKey(), "Unknown vanity (emote/taunt) item");
            }
        }
        if (this.getAmount() != null) {
            return this.getAmountFormatted() + " "
                    + t.getOrDefault("type." + this.getRewardKey(), "Unknown ").replace("{$game}", this.getGameName());
        }
        return t.getOrDefault("type." + this.getRewardKey(), "");
    }

    public String getDescription(HashMap<String, String> t) {
        if (this.getRewardKey().equals("add_vanity") && this.getPackageKey() != null) {
            if (this.getPackageKey().contains("suit")) {
                return t.getOrDefault("vanity.suits.description", "Unknown translation for " + this.getPackageKey());
            }
            if (this.getPackageKey().contains("emote")) {
                return t.getOrDefault("vanity.emotes.description", "Unknown translation for " + this.getPackageKey());
            }
            if (this.getPackageKey().contains("taunt")) {
                return t.getOrDefault("vanity.gestures.description", "Unknown translation for " + this.getPackageKey());
            }
        }
        if (this.getRewardKey().equals("tokens") || this.getRewardKey().equals("coins")) {
            return t.getOrDefault("type." + this.getRewardKey() + ".description",
                    "Unknown translation for reward " + this.getRewardKey()).replace("{$game}", this.getGameName());
        }
        if (this.getRewardKey().equals("housing_package")) {
            return t.getOrDefault("type." + this.getRewardKey(), "Unknown housing item") + ": "
                    + t.getOrDefault("housing.skull." + (this.getPackageKey() != null
                    ? this.getPackageKey().replace("specialoccasion_reward_card_skull_", "")
                    : ""), "Unknown housing package")
                    + " (" + t.getOrDefault("type." + this.getRewardKey() + ".description",
                    " Unknown translation for reward " + this.getRewardKey()) + ")";
        }
        return t.getOrDefault("type." + this.getRewardKey() + ".description",
                "Unknown translation for reward " + this.getRewardKey());
    }

    /** Colour used for this reward's chat button. Matches the original section-sign codes (\u00a77, \u00a7b, \u00a75, \u00a76). */
    public Formatting getRarityColor() {
        if (this.rarity == null) {
            return Formatting.RESET;
        }
        switch (this.rarity) {
            case COMMON:
                return Formatting.GRAY;        // §7
            case RARE:
                return Formatting.AQUA;        // §b
            case EPIC:
                return Formatting.DARK_PURPLE; // §5
            case LEGENDARY:
                return Formatting.GOLD;        // §6
            default:
                return Formatting.RESET;       // §r
        }
    }

    private String getGameName() {
        if (this.gameMode == null) {
            return "Unknown";
        }
        switch (this.gameMode) {
            case BEDWARS:
                return "Bed Wars";
            case SKYWARS:
                return "SkyWars";
            case PROTOTYPE:
                return "Prototype";
            case SKYBLOCK:
                return "SkyBlock";
            case MAIN:
                return "Main";
            case MURDER_MYSTERY:
                return "Murder Mystery";
            case HOUSING:
                return "Housing";
            case ARCADE:
                return "Arcade";
            case BUILD_BATTLE:
                return "Build Battle";
            case DUELS:
                return "Duels";
            case PIT:
                return "PIT";
            case UHC:
                return "UHC";
            case SPEED_UHC:
                return "Speed UHC";
            case TNTGAMES:
                return "TNT Games";
            case LEGACY:
                return "Classic";
            case QUAKECRAFT:
                return "Quakecraft";
            case WALLS:
                return "Walls";
            case PAINTBALL:
                return "Paintball";
            case VAMPIREZ:
                return "VampireZ";
            case ARENA:
                return "Arena";
            case GINGERBREAD:
                return "Turbo Kart Racers";
            case MCGO:
                return "Cops and Crims";
            case SURVIVAL_GAMES:
                return "Blitz SG";
            case WALLS3:
                return "Mega Walls";
            case SUPER_SMASH:
                return "Smash Heroes";
            case BATTLEGROUND:
                return "Warlords";
            default:
                return "Unknown";
        }
    }
}
