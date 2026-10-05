package com.ootruffle.simplerewardclaim;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import org.polyfrost.oneconfig.api.commands.v1.CommandManager;

/**
 * /claim id <id>       - fetch the rewards for a reward link id
 * /claim reward <1-3>  - claim one of the fetched rewards
 *
 * Debug builds also get:
 * /claim debug         - fake a found link, to test that chat buttons are clickable
 * /claim screens       - toggle printing the class of every screen that opens
 * /claim selftest [net] - run every stage of the mod on fake data and print PASS/FAIL for each
 *
 * The original Forge version subclassed CommandBase; here the same command tree is built with
 * Brigadier through OneConfig's CommandManager, which is what Fabric/Ornithe 1.8.9 mods use.
 */
public final class Command {
    private static final String USAGE = Constants.DEBUG ? "<id|reward|debug|screens|selftest>" : "<id|reward>";

    private Command() {
    }

    public static void register() {
        var manager = CommandManager.INSTANCE;

        var claim = CommandManager.literal("claim")
                .executes(ctx -> {
                    Utils.error("Missing arguments. Usage: /claim " + USAGE);
                    return 0;
                })
                .then(CommandManager.literal("id")
                        .executes(ctx -> {
                            Utils.error("Missing arguments. Usage: /claim id <id>");
                            return 0;
                        })
                        .then(manager.argument("id", StringArgumentType.word())
                                .executes(ctx -> {
                                    String id = StringArgumentType.getString(ctx, "id");
                                    new Thread(RewardClaim.fetch(id)).start();
                                    return 1;
                                })))
                .then(CommandManager.literal("reward")
                        .executes(ctx -> {
                            Utils.error("Missing arguments. Usage: /claim reward <1|2|3>");
                            return 0;
                        })
                        .then(manager.argument("number", IntegerArgumentType.integer(1, 3))
                                .suggests((ctx, builder) -> {
                                    builder.suggest("1");
                                    builder.suggest("2");
                                    builder.suggest("3");
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    int selection = IntegerArgumentType.getInteger(ctx, "number");
                                    new Thread(RewardClaim.claim(selection)).start();
                                    return 1;
                                })));

        if (Constants.DEBUG) {
            claim.then(CommandManager.literal("debug")
                    .executes(ctx -> {
                        new Thread(Debug.fakeLink()).start();
                        return 1;
                    }));
            claim.then(CommandManager.literal("screens")
                    .executes(ctx -> {
                        boolean on = Debug.toggleScreens();
                        Utils.info("[RewardClaim] Screen logging " + (on ? "ON" : "OFF")
                                + ". While on, every opened screen is printed in chat.");
                        return 1;
                    }));
            claim.then(CommandManager.literal("selftest")
                    .executes(ctx -> {
                        new Thread(() -> SelfTest.run(false)).start();
                        return 1;
                    })
                    .then(CommandManager.literal("net")
                            .executes(ctx -> {
                                new Thread(() -> SelfTest.run(true)).start();
                                return 1;
                            })));
        }

        // Catch-all so anything that is not a known subcommand shows usage instead of
        // Brigadier's generic "Incorrect argument for command" error.
        claim.then(manager.argument("args", StringArgumentType.greedyString())
                .executes(ctx -> {
                    Utils.error("Unknown argument. Usage: /claim " + USAGE);
                    return 0;
                }));

        manager.register(claim);
    }
}
