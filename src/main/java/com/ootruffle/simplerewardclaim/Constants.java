package com.ootruffle.simplerewardclaim;

public final class Constants {
    // Substituted from stonecutter.properties.toml by the bloom plugin
    public static final String ID = "@MOD_ID@";
    public static final String NAME = "@MOD_NAME@";
    public static final String VERSION = "@MOD_VERSION@";

    /**
     * Whether the debug commands (/claim debug, /claim screens, /claim selftest) exist in this build.
     * Set by the buildscript: on for normal builds, off for `./gradlew build -Prelease`.
     */
    public static final boolean DEBUG = Boolean.parseBoolean("@DEBUG@");

    private Constants() {
    }
}
