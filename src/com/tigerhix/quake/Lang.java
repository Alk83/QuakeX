package com.tigerhix.quake;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * An enum for requesting strings from the language file.
 *
 * @author gomeow
 */

public enum Lang {

    STATS("stats", "&4&lStats"),
    LEADERBOARD("leaderboard", "&4&lLeaderboard"),
    POINTS("points", "&bPoints"),
    COINS("coins", "&bCoins"),
    KILLS("kills", "&bKills"),
    CLICK_TO_SHOW("click-to-show", "Click to show"),

    JOIN("join", "Join"),
    FULL("full", "Full"),
    IN_PROGRESS("in-progress", "In progress"),

    BUY_MENU("buy-menu", "Quake shop"),
    SHOP("shop", "Shop"),

    WOOD_HOE("wood-railgun", "Wood railgun"),
    STONE_HOE("stone-railgun", "Stone railgun"),
    IRON_HOE("iron-railgun", "Iron railgun"),
    GOLD_HOE("gold-railgun", "Gold railgun"),
    DIAMOND_HOE("diamond-railgun", "Diamond railgun"),

    WOOD_HOE_DESCRIPTION("wood-railgun-description", "Old railgun"),
    STONE_HOE_DESCRIPTION("stone-railgun-description", "Newbie railgun"),
    IRON_HOE_DESCRIPTION("iron-railgun-description", "Advanced railgun"),
    GOLD_HOE_DESCRIPTION("gold-railgun-description", "Super railgun"),
    DIAMOND_HOE_DESCRIPTION("diamond-railgun-description", "Awesome railgun"),

    ARENA_NOT_FOUND("arena-not-found", "&c Cannot find the arena."),
    ARENA_NOT_FOUND_CANT_CREATE("arena-not-found-cannot-create", "&c Cannot find the arena. Join sign not set."),
    ARENA_NOT_SELECTED("arena-not-selected", "&c No arena selected. Please use '/quake select' to select an arena."),
    ARENA_ALREADY_EXISTS("arena-already-exists", "&c Arena already exists!"),
    ALREADY_JOINED("already-joined", "&c You are already in-game! Please use '/quake leave' to leave current arena."),
    ARENA_NOT_JOINED("arena-not-joined", "&c You are not in-game!"),
    ARENA_ALREADY_STARTED("arena-already-started", "&c Arena is already started!"),
    ARENA_FULL("arena-full", "&c Arena is full!"),
    ARENA_LEAVED("arena-leaved", "&c You left the arena."),
    CANT_LEAVE_ARENA("cant-leave-arena", "&c Can't leave arena at this time!"),
    NO_COMMANDS("no-commands", "&c Non-Quake commands are disabled in arenas!"),

    PLAYER_JOINED("player-joined", "&7 &b%player &7joined the game."),
    PLAYER_LEAVED("player-leaved", "&7 &b%player &7left the game."),
    PLAYER_KILLED_PLAYER("player-killed-player", "&7 &b%killer &7gibbed &b%killed&7!"),
    LAVA_KILLED_PLAYER("lava-killed-player", "&7 &b%killed &7tried to swim in lava!"),
    VOID_KILLED_PLAYER("void-killed-player", "&7 &b%killed &7fell into void!"),
    PLAYER_WON("player-won", "&7 &b%player &7won the game!"),

    MATCH_STARTED("match-started", "&7 Match started! Right click your railgun to instant-kill others!"),
    MATCH_ENDED("match-ended", "&7 Match ended!"),
    MATCH_IS_STARTING_IN("match-is-starting-in", "&7 Match starts in %seconds seconds!"),

    MET_MIN_REQUIREMENT("met-min-requirement", "&7 Met minimum player requirement! Match starts in %seconds seconds.."),
    MET_MAX_REQUIREMENT("met-max-requirement", "&7 Met maximum player requirement! Match starts in %seconds seconds.."),
    CANT_MET_MINIMUM_REQUIREMENT("cant-met-minimum-requirement", "&c Someone left and can't meet minimum player requirement right now. Canceled starting of match..."),

