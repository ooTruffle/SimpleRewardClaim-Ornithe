package com.ootruffle.simplerewardclaim.types;

import java.util.List;

public class Data {
    private List<Reward> rewards;
    private DailyStreak dailyStreak;
    private String id;
    private String csrfToken;

    public int getDailyStreak() {
        return this.dailyStreak.getValue();
    }

    public String getId() {
        return this.id;
    }

    public List<Reward> getRewards() {
        return this.rewards;
    }

    public String getCsrfToken() {
        return this.csrfToken;
    }

    public void setCsrfToken(String csrfToken) {
        this.csrfToken = csrfToken;
    }
}
