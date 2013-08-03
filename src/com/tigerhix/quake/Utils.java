package com.tigerhix.quake;

import net.minecraft.server.v1_6_R2.Packet62NamedSoundEffect;
import org.bukkit.*;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.craftbukkit.v1_6_R2.entity.CraftPlayer;
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
import org.bukkit.scoreboard.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.logging.Logger;

public class Utils {

    public static Logger log;
    public static Random randomizer = new Random();

    public static Main main;
    public static IconMenu buyMenu;

    public Utils(Main main) {
        Utils.main = main;
    }

    public static void openMenu(Player p) {

        // Setup the buy menu

        List<String> woodDescription = new ArrayList<String>();
        woodDescription.add(Lang.WOOD_HOE_DESCRIPTION.toString());
        woodDescription.add(Lang.SHOP_RELOAD_TIME.toString() + Lang.SHOP_RELOAD_TIME_FORMAT.toString()
                .replace("%time", String.valueOf((float) main.getConfig()
                        .getInt("railguns.wood.reload") / 1000)));
        woodDescription.add(Lang.SHOP_PRICE.toString() + Lang.SHOP_PRICE_FORMAT.toString()
                .replace("%price", "FREE"));
        woodDescription.add(Lang.SHOP_ALREADY_PURCHASED.toString()); // Everyone have a wood hoe right?
        List<String> stoneDescription = new ArrayList<String>();
        stoneDescription.add(Lang.STONE_HOE_DESCRIPTION.toString());
        stoneDescription.add(Lang.SHOP_RELOAD_TIME.toString() + Lang.SHOP_RELOAD_TIME_FORMAT.toString()
                .replace("%time", String.valueOf((float) main.getConfig()
                        .getInt("railguns.stone.reload") / 1000)));
        stoneDescription.add(Lang.SHOP_PRICE.toString() + Lang.SHOP_PRICE_FORMAT.toString()
                .replace("%price", String.valueOf(main.getConfig()
                        .getInt("railguns.stone.price"))));
        List<String> ironDescription = new ArrayList<String>();
        ironDescription.add(Lang.IRON_HOE_DESCRIPTION.toString());
        ironDescription.add(Lang.SHOP_RELOAD_TIME.toString() + Lang.SHOP_RELOAD_TIME_FORMAT.toString()
                .replace("%time", String.valueOf((float) main.getConfig()
                        .getInt("railguns.iron.reload") / 1000)));
        ironDescription.add(Lang.SHOP_PRICE.toString() + Lang.SHOP_PRICE_FORMAT.toString()
                .replace("%price", String.valueOf(main.getConfig()
                        .getInt("railguns.iron.price"))));
        List<String> goldDescription = new ArrayList<String>();
        goldDescription.add(Lang.GOLD_HOE_DESCRIPTION.toString());
        goldDescription.add(Lang.SHOP_RELOAD_TIME.toString() + Lang.SHOP_RELOAD_TIME_FORMAT.toString()
                .replace("%time", String.valueOf((float) main.getConfig()
                        .getInt("railguns.gold.reload") / 1000)));
        goldDescription.add(Lang.SHOP_PRICE.toString() + Lang.SHOP_PRICE_FORMAT.toString()
                .replace("%price", String.valueOf(main.getConfig()
                        .getInt("railguns.gold.price"))));
        List<String> diamondDescription = new ArrayList<String>();
        diamondDescription.add(Lang.DIAMOND_HOE_DESCRIPTION.toString());
        diamondDescription.add(Lang.SHOP_RELOAD_TIME.toString() + Lang.SHOP_RELOAD_TIME_FORMAT.toString()
                .replace("%time", String.valueOf((float) main.getConfig()
                        .getInt("railguns.diamond.reload") / 1000)));
        diamondDescription.add(Lang.SHOP_PRICE.toString() + Lang.SHOP_PRICE_FORMAT.toString()
                .replace("%price", String.valueOf(main.getConfig()
                        .getInt("railguns.diamond.price"))));

        List<String> boughtHoes = getBoughtHoes(p.getName());

        if (boughtHoes == null) { // If null
            setBoughtHoes(p.getName(), "wood"); // Initialize
            boughtHoes = getBoughtHoes(p.getName()); // Refresh
        }

        if (boughtHoes.contains("stone")) stoneDescription.add(Lang.SHOP_ALREADY_PURCHASED.toString());
        if (boughtHoes.contains("iron")) ironDescription.add(Lang.SHOP_ALREADY_PURCHASED.toString());
        if (boughtHoes.contains("gold")) goldDescription.add(Lang.SHOP_ALREADY_PURCHASED.toString());
        if (boughtHoes.contains("diamond")) diamondDescription.add(Lang.SHOP_ALREADY_PURCHASED.toString());

        buyMenu = new IconMenu(Lang.BUY_MENU.toString(), 27, new IconMenu.OptionClickEventHandler() {
            @
                    Override
            public void onOptionClick(final IconMenu.OptionClickEvent evt) {
                Player p = evt.getPlayer();
                if (evt.getPosition() == 0) buyRailgun(p, "wood");
                if (evt.getPosition() == 1) buyRailgun(p, "stone");
                if (evt.getPosition() == 2) buyRailgun(p, "iron");
                if (evt.getPosition() == 3) buyRailgun(p, "gold");
                if (evt.getPosition() == 4) buyRailgun(p, "diamond");
                evt.setWillClose(true);
                evt.setWillDestroy(true);
            }
        }, main)
                .setOption(0, new ItemStack(Material.WOOD_HOE, 1), Lang.WOOD_HOE.toString(), woodDescription.toArray(new String[]{}))
                .setOption(1, new ItemStack(Material.STONE_HOE, 1), Lang.STONE_HOE.toString(), stoneDescription.toArray(new String[]{}))
                .setOption(2, new ItemStack(Material.IRON_HOE, 1), Lang.IRON_HOE.toString(), ironDescription.toArray(new String[]{}))
                .setOption(3, new ItemStack(Material.GOLD_HOE, 1), Lang.GOLD_HOE.toString(), goldDescription.toArray(new String[]{}))
                .setOption(4, new ItemStack(Material.DIAMOND_HOE, 1), Lang.DIAMOND_HOE.toString(), diamondDescription.toArray(new String[]{}));
        buyMenu.open(p);
    }

