package com.tigerhix.quake;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import com.tigerhix.quake.Main;

public class Utils {

    public static Logger log;
    public static Random randomizer = new Random();

    public static Main main;

    public Utils(Main main) {
        Utils.main = main;
    }

    public static void joinGame(final Player p, String name) {
        // Teleport
        QuakePlayer player = getQuakePlayer(p.getName());
        QuakeArena arena = getQuakeArena(name);
        player.arena = name;
        player.score = 0;
        arena.players.add(p.getName());
        broadcastPlayers(name, Lang.PLAYER_JOINED.toString().replace("%player", p.getName()));
        randomTeleport(p);
        // Toggle inventory needs a delay anyway
        Bukkit.getScheduler().scheduleSyncDelayedTask(main, new Runnable() {

            @
            Override
            public void run() {
                main.inventories.put(p.getName(), InventoryToString(p.getInventory()));
                p.getInventory().clear();
            }

        }, 10);
        // Check requirement
        if (arena.players.size() == main.getConfig().getInt("arenas." + name + ".min")) {
            readyGame(name, false);
        } else if (arena.players.size() == main.getConfig().getInt("arenas." + name + ".max")) {
            readyGame(name, true);
        }
    }

    public static void leaveGame(Player p) {
        // Teleport
        QuakePlayer player = getQuakePlayer(p.getName());
        QuakeArena arena = getQuakeArena(player.arena);
        player.arena = "";
        player.score = 0;
        // Finished check - Avoid concurrentModificationException
        if (arena.status != "finished") {
            arena.players.remove(p.getName());
            broadcastPlayers(arena.name, Lang.PLAYER_LEAVED.toString().replace("%player", p.getName()));
        }
        // If arena is empty
        if (arena.players.size() == 0) {
            // Cancel 30-sec/10-sec wait
            if (arena.waitingID != 0) {
                Bukkit.getScheduler().cancelTask(arena.waitingID);
                arena.waitingID = 0;
            }
            // Cancel scoreboard
            if (arena.scoreboardID != 0) {
                Bukkit.getScheduler().cancelTask(arena.scoreboardID);
                arena.scoreboardID = 0;
            }
            // Change status
            arena.status = "waiting";
            arena.seconds = 0;
        }
        lobbyTeleport(p);
        // Toggle inventory
        p.getInventory().clear();
        Inventory i = StringToInventory(main.inventories.get(p.getName()));
        p.getInventory().setContents(i.getContents());
        // Remove scoreboard
        p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        // Remove potion effects
        for (PotionEffect effect: p.getActivePotionEffects()) {
            p.removePotionEffect(effect.getType());
        }
        // Remove level
        p.setLevel(0);
    }

    public static void readyGame(final String name, Boolean isMax) {
        final QuakeArena arena = getQuakeArena(name);
        if (isMax) {
            // Cancel 30-sec wait
            Bukkit.getScheduler().cancelTask(arena.waitingID);
            // Set seconds
            arena.seconds = main.getConfig().getInt("general.max-waiting-time");
            broadcastPlayers(name, Lang.MET_MAX_REQUIREMENT.toString().replace("%seconds", String.valueOf(arena.seconds)));
            // Start 10-sec wait
            arena.waitingID = Bukkit.getScheduler().scheduleSyncRepeatingTask(main, new Runnable() {

                @
                Override
                public void run() {
                    arena.seconds--;
                    if (arena.seconds == 0) {
                        startGame(name);
                        Bukkit.getScheduler().cancelTask(arena.waitingID);
                    }
                    for (String p: arena.players) {
                        main.getServer().getPlayer(p).setLevel(arena.seconds);
                    }
                }

            }, 0L, 20);

        } else {
            // Set seconds
            arena.seconds = main.getConfig().getInt("general.min-waiting-time");
            broadcastPlayers(name, Lang.MET_MIN_REQUIREMENT.toString().replace("%seconds", String.valueOf(arena.seconds)));
            // Start 30-sec wait
            arena.waitingID = Bukkit.getScheduler().scheduleSyncRepeatingTask(main, new Runnable() {

                @
                Override
                public void run() {
                    arena.seconds--;
                    if (arena.seconds == 0) {
                        startGame(name);
                        Bukkit.getScheduler().cancelTask(arena.waitingID);
                    }
                    for (String p: arena.players) {
                        main.getServer().getPlayer(p).setLevel(arena.seconds);
                    }
                }

            }, 0L, 20);
        }
    }

