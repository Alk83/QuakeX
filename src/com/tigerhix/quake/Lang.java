package com.tigerhix.quake;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * An enum for requesting strings from the language file.
 * @author gomeow
 */

public enum Lang {
   
    STATS("stats", "Stats"),
    LEADERBOARD("leaderboard", "Leaderboard"),
    POINTS("points", "Points"),
    COINS("coins", "Coins"),
    KILLS("kills", "Kills"),
    
    JOIN("join", "Join"),
    FULL("full", "Full"),
    IN_PROGRESS("in-progress", "In progress"),
    
    BUY_MENU("buy-menu", "Buy menu"),
    
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
    
    ARENA_NOT_FOUND("arena-not-found", "&4* Cannot find the arena."),
    ARENA_NOT_FOUND_CANT_CREATE("arena-not-found-cannot-create", "&4* Cannot find the arena. Join sign not set."),
    ARENA_NOT_SELECTED("arena-not-selected", "&4* No arena selected. Please use '/quake select' to select an arena."),
    ARENA_ALREADY_EXISTS("arena-already-exists", "&4* Arena already exists!"),
    ALREADY_JOINED("already-joined", "&4* You are already in-game! Please use '/quake leave' to leave current arena."),
    ARENA_NOT_JOINED("arena-not-joined", "&4* You are not in-game!"),
    ARENA_ALREADY_STARTED("arena-already-started", "&4* Arena is already started!"),
    ARENA_FULL("arena-full", "&4* Arena is full!"),
    ARENA_LEAVED("arena-leaved", "&4* You leaved the arena."),
    NO_COMMANDS("no-commands", "&4* Commands are disabled in arenas!"),
    
    PLAYER_JOINED("player-joined", "&7* &b%player &7joined the game."),
    PLAYER_LEAVED("player-leaved", "&7* &b%player &7leaved the game."),
    PLAYER_KILLED_PLAYER("player-killed-player", "&7* &b%killer &7gibbed &b%killed&7!"),
    PLAYER_WON("player-won", "&7* &b%player &7won the game!"),
    
    MATCH_STARTED("match-started", "&7* Match started! Right click your railgun to instant-kill others!"),
    MATCH_ENDED("match-ended", "&7* Match ended!"),
    
    MET_MIN_REQUIREMENT("met-min-requirement", "&7* Met minimum player requirement! Match starts in %seconds seconds.."),
    MET_MAX_REQUIREMENT("met-max-requirement", "&7* Met maximum player requirement! Match starts in %seconds seconds.."),
    CANT_MET_MINIMUM_REQUIREMENT("cant-met-minimum-requirement", "&4* Someone leaved and can't meet minimum player requirement right now. Cancelled starting of match..."),
    
    SHOP_ALREADY_PURCHASED("shop-already-purchased", "&4Already purchased"),
    SHOP_RELOAD_TIME("shop-reload-time", "&eReload time: "),
    SHOP_RELOAD_TIME_FORMAT("shop-reload-time-format", "&b%times"),
    SHOP_PRICE("shop-price", "&ePrice: "),
    SHOP_PRICE_FORMAT("shop-price-format", "&b$%price"),
    
    PURCHASED("purchased", "&7* Successfully purchased."),
    
    
    ARENA_CREATED("arena-created", "&7* Arena created and auto-selected. Please use '/quake addspawn' to add spawnpoints."),
    ARENA_SELECTED("arena-selected", "&7* Arena selected."),
    JOIN_SIGN_SET("join-sign-set", "&7* Join sign set."),
    JOIN_SIGN_REMOVED("join-sign-removed", "&7* Join sign removed."),
    NOT_IN_LOBBY_WORLD("not-in-lobby-world", "&4* You are not in lobby world!"),
    SPAWN_SET("spawn-set", "&7* Spawn set."),
    LOBBY_SET("lobby-set", "&7* Lobby set. Now you can put join signs in this world."),
    STATS_CHANGED("stats-changed", "&7* Stats changed."),
    WRONG_COMMAND("wrong-command", "&4* Unknown command.");

    private String path;
    private String def;
    private static YamlConfiguration LANG;

    /**
     * Lang enum constructor.
     * @param path The string path.
     * @param start The default string.
     */
    Lang(String path, String start) {
        this.path = path;
        this.def = start;
    }

    /**
     * Set the {@code YamlConfiguration} to use.
     * @param config The config to set.
     */
    public static void setFile(YamlConfiguration config) {
        LANG = config;
    }

    @
    Override
    public String toString() {
        return ChatColor.translateAlternateColorCodes('&', LANG.getString(this.path, def));
    }

    /**
     * Get the default value of the path.
     * @return The default value of the path.
     */
    public String getDefault() {
        return this.def;
    }

    /**
     * Get the path to the string.
     * @return The path to the string.
     */
    public String getPath() {
        return this.path;
    }
}