    public static void buyRailgun(Player p, String hoe) {
        List<String> boughtHoes = getBoughtHoes(p.getName());
        if (boughtHoes.contains(hoe)) { // Purchased
            p.sendMessage(Lang.CHOSEN.toString());
            setHoe(p.getName(), hoe);
            return;
        }
        if (getCoins(p.getName()) < main.getConfig()
                .getInt("railguns." + hoe + ".price")) { // Not enough coins
            p.sendMessage(Lang.NOT_ENOUGH_COINS.toString());
            return;
        }
        setBoughtHoes(p.getName(), hoe);
        setHoe(p.getName(), hoe);
        setCoins(p.getName(), getCoins(p.getName()) - main.getConfig()
                .getInt("railguns." + hoe + ".price"));
        p.sendMessage(Lang.PURCHASED.toString());
    }

    public static void joinGame(final Player p, String name) {
        // Teleport
        QuakePlayer player = getQuakePlayer(p.getName());
        QuakeArena arena = getQuakeArena(name);
        player.arena = name;
        player.score = 0;
        arena.players.add(p.getName());
        broadcastPlayers(name, Lang.PLAYER_JOINED.toString()
                .replace("%player", p.getName()) + " (" + arena.players.size() + "/" + arena.max + ")");
        randomTeleport(p);
        // Toggle inventory needs a delay anyway
        Bukkit.getScheduler()
                .scheduleSyncDelayedTask(main, new Runnable() {

                    @
                            Override
                    public void run() {
                        p.getInventory().remove(Material.EMERALD);
                        main.inventories.put(p.getName(), InventoryToString(p.getInventory()));
                        p.getInventory()
                                .clear();
                        p.updateInventory();
                    }

                }, 10);
        // Check requirement
        if (arena.players.size() == main.getConfig()
                .getInt("arenas." + name + ".min")) {
            readyGame(name, false);
        } else if (arena.players.size() == main.getConfig()
                .getInt("arenas." + name + ".max")) {
            readyGame(name, true);
        }
    }