    public static void startGame(String name) {

        final QuakeArena arena = getQuakeArena(name);
        arena.status = "started";
        broadcastPlayers(name, Lang.MATCH_STARTED.toString());
        // Register scoreboard
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard board = manager.getNewScoreboard();
        Objective objective = board.registerNewObjective("score", "playerKillCount");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.setDisplayName(Lang.LEADERBOARD.toString());
        for (String pname: arena.players) {
            final Player p = main.getServer().getPlayer(pname);
            final QuakePlayer player = getQuakePlayer(pname);
            // Teleport
            randomTeleport(p);
            // Give hoe
            if (getHoe(p.getName()) == null) {
            	setHoe(p.getName(), "wood");
            }
            ItemStack hoe = new ItemStack(Material.WOOD_HOE, 1);
            if (getHoe(p.getName()) == "wood") {
            	hoe = new ItemStack(Material.WOOD_HOE, 1);
            	ItemMeta meta = hoe.getItemMeta();
            	meta.setDisplayName(Lang.WOOD_HOE.toString());
            	List <String> lore = new ArrayList <String>();
            	lore.add(Lang.WOOD_HOE_DESCRIPTION.toString());
            	meta.setLore(lore);
            	hoe.setItemMeta(meta);
            } else if (getHoe(p.getName()) == "stone") {
            	hoe = new ItemStack(Material.STONE_HOE, 1);
            	ItemMeta meta = hoe.getItemMeta();
            	meta.setDisplayName(Lang.STONE_HOE.toString());
            	List <String> lore = new ArrayList <String>();
            	lore.add(Lang.STONE_HOE_DESCRIPTION.toString());
            	meta.setLore(lore);
            	hoe.setItemMeta(meta);
            } else if (getHoe(p.getName()) == "iron") {
            	hoe = new ItemStack(Material.IRON_HOE, 1);
            	ItemMeta meta = hoe.getItemMeta();
            	meta.setDisplayName(Lang.IRON_HOE.toString());
            	List <String> lore = new ArrayList <String>();
            	lore.add(Lang.IRON_HOE_DESCRIPTION.toString());
            	meta.setLore(lore);
            	hoe.setItemMeta(meta);
            } else if (getHoe(p.getName()) == "gold") {
            	hoe = new ItemStack(Material.GOLD_HOE, 1);
            	ItemMeta meta = hoe.getItemMeta();
            	meta.setDisplayName(Lang.GOLD_HOE.toString());
            	List <String> lore = new ArrayList <String>();
            	lore.add(Lang.GOLD_HOE_DESCRIPTION.toString());
            	meta.setLore(lore);
            	hoe.setItemMeta(meta);
            } else if (getHoe(p.getName()) == "diamond") {
            	hoe = new ItemStack(Material.DIAMOND_HOE, 1);
            	ItemMeta meta = hoe.getItemMeta();
            	meta.setDisplayName(Lang.DIAMOND_HOE.toString());
            	List <String> lore = new ArrayList <String>();
            	lore.add(Lang.DIAMOND_HOE_DESCRIPTION.toString());
            	meta.setLore(lore);
            	hoe.setItemMeta(meta);
            } else { // Not possible if everything is going well. Just wrote those codes to prevent NPE
            	hoe = new ItemStack(Material.WOOD_HOE, 1);
            	ItemMeta meta = hoe.getItemMeta();
            	meta.setDisplayName(Lang.WOOD_HOE.toString());
            	List <String> lore = new ArrayList <String>();
            	lore.add(Lang.WOOD_HOE_DESCRIPTION.toString());
            	meta.setLore(lore);
            	hoe.setItemMeta(meta);
            }
            p.getInventory().addItem(hoe);
            // Potion effects
            setPotionEffects(p);
            // Set scoreboard
            final Score score = objective.getScore(p);
            score.setScore(0);
            p.setScoreboard(manager.getNewScoreboard());
            p.setScoreboard(board);
            // Set repeat
            arena.scoreboardID = main.getServer().getScheduler().scheduleSyncRepeatingTask(main, new BukkitRunnable() {

                @
                Override
                public void run() {

                    // EXP
                    if (main.exp.tryUse(p)) {
                        if (p.getExp() > 0) {
                            p.setExp((float)(p.getExp() - 0.1));
                        }
                    }
                    // Update score

                    score.setScore(player.score);

                }

            }, 0, 3);
        }
    }

