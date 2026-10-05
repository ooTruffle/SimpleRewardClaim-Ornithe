package com.ootruffle.simplerewardclaim;

import org.polyfrost.oneconfig.api.config.v1.Config;
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch;

/** Settings shown in the OneConfig menu under Hypixel. Stored in simplerewardclaim.json. */
public class RewardClaimConfig extends Config {
    public static final RewardClaimConfig INSTANCE = new RewardClaimConfig();

    @Switch(
            title = "Hide reward link message",
            description = "Hides Hypixel's daily reward link from chat. The mod still reads it and shows the reward buttons instead."
    )
    public boolean hideLinkMessage = true;

    @Switch(
            title = "Hide reward book",
            description = "Stops the book that Hypixel opens alongside the reward link from showing."
    )
    public boolean hideBook = true;

    public RewardClaimConfig() {
        super(Constants.ID + ".json", Constants.NAME, Category.HYPIXEL);
    }
}