    SHOP_ALREADY_PURCHASED("shop-already-purchased", "&bPurchased"),
    SHOP_RELOAD_TIME("shop-reload-time", "&eReload time: "),
    SHOP_RELOAD_TIME_FORMAT("shop-reload-time-format", "&b%times"),
    SHOP_PRICE("shop-price", "&ePrice: "),
    SHOP_PRICE_FORMAT("shop-price-format", "&b$%price"),

    PURCHASED("purchased", "&7 Successfully purchased and auto-selected. You will be able to use it in matches now."),
    CHOSEN("chosen", "&7 Successfully chosen this railgun."),
    NOT_ENOUGH_COINS("not-enough-coins", "&c You don't have enough coins!"),

    DOUBLE_KILL("double-kill", "&e Double kill!"),
    TRIPLE_KILL("triple-kill", "&b Triple kill!!"),
    QUADRUPLE_KILL("quadruple-kill", "&c&l Quadruple kill!!!"),
    UNBELIEVABLE_KILL("more-than-4-kill", "&c&l&o UNBELIEVABLE KILL!!!!"),

    ARENA_CREATED("arena-created", "&7 Arena created and auto-selected. Please use '/quake addspawn' to add spawnpoints."),
    ARENA_SELECTED("arena-selected", "&7 Arena selected."),
    JOIN_SIGN_SET("join-sign-set", "&7 Join sign set."),
    JOIN_SIGN_REMOVED("join-sign-removed", "&7 Join sign removed."),
    NOT_IN_LOBBY_WORLD("not-in-lobby-world", "&c You are not in lobby world!"),
    SPAWN_SET("spawn-set", "&7 Spawn set."),
    LOBBY_SET("lobby-set", "&7 Lobby set. Now you can put join signs in this world."),
    STATS_CHANGED("stats-changed", "&7 Stats changed."),
    WRONG_COMMAND("wrong-command", "&c Unknown command or wrong arguments. Type '/quake help' for help."),
    DISABLED_COMMAND("disabled-command", "&c This command is disabled in matches.");

    private String path;
    private String def;
    private static YamlConfiguration LANG;

    private List<String> words = new ArrayList<>();

    /**
     * Lang enum constructor.
     *
     * @param path  The string path.
     * @param start The default string.
     */
    Lang(String path, String start) {
        words.add("shop-already-purchased");
        words.add("shop-reload-time");
        words.add("shop-reload-time-format");
        words.add("shop-price");
        words.add("shop-price-format");
        words.add("stats");
        words.add("leaderboard");
        words.add("points");
        words.add("coins");
        words.add("kills");
        words.add("join");
        words.add("full");
        words.add("shop");
        words.add("click-to-show");
        words.add("in-progress");
        words.add("buy-menu");
        words.add("wood-railgun");
        words.add("stone-railgun");
        words.add("iron-railgun");
        words.add("gold-railgun");
        words.add("diamond-railgun");
        words.add("wood-railgun-description");
        words.add("stone-railgun-description");
        words.add("iron-railgun-description");
        words.add("gold-railgun-description");
        words.add("diamond-railgun-description");
        this.path = path;
        this.def = start;
    }

    /**
     * Set the {@code YamlConfiguration} to use.
     *
     * @param config The config to set.
     */
    public static void setFile(YamlConfiguration config) {
        LANG = config;
    }

    @
            Override
    public String toString() {
        if (words.contains(this.path))
            return ChatColor.translateAlternateColorCodes('&', LANG.getString(this.path, def));
        return "\ufffd\ufffde[\ufffd\ufffdb\ufffd\ufffdlQuake\ufffd\ufffdc\ufffd\ufffdlDM\ufffd\ufffdr\ufffd\ufffde]" + ChatColor.translateAlternateColorCodes('&', LANG.getString(this.path, def));
    }

    /**
     * Get the default value of the path.
     *
     * @return The default value of the path.
     */
    public String getDefault() {
        return this.def;
    }

    /**
     * Get the path to the string.
     *
     * @return The path to the string.
     */
    public String getPath() {
        return this.path;
    }
}