    public static void stopGame(String name, String winnername) {
        final Player p = main.getServer().getPlayer(winnername);
        // Message
        broadcastPlayers(name, Lang.MATCH_ENDED.toString());
        broadcastPlayers(name, Lang.PLAYER_WON.toString().replace("%player", p.getName()));
        // Add points for winner
        setPoints(winnername, getPoints(winnername) + main.getConfig().getInt("general.points.win"));
        // Play firework
        final int fireworkTask = main.getServer().getScheduler().scheduleSyncRepeatingTask(main, new Runnable() {

            @
            Override
            public void run() {
                Firework firework = p.getWorld().spawn(p.getLocation(), Firework.class);
                FireworkMeta data = (FireworkMeta) firework.getFireworkMeta();
                data.addEffects(FireworkEffect.builder().withColor(Color.AQUA).with(Type.BALL_LARGE).build());
                data.setPower(1);
                firework.setFireworkMeta(data);
            }

        }, 0, 2);
        // Set arena
        final QuakeArena arena = getQuakeArena(name);
        arena.status = "finished";
        // Set delay
        arena.scoreboardID = main.getServer().getScheduler().scheduleSyncDelayedTask(main, new BukkitRunnable() {

            @
            Override
            public void run() {

                // Cancel task
                main.getServer().getScheduler().cancelTask(fireworkTask);
                // Set players
                for (String p: arena.players) {
                    leaveGame(main.getServer().getPlayer(p));
                }
                // Set arena
                arena.status = "waiting";
                arena.players = new ArrayList < String > ();
                arena.waitingID = 0;
                arena.scoreboardID = 0;
                arena.seconds = 0;


            }

        }, 20 * main.getConfig().getInt("general.after-waiting-time"));
    }

    public static void killPlayer(String killername, String killedname) {
        // Kill player
        main.getServer().getPlayer(killedname).setHealth(0.0);
        QuakePlayer killer = getQuakePlayer(killername);
        QuakePlayer killed = getQuakePlayer(killedname);
        // Add score
        killer.score++;
        killed.died = true;
        // Broadcast
        Utils.broadcastPlayers(killer.arena, Lang.PLAYER_KILLED_PLAYER.toString().replace("%killed", killed.name).replace("%killer", killer.name));
        // Check if killer reaches winning points
        if (killer.score == main.getConfig().getInt("general.winning-points")) {
            stopGame(killer.arena, killer.name);
        }
        // Add points for killer
        setPoints(killername, getPoints(killername) + main.getConfig().getInt("general.points.kill"));
    }

    // For join before match
    public static void randomTeleport(Player p) {
        QuakePlayer player = getQuakePlayer(p.getName());
        List < Location > spawns = Utils.getQuakeArena(player.arena).spawns;
        p.teleport(spawns.get(Utils.randomInt(0, spawns.size() - 1)));
    }