    public static void leaveGame(final Player p) {
        // Teleport
        QuakePlayer player = getQuakePlayer(p.getName());
        QuakeArena arena = getQuakeArena(player.arena);
        player.arena = "";
        player.score = 0;
        // Finished check - Avoid concurrentModificationException
        if (arena.status != "finished") {
            arena.players.remove(p.getName());
            broadcastPlayers(arena.name, Lang.PLAYER_LEAVED.toString()
                    .replace("%player", p.getName()));
        }
        // If arena is empty
        if (arena.players.size() == 0) {
            // Cancel 30-sec/10-sec wait
            if (arena.waitingID != 0) {
                Bukkit.getScheduler()
                        .cancelTask(arena.waitingID);
                arena.waitingID = 0;
            }
            // Cancel scoreboard
            if (arena.scoreboardID != 0) {
                Bukkit.getScheduler()
                        .cancelTask(arena.scoreboardID);
                arena.scoreboardID = 0;
            }
            // Change status
            arena.status = "waiting";
            arena.seconds = 0;
        }
        // If arena only have one player
        if (arena.players.size() == 1) {
            // If arena is started
            if (arena.status == "started") {
                // Stop game
                stopGame(arena.name, arena.players.get(0));
            }
        }
        // If total amount of players is less than minimum requirements
        if (arena.players.size() < arena.min && arena.status == "waiting") {
            // Cancel 30-sec/10-sec wait
            if (arena.waitingID != 0) {
                Bukkit.getScheduler()
                        .cancelTask(arena.waitingID);
                arena.waitingID = 0;
                broadcastPlayers(arena.name, Lang.CANT_MET_MINIMUM_REQUIREMENT.toString());
            }
        }
        lobbyTeleport(p);
        // Toggle inventory needs a delay anyway
        Bukkit.getScheduler()
                .scheduleSyncDelayedTask(main, new Runnable() {

                    @SuppressWarnings("deprecation")
                    @
                            Override
                    public void run() {
                        p.getInventory()
                                .clear();
                        Inventory i = StringToInventory(main.inventories.get(p.getName()));
                        p.getInventory()
                                .setContents(i.getContents());
                        p.updateInventory();
                        // Remove scoreboard
                        p.setScoreboard(Bukkit.getScoreboardManager()
                                .getNewScoreboard());
                        // Remove potion effects
                        for (PotionEffect effect : p.getActivePotionEffects()) {
                            p.removePotionEffect(effect.getType());
                        }
                        // Remove level
                        p.setLevel(0);
                    }

                }, 10);
    }