    // For respawn in match
    public static Location getRandomTeleport(Player p) {
        QuakePlayer player = getQuakePlayer(p.getName());
        List < Location > spawns = Utils.getQuakeArena(player.arena).spawns;
        return spawns.get(Utils.randomInt(0, spawns.size() - 1));
    }

    public static void lobbyTeleport(Player p) {
        if (main.lobbyLoc != null) {
            p.teleport(main.lobbyLoc);
        }
    }

    public static void broadcastPlayers(String name, String message) {
        QuakeArena arena = getQuakeArena(name);
        List < String > players = arena.players;
        for (int index = 0; index < players.size(); index++) {
            if (main.getServer().getPlayer(players.get(index)) != null) {
                Player p = main.getServer().getPlayer(players.get(index));
                p.sendMessage(message);
            }
        }
    }

    public static void setPotionEffects(Player p) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1), true);
    }

    public static int getPoints(String p) {
        if (!main.vaultEnabled) {
            if (main.getConfig().get("players." + p + ".points") != null) {
                return main.getConfig().getInt("players." + p + ".points");
            }
        } else {
            return (int) main.economy.getBalance(p);
        }
        return 0;
    }

    public static int getCoins(String p) {
        if (main.getConfig().get("players." + p + ".coins") != null) {
            return main.getConfig().getInt("players." + p + ".coins");
        } else {
            return 0;
        }
    }
    
    public static String getHoe(String p) {
    	if (main.getConfig().get("players." + p + ".railgun") != null) {
            return main.getConfig().getString("players." + p + ".railgun");
        } else {
            return null;
        }
    }

    public static String getSelectedArena(Player p) {
        if (main.getConfig().get("players." + p.getName() + ".selected-arena") != null) {
            return main.getConfig().getString("players." + p.getName() + ".selected-arena");
        } else {
            return null;
        }
    }

    public static void setPoints(String p, int points) {
        if (!main.vaultEnabled) {
            main.getConfig().set("players." + p + ".points", points);
            main.saveConfig();
        } else {
            main.economy.withdrawPlayer(p, main.economy.getBalance(p));
            main.economy.depositPlayer(p, points);
        }
    }

    public static void setCoins(String p, int coins) {
        main.getConfig().set("players." + p + ".coins", coins);
        main.saveConfig();
    }
    
    public static void setHoe(String p, String hoe) {
    	main.getConfig().set("players." + p + ".railgun", hoe);
    	main.saveConfig();
    }

    public static void setSelectedArena(String p, String name) {
        main.getConfig().set("players." + p + ".selected-arena", name);
        main.saveConfig();
    }

    public static void setupScoreboardTimer() {
        Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(main, new Runnable() {
            public void run() {
                for (Player p: Bukkit.getOnlinePlayers()) {
                    QuakePlayer player = getQuakePlayer(p.getName());
                    if (player.arena == "") {
                        if (p.getWorld() == main.lobbyLoc.getWorld() && main.getConfig().getBoolean("general.stats.enabled")) {
                            // Player is in lobby
                            setStatsScoreboard(p);
                        } else {
                            // Player is in other arena
                            // TODO: Other mini-games
                            p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
                        }
                    }
                }
            }
        }, 15, 10);
    }

    public static void setupSignTimer() {
        Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(main, new Runnable() {
            public void run() {
                for (Location loc: main.signLocs) {
                    BlockState state = loc.getBlock().getState();
                    if (state instanceof Sign) {
                        Sign s = (Sign) state;
                        QuakeArena arena = main.arenas.get(ChatColor.stripColor(s.getLine(1)));
                        if (arena.status != null) {
                            if (arena.status == "waiting") {
                                if (arena.players.size() < arena.max) {
                                    s.setLine(0, ChatColor.DARK_GREEN + "[" + Lang.JOIN.toString() + "]");
                                } else {
                                    s.setLine(0, ChatColor.DARK_RED + "[" + Lang.FULL.toString() + "]");
                                }
                            } else {
                                s.setLine(0, ChatColor.DARK_RED + "[" + Lang.IN_PROGRESS.toString() + "]");
                            }
                        }
                        s.setLine(2, arena.players.size() + "/" + arena.max);
                        s.update();
                    }
                }
            }
        }, 40, 10);
    }

    public static void setArenaScoreboard(Player p) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard board = manager.getNewScoreboard();

        Objective objective = board.registerNewObjective("quake", "dummy");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.setDisplayName(Lang.LEADERBOARD.toString());

        Score playerScore = objective.getScore(p);
        playerScore.setScore(getQuakePlayer(p.getName()).score);

        p.setScoreboard(board);
    }

    public static void setStatsScoreboard(Player p) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard board = manager.getNewScoreboard();

        Objective objective = board.registerNewObjective("quake", "dummy");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.setDisplayName(Lang.STATS.toString());

        Score pointsScore = objective.getScore(Bukkit.getOfflinePlayer(Lang.POINTS.toString()));
        pointsScore.setScore(Utils.getPoints(p.getName()));

        if (!main.vaultEnabled) {
            Score coinsScore = objective.getScore(Bukkit.getOfflinePlayer(Lang.COINS.toString()));
            coinsScore.setScore(Utils.getCoins(p.getName()));
        }

        p.setScoreboard(board);
    }

    public static boolean isQuakePlayer(String p) {
        if (main.players != null) {
            if (main.players.containsKey(p)) {
                return true;
            }
        }
        return false;
    }

    public static QuakePlayer getQuakePlayer(String p) {
        if (main.players.containsKey(p)) {
            return main.players.get(p);
        }
        return null;
    }

    public static QuakeArena getQuakeArena(String p) {
        if (main.arenas.containsKey(p)) {
            return main.arenas.get(p);
        }
        return null;
    }

    public static String locationToString(Location loc, Boolean isSign) {
        if (isSign) {
            return loc.getX() + "," + loc.getY() + "," + loc.getZ();
        } else {
            return loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ() + "," + loc.getYaw() + "," + loc.getPitch();
        }
    }

    public static Location stringToLocation(String str, Boolean isSign) {
        String[] stringListLoc = str.split(",");
        if (isSign) {
            if (stringListLoc.length == 3) {
                Location loc = new Location(main.lobbyLoc.getWorld(), Double.parseDouble(stringListLoc[0]), Double.parseDouble(stringListLoc[1]), Double.parseDouble(stringListLoc[2]));
                return loc;
            } else {
                return null;
            }
        } else {
            if (stringListLoc.length == 6) {
                Location loc = new Location(main.getServer().getWorld(stringListLoc[0]), Double.parseDouble(stringListLoc[1]), Double.parseDouble(stringListLoc[2]), Double.parseDouble(stringListLoc[3]), Float.parseFloat(stringListLoc[4]), Float.parseFloat(stringListLoc[5]));
                return loc;
            } else {
                return null;
            }
        }
    }

    public static String InventoryToString(Inventory invInventory) {
        String serialization = invInventory.getSize() + ";";
        for (int i = 0; i < invInventory.getSize(); i++) {
            ItemStack is = invInventory.getItem(i);
            if (is != null) {
                String serializedItemStack = new String();

                String isType = String.valueOf(is.getType().getId());
                serializedItemStack += "t@" + isType;

                if (is.getDurability() != 0) {
                    String isDurability = String.valueOf(is.getDurability());
                    serializedItemStack += ":d@" + isDurability;
                }

                if (is.getAmount() != 1) {
                    String isAmount = String.valueOf(is.getAmount());
                    serializedItemStack += ":a@" + isAmount;
                }

                Map < Enchantment, Integer > isEnch = is.getEnchantments();
                if (isEnch.size() > 0) {
                    for (Entry < Enchantment, Integer > ench: isEnch.entrySet()) {
                        serializedItemStack += ":e@" + ench.getKey().getId() + "@" + ench.getValue();
                    }
                }

                serialization += i + "#" + serializedItemStack + ";";
            }
        }
        return serialization;
    }

    public static Inventory StringToInventory(String invString) {
        String[] serializedBlocks = invString.split(";");
        String invInfo = serializedBlocks[0];
        Inventory deserializedInventory = Bukkit.getServer().createInventory(null, Integer.valueOf(invInfo));

        for (int i = 1; i < serializedBlocks.length; i++) {
            String[] serializedBlock = serializedBlocks[i].split("#");
            int stackPosition = Integer.valueOf(serializedBlock[0]);

            if (stackPosition >= deserializedInventory.getSize()) {
                continue;
            }

            ItemStack is = null;
            Boolean createdItemStack = false;

            String[] serializedItemStack = serializedBlock[1].split(":");
            for (String itemInfo: serializedItemStack) {
                String[] itemAttribute = itemInfo.split("@");
                if (itemAttribute[0].equals("t")) {
                    is = new ItemStack(Material.getMaterial(Integer.valueOf(itemAttribute[1])));
                    createdItemStack = true;
                } else if (itemAttribute[0].equals("d") && createdItemStack) {
                    is.setDurability(Short.valueOf(itemAttribute[1]));
                } else if (itemAttribute[0].equals("a") && createdItemStack) {
                    is.setAmount(Integer.valueOf(itemAttribute[1]));
                } else if (itemAttribute[0].equals("e") && createdItemStack) {
                    is.addEnchantment(Enchantment.getById(Integer.valueOf(itemAttribute[1])), Integer.valueOf(itemAttribute[2]));
                }
            }
            deserializedInventory.setItem(stackPosition, is);
        }

        return deserializedInventory;
    }

    public static int randomInt(int aStart, int aEnd) {
        Random aRandom = new Random();
        if (aStart > aEnd) {
            throw new IllegalArgumentException("Start cannot exceed End.");
        }
        // Get the range, casting to long to avoid overflow problems
        long range = (long) aEnd - (long) aStart + 1;
        // Compute a fraction of the range, 0 <= frac < range
        long fraction = (long)(range * aRandom.nextDouble());
        int randomNumber = (int)(fraction + aStart);
        return randomNumber;
    }
    
    public static Type getType(String str) {
    	Type type = Type.BALL;
    	switch (str) {
    	case "BALL":
    		type = Type.BALL;
    	case "BALL_LARGE":
    		type = Type.BALL_LARGE;
    	case "BURST":
    		type = Type.BURST;
    	case "CREEPER":
    		type = Type.CREEPER;
    	case "STAR":
    		type = Type.STAR;
    	}
    	return type;
    }

    public static Color getColor(String str) {
        Color color = Color.AQUA;
        switch (str) {
        case "AQUA":
            color = Color.AQUA;
        case "BLACK":
            color = Color.BLACK;
        case "BLUE":
            color = Color.BLUE;
        case "FUCHSIA":
            color = Color.FUCHSIA;
        case "GRAY":
            color = Color.GRAY;
        case "GREEN":
            color = Color.GREEN;
        case "LIME":
            color = Color.LIME;
        case "MAROON":
            color = Color.MAROON;
        case "NAVY":
            color = Color.NAVY;
        case "OLIVE":
            color = Color.OLIVE;
        case "ORANGE":
            color = Color.ORANGE;
        case "PURPLE":
            color = Color.PURPLE;
        case "RED":
            color = Color.RED;
        case "SILVER":
            color = Color.SILVER;
        case "TEAL":
            color = Color.TEAL;
        case "WHITE":
            color = Color.WHITE;
        case "YELLOW":
            color = Color.YELLOW;
        }
        return color;
    }

}