    public static void readyGame(final String name, Boolean isMax) {
        final QuakeArena arena = getQuakeArena(name);
        if (isMax) {
            // Cancel 30-sec wait
            Bukkit.getScheduler()
                    .cancelTask(arena.waitingID);
            // Set seconds
            arena.seconds = main.getConfig()
                    .getInt("general.max-waiting-time") + 1;
            broadcastPlayers(name, Lang.MET_MAX_REQUIREMENT.toString()
                    .replace("%seconds", String.valueOf(arena.seconds - 1)));
            // Start 10-sec wait
            arena.waitingID = Bukkit.getScheduler()
                    .scheduleSyncRepeatingTask(main, new Runnable() {

                        @
                                Override
                        public void run() {
                            arena.seconds--;
                            if (arena.seconds == 0) {
                                startGame(name);
                                Bukkit.getScheduler()
                                        .cancelTask(arena.waitingID);
                            } else {
                                if (arena.seconds <= 10) {
                                broadcastPlayers(arena.name, Lang.MATCH_IS_STARTING_IN.toString().replace("%seconds", String.valueOf(arena.seconds)));
                                }
                                }
                            for (String pname : arena.players) {
                                Player p = main.getServer()
                                        .getPlayer(pname);
                                p.setLevel(arena.seconds);
                                if (arena.seconds <= 10 && arena.seconds > 0)
                                    p.getWorld().playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 0.5F);
                            }
                        }

                    }, 0L, 20);

        } else {
            // Set seconds
            arena.seconds = main.getConfig()
                    .getInt("general.min-waiting-time") + 1;
            broadcastPlayers(name, Lang.MET_MIN_REQUIREMENT.toString()
                    .replace("%seconds", String.valueOf(arena.seconds - 1)));
            // Start 30-sec wait
            arena.waitingID = Bukkit.getScheduler()
                    .scheduleSyncRepeatingTask(main, new Runnable() {

                        @
                                Override
                        public void run() {
                            arena.seconds--;
                            if (arena.seconds == 0) {
                                startGame(name);
                                Bukkit.getScheduler()
                                        .cancelTask(arena.waitingID);
                            } else {
                                if (arena.seconds <= 10) {
                                broadcastPlayers(arena.name, Lang.MATCH_IS_STARTING_IN.toString().replace("%seconds", String.valueOf(arena.seconds)));
                                }
                            }
                            for (String pname : arena.players) {
                                Player p = main.getServer()
                                        .getPlayer(pname);
                                p.setLevel(arena.seconds);
                                if (arena.seconds <= 10 && arena.seconds > 0)
                                    p.getWorld().playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 0.5F);
                            }
                        }

                    }, 20, 20);
        }
    }

    public static void startGame(String name) {

        final QuakeArena arena = getQuakeArena(name);
        arena.status = "started";
        broadcastPlayers(name, Lang.MATCH_STARTED.toString());
        // Register scoreboard
        arena.board = arena.manager.getNewScoreboard();
        arena.objective = arena.board.registerNewObjective("score", "playerKillCount");
        arena.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        arena.objective.setDisplayName(Lang.LEADERBOARD.toString());
        for (String pname : arena.players) {
            final Player p = main.getServer()
                    .getPlayer(pname);
            final QuakePlayer player = getQuakePlayer(pname);
            // Teleport
            randomTeleport(p);
            // Play sound
            p.getWorld().playSound(p.getLocation(), Sound.NOTE_PLING, 2F, 1F);
            // Give hoe
            if (getHoe(p.getName()) == null) {
                setHoe(p.getName(), "wood");
            }
            ItemStack hoe = new ItemStack(Material.WOOD_HOE, 1);
            if (getHoe(p.getName()).equalsIgnoreCase("wood")) {
                hoe = new ItemStack(Material.WOOD_HOE, 1);
                ItemMeta meta = hoe.getItemMeta();
                meta.setDisplayName(Lang.WOOD_HOE.toString());
                List<String> lore = new ArrayList<String>();
                lore.add(Lang.WOOD_HOE_DESCRIPTION.toString());
                meta.setLore(lore);
                hoe.setItemMeta(meta);
            } else if (getHoe(p.getName()).equalsIgnoreCase("stone")) {
                hoe = new ItemStack(Material.STONE_HOE, 1);
                ItemMeta meta = hoe.getItemMeta();
                meta.setDisplayName(Lang.STONE_HOE.toString());
                List<String> lore = new ArrayList<String>();
                lore.add(Lang.STONE_HOE_DESCRIPTION.toString());
                meta.setLore(lore);
                hoe.setItemMeta(meta);
            } else if (getHoe(p.getName()).equalsIgnoreCase("iron")) {
                hoe = new ItemStack(Material.IRON_HOE, 1);
                ItemMeta meta = hoe.getItemMeta();
                meta.setDisplayName(Lang.IRON_HOE.toString());
                List<String> lore = new ArrayList<String>();
                lore.add(Lang.IRON_HOE_DESCRIPTION.toString());
                meta.setLore(lore);
                hoe.setItemMeta(meta);
            } else if (getHoe(p.getName()).equalsIgnoreCase("gold")) {
                hoe = new ItemStack(Material.GOLD_HOE, 1);
                ItemMeta meta = hoe.getItemMeta();
                meta.setDisplayName(Lang.GOLD_HOE.toString());
                List<String> lore = new ArrayList<String>();
                lore.add(Lang.GOLD_HOE_DESCRIPTION.toString());
                meta.setLore(lore);
                hoe.setItemMeta(meta);
            } else if (getHoe(p.getName()).equalsIgnoreCase("diamond")) {
                hoe = new ItemStack(Material.DIAMOND_HOE, 1);
                ItemMeta meta = hoe.getItemMeta();
                meta.setDisplayName(Lang.DIAMOND_HOE.toString());
                List<String> lore = new ArrayList<String>();
                lore.add(Lang.DIAMOND_HOE_DESCRIPTION.toString());
                meta.setLore(lore);
                hoe.setItemMeta(meta);
            } else { // Not possible if everything is going well. Just wrote those codes to prevent NPE
                hoe = new ItemStack(Material.WOOD_HOE, 1);
                ItemMeta meta = hoe.getItemMeta();
                meta.setDisplayName(Lang.WOOD_HOE.toString());
                List<String> lore = new ArrayList<String>();
                lore.add(Lang.WOOD_HOE_DESCRIPTION.toString());
                meta.setLore(lore);
                hoe.setItemMeta(meta);
            }
            p.getInventory()
                    .addItem(hoe);
            p.updateInventory();
            // Potion effects
            setPotionEffects(p);
            // Set scoreboard
            final Score score = arena.objective.getScore(p);
            score.setScore(0);
            p.setScoreboard(Bukkit.getScoreboardManager()
                    .getNewScoreboard());
            p.setScoreboard(arena.board);
            // Set repeat
            arena.scoreboardID = main.getServer()
                    .getScheduler()
                    .scheduleSyncRepeatingTask(main, new BukkitRunnable() {

                        @
                                Override
                        public void run() {

                            // EXP
                            if (main.exp.tryUse(p)) {
                                if (p.getExp() > 0) {
                                    p.setExp((float) (p.getExp() - 0.1));
                                }
                            }
                            // Update score

                            score.setScore(player.score);

                        }

                    }, 0, 3);
        }
    }

    public static void stopGame(String name, String winnername) {
        final Player p = main.getServer()
                .getPlayer(winnername);
        // Message
        broadcastPlayers(name, Lang.MATCH_ENDED.toString());
        broadcastPlayers(name, Lang.PLAYER_WON.toString()
                .replace("%player", p.getName()));
        // Add points for winner
        setPoints(winnername, getPoints(winnername) + main.getConfig()
                .getInt("general.points.win"));
        // Play firework
        final List<Color> colorList = new ArrayList<Color>();
        for (int i = 0; i < 20; i++) {
            int index = randomInt(1, 17);
            Color color = getColorByIndex(index);
            colorList.add(color);
        }
        final int fireworkTask = main.getServer()
                .getScheduler()
                .scheduleSyncRepeatingTask(main, new Runnable() {

                    @
                            Override
                    public void run() {
                        Firework firework = p.getWorld()
                                .spawn(p.getLocation(), Firework.class);
                        FireworkMeta data = (FireworkMeta) firework.getFireworkMeta();
                        data.addEffects(FireworkEffect.builder()
                                .withColor(colorList.get(randomInt(0, 19)))
                                .with(Type.BALL_LARGE)
                                .build());
                        data.setPower(1);
                        firework.setFireworkMeta(data);
                    }

                }, 0, 2);
        // Set arena
        final QuakeArena arena = getQuakeArena(name);
        arena.status = "finished";
        // Set delay
        arena.scoreboardID = main.getServer()
                .getScheduler()
                .scheduleSyncDelayedTask(main, new BukkitRunnable() {

                    @
                            Override
                    public void run() {

                        // Cancel task
                        main.getServer()
                                .getScheduler()
                                .cancelTask(fireworkTask);
                        // Set players
                        for (String p : arena.players) {
                            // Reset scores

                            leaveGame(main.getServer()
                                    .getPlayer(p));
                        }
                        // Set arena
                        arena.status = "waiting";
                        arena.players = new ArrayList<String>();
                        arena.waitingID = 0;
                        arena.scoreboardID = 0;
                        arena.seconds = 0;


                    }

                }, 20 * main.getConfig()
                        .getInt("general.after-waiting-time"));
    }

    public static void killPlayer(String killername, String killedname) {
        // Kill player
        main.getServer()
                .getPlayer(killedname)
                .setHealth(0.0);

        QuakePlayer killer = getQuakePlayer(killername);
        QuakePlayer killed = getQuakePlayer(killedname);
        // Add score
        killer.score++;
        killed.died = true;
        // Broadcast
        Utils.broadcastPlayers(killer.arena, Lang.PLAYER_KILLED_PLAYER.toString()
                .replace("%killed", killed.name)
                .replace("%killer", killer.name));
        // Check if killer reaches winning points
        if (killer.score == main.getConfig()
                .getInt("general.winning-points")) {
            stopGame(killer.arena, killer.name);
        }
        // Add points for killer
        setPoints(killername, getPoints(killername) + main.getConfig()
                .getInt("general.points.kill"));
        setCoins(killername, getCoins(killername) + main.getConfig()
                .getInt("general.coins.kill"));
        setKills(killername, getKills(killername) + 1);
    }

    // For join before match
    public static void randomTeleport(Player p) {
        QuakePlayer player = getQuakePlayer(p.getName());
        List<Location> spawns = Utils.getQuakeArena(player.arena)
                .spawns;
        p.teleport(spawns.get(Utils.randomInt(0, spawns.size() - 1)));
    }

    // For respawn in match
    public static Location getRandomTeleport(Player p) {
        QuakePlayer player = getQuakePlayer(p.getName());
        List<Location> spawns = Utils.getQuakeArena(player.arena)
                .spawns;
        return spawns.get(Utils.randomInt(0, spawns.size() - 1));
    }

    public static void lobbyTeleport(Player p) {
        if (main.lobbyLoc != null) {
            p.teleport(main.lobbyLoc);
        }
    }

    public static void broadcastPlayers(String name, String message) {
        QuakeArena arena = getQuakeArena(name);
        List<String> players = arena.players;
        for (int index = 0; index < players.size(); index++) {
            if (main.getServer()
                    .getPlayer(players.get(index)) != null) {
                Player p = main.getServer()
                        .getPlayer(players.get(index));
                p.sendMessage(message);
            }
        }
    }

    public static void setPotionEffects(Player p) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1), true);
    }

    public static int getPoints(String p) {
        if (main.getConfig()
                .get("players." + p + ".points") != null) {
            return main.getConfig()
                    .getInt("players." + p + ".points");
        } else {
            return 0;
        }
    }

    public static int getCoins(String p) {
        if (!main.vaultEnabled) {
            if (main.getConfig()
                    .get("players." + p + ".coins") != null) {
                return main.getConfig()
                        .getInt("players." + p + ".coins");
            }
        } else {
            return (int) main.economy.getBalance(p);
        }
        return 0;
    }

    public static int getKills(String p) {
        if (main.getConfig()
                .get("players." + p + ".kills") != null) {
            return main.getConfig()
                    .getInt("players." + p + ".kills");
        } else {
            return 0;
        }
    }

    public static String getHoe(String p) {
        if (main.getConfig()
                .get("players." + p + ".railgun") != null) {
            return main.getConfig()
                    .getString("players." + p + ".railgun");
        } else {
            return null;
        }
    }

    public static List<String> getBoughtHoes(String p) {
        if (main.getConfig()
                .get("players." + p + ".bought-hoes") != null) {
            return main.getConfig()
                    .getStringList("players." + p + ".bought-hoes");
        } else {
            return null;
        }
    }

    public static String getSelectedArena(Player p) {
        if (main.getConfig()
                .get("players." + p.getName() + ".selected-arena") != null) {
            return main.getConfig()
                    .getString("players." + p.getName() + ".selected-arena");
        } else {
            return null;
        }
    }

    public static void setPoints(String p, int points) {
        main.getConfig()
                .set("players." + p + ".points", points);
        main.saveConfig();
    }

    public static void setCoins(String p, int coins) {
        if (!main.vaultEnabled) {
            main.getConfig()
                    .set("players." + p + ".coins", coins);
            main.saveConfig();
        } else {
            main.economy.withdrawPlayer(p, main.economy.getBalance(p));
            main.economy.depositPlayer(p, coins);
        }
    }

    public static void setKills(String p, int kills) {
        main.getConfig()
                .set("players." + p + ".kills", kills);
        main.saveConfig();
    }

    public static void setHoe(String p, String hoe) {
        main.getConfig()
                .set("players." + p + ".railgun", hoe);
        main.saveConfig();
    }

    public static void setBoughtHoes(String p, String hoe) {
        List<String> boughtHoes = main.getConfig()
                .getStringList("players." + p + ".bought-hoes");
        boughtHoes.add(hoe);
        main.getConfig()
                .set("players." + p + ".bought-hoes", boughtHoes);
        main.saveConfig();
    }

    public static void setSelectedArena(String p, String name) {
        main.getConfig()
                .set("players." + p + ".selected-arena", name);
        main.saveConfig();
    }

    public static void setupScoreboardTimer() {
        Bukkit.getServer()
                .getScheduler()
                .scheduleSyncRepeatingTask(main, new Runnable() {
                    public void run() {
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            QuakePlayer player = getQuakePlayer(p.getName());
                            if (player != null && player.arena == "") {
                                if (p.getWorld() == main.lobbyLoc.getWorld() && main.getConfig()
                                        .getBoolean("general.stats.enabled")) {
                                    // Player is in lobby
                                    setStatsScoreboard(p);
                                } else {
                                    // Player is in other arena
                                    // TODO: Other mini-games
                                    p.setScoreboard(Bukkit.getScoreboardManager()
                                            .getNewScoreboard());
                                }
                            }
                        }
                    }
                }, 15, 10);
    }

    public static void setupSignTimer() {
        Bukkit.getServer()
                .getScheduler()
                .scheduleSyncRepeatingTask(main, new Runnable() {
                    public void run() {
                        for (Location loc : main.signLocs) {
                            BlockState state = loc.getBlock()
                                    .getState();
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

    public static void setArenaScoreboardSS(Player p) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard board = manager.getNewScoreboard();

        Objective objective = board.registerNewObjective("quake", "dummy");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.setDisplayName(Lang.LEADERBOARD.toString());

        Score playerScore = objective.getScore(p);
        playerScore.setScore(getQuakePlayer(p.getName())
                .score);

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

        Score coinsScore = objective.getScore(Bukkit.getOfflinePlayer(Lang.COINS.toString()));
        coinsScore.setScore(Utils.getCoins(p.getName()));

        Score killsScore = objective.getScore(Bukkit.getOfflinePlayer(Lang.KILLS.toString()));
        killsScore.setScore(Utils.getKills(p.getName()));

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
            return loc.getWorld()
                    .getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ() + "," + loc.getYaw() + "," + loc.getPitch();
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
                Location loc = new Location(main.getServer()
                        .getWorld(stringListLoc[0]), Double.parseDouble(stringListLoc[1]), Double.parseDouble(stringListLoc[2]), Double.parseDouble(stringListLoc[3]), Float.parseFloat(stringListLoc[4]), Float.parseFloat(stringListLoc[5]));
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

                String isType = String.valueOf(is.getType()
                        .getId());
                serializedItemStack += "t@" + isType;

                if (is.getDurability() != 0) {
                    String isDurability = String.valueOf(is.getDurability());
                    serializedItemStack += ":d@" + isDurability;
                }

                if (is.getAmount() != 1) {
                    String isAmount = String.valueOf(is.getAmount());
                    serializedItemStack += ":a@" + isAmount;
                }

                Map<Enchantment, Integer> isEnch = is.getEnchantments();
                if (isEnch.size() > 0) {
                    for (Entry<Enchantment, Integer> ench : isEnch.entrySet()) {
                        serializedItemStack += ":e@" + ench.getKey()
                                .getId() + "@" + ench.getValue();
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
        Inventory deserializedInventory = Bukkit.getServer()
                .createInventory(null, Integer.valueOf(invInfo));

        for (int i = 1; i < serializedBlocks.length; i++) {
            String[] serializedBlock = serializedBlocks[i].split("#");
            int stackPosition = Integer.valueOf(serializedBlock[0]);

            if (stackPosition >= deserializedInventory.getSize()) {
                continue;
            }

            ItemStack is = null;
            Boolean createdItemStack = false;

            String[] serializedItemStack = serializedBlock[1].split(":");
            for (String itemInfo : serializedItemStack) {
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
        long fraction = (long) (range * aRandom.nextDouble());
        int randomNumber = (int) (fraction + aStart);
        return randomNumber;
    }

    public static Color getColorByIndex(int index) {
        Color color = Color.AQUA;
        switch (index) {
            case 1:
                color = Color.AQUA;
                return color;
            case 2:
                color = Color.BLACK;
                return color;
            case 3:
                color = Color.BLUE;
                return color;
            case 4:
                color = Color.FUCHSIA;
                return color;
            case 5:
                color = Color.GRAY;
                return color;
            case 6:
                color = Color.GREEN;
                return color;
            case 7:
                color = Color.LIME;
                return color;
            case 8:
                color = Color.MAROON;
                return color;
            case 9:
                color = Color.NAVY;
                return color;
            case 10:
                color = Color.OLIVE;
                return color;
            case 11:
                color = Color.ORANGE;
                return color;
            case 12:
                color = Color.PURPLE;
                return color;
            case 13:
                color = Color.RED;
                return color;
            case 14:
                color = Color.SILVER;
                return color;
            case 15:
                color = Color.TEAL;
                return color;
            case 16:
                color = Color.WHITE;
                return color;
            case 17:
                color = Color.YELLOW;
                return color;
            default:
                color = Color.AQUA;
                return color;
        }

    }

    /*public static Type getType(String str) {

        switch (str) {
            case "BALL":
                return Type.STAR;
                break;
            case "BALL_LARGE":
                return Type.BALL_LARGE;
                break;
            case "BURST":
                return Type.BURST;
                break;
            case "CREEPER":
                return Type.CREEPER;
                break;
            default:
                return Type.BALL;
                break;
        }

    }*/

    /**
     * Returns an instance of a FireworkType.
     *
     * @param str Name of the FireworkType
     * @return FireworkEffect.Type
     */
    public static FireworkEffect.Type getfireworkType(String str) {

        return FireworkEffect.Type.valueOf(str.toUpperCase());

    }


/*    //This is a pointless function you are passing in a string to return the Color format why not just do Color.AQUA from
    //You code direct if thats the color you want.
    public static Color getColor(String str) {

        Color color = Color.AQUA;
        switch (str) {
            case "AQUA":
                color = Color.AQUA;
                break;
            case "BLACK":
                color = Color.BLACK;
                break;
            case "BLUE":
                color = Color.BLUE;
                break;
            case "FUCHSIA":
                color = Color.FUCHSIA;
                break;
            case "GRAY":
                color = Color.GRAY;
                break;
            case "GREEN":
                color = Color.GREEN;
                break;
            case "LIME":
                color = Color.LIME;
                break;
            case "MAROON":
                color = Color.MAROON;
                break;
            case "NAVY":
                color = Color.NAVY;
                break;
            case "OLIVE":
                color = Color.OLIVE;
                break;
            case "ORANGE":
                color = Color.ORANGE;
                break;
            case "PURPLE":
                color = Color.PURPLE;
                break;
            case "RED":
                color = Color.RED;
                break;
            case "SILVER":
                color = Color.SILVER;
                break;
            case "TEAL":
                color = Color.TEAL;
                break;
            case "WHITE":
                color = Color.WHITE;
                break;
            default:
                color = Color.YELLOW;
                break;
        }
        return color;
    }*/

    public static void playSound(Player to, String sound, Location loc, float pitch, float volume) {
        ((CraftPlayer) to)
                .getHandle()
                .playerConnection.sendPacket(new Packet62NamedSoundEffect(sound, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), pitch, volume));
    }

    public static void playSound(Player to, String sound, float pitch, float volume) {
        playSound(to, sound, to.getLocation(), pitch, volume);
    }

    public String[] increaseArray(String[] theArray, int increaseBy) {
        int i = theArray.length;
        int n = ++i;
        String[] newArray = new String[n];
        for (int cnt = 0; cnt < theArray.length; cnt++) {
            newArray[cnt] = theArray[cnt];
        }
        return newArray;
    }

}
