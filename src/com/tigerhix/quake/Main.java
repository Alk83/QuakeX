package com.tigerhix.quake;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


@SuppressWarnings("ResultOfMethodCallIgnored")
public class Main extends JavaPlugin {

	/*
	 * CHANGELOG:
     * - Remove lobby-world setting. Replaced with /quake setlobby, a better solution.
	 * - Add shop; buy 5 railguns from shop using coins you got from matches!
	 * - Add VIP support. If an arena needs one more player to reach its max (like 7/8), only VIP can join it
	 * - Add Vault-based economy system; if you enable it, coins will be removed and player use money from Vault to buy things
	 * - Add emerald shop; can be enabled in config.yml
	 * - Add kills count on stats
	 * - Add new commands:
	 * # /quake - Show info about this plugin
	 * # /quake lobby - Teleport you to main lobby
	 * # /quake buy - Open Quake shop
	 * # /quake stats - Show Quake stats
	 * # /quake vip - VIP a player; VIPs can join near-full arena
	 * # /quake setmin [arena] [min] - Set minimum player requirement for arena
	 * # /quake setmax [arena] [max] - Set maximum player requirement for arena
	 * # /quake setlobby - Set current location lobby
	 * # /quake start [arena] - Force start an arena
	 * # /quake stop [arena] - Force stop an arena
	 * # /quake help - Show help page
	 * - Add sounds for countdown when lesser than 10 seconds
	 * - Add ability to double/triple/quadruple/more than 4 kills in one shoot!
	 * - New sounds for shooting and player death; shooting sound has random pitch too, which imitate Hypixel's one
	 * - Now keep inventory on death in matches
	 * - Now player can't break/place blocks in matches
	 * - Now player can't use commands in arenas except /quake leave
	 * - Now player died because of lava or void will have corresponding death messages
	 * - Metrics added. Thanks to Relicum!
	 * - Over 50+ code optimizations. Thanks to IntelliJ! (Wait what?)
	 * - Change the plugin name from QuakeX to QuakeDM
	 * - Fix bugs:
	 * # Inventories doesn't get restored
	 * # Railgun gave to players don't have a custom name
	 * # Arena meets the minimum player requirement and ready to start; 
	 * if any player leaves which cause total amount of players is less than minimum requirement,
	 * match still starts
	 * # Arena started, players leaves when only 1 player is left; match don't stop
	 * # You can see other arena's scoreboard stats if you are playing
	 * # No errors if you typed commands wrong
	 * # No notification if you leaved the game
	 * # Non-OP players can remove join signs
	 * # If you leave the game when your exp bar not full, exp bar bugs
	 * # If you use '/quake lobby' in game, you won't leave the arena
	 */
	
	/*
	 * TODO:
	 * - VIP rank
	 * - Hats and kits
	 * - Regioned arena stats
	 * - Stop players from respawning all in the same area
	 * - Arena limit time
	 */

	public static Logger log;
	public static YamlConfiguration LANG;
	public static File LANG_FILE;

	public HashMap<String, QuakePlayer> players = new HashMap<>();
	public HashMap<String, QuakeArena> arenas = new HashMap<>();

	public HashMap<String, String> inventories = new HashMap<>();
	public HashMap<String, String> match = new HashMap<>();

	public List<String> vips = new ArrayList<>();

	public Boolean vaultEnabled = false;
	public Boolean emeraldEnabled = false;

	public int vipSlots = 1;

	public Location lobbyLoc;

	public List<Location> signLocs = new ArrayList<>();

	public Ability woodShoot = new Ability(1, 1500, TimeUnit.MILLISECONDS);
	public Ability stoneShoot = new Ability(1, 1400, TimeUnit.MILLISECONDS);
	public Ability ironShoot = new Ability(1, 1300, TimeUnit.MILLISECONDS);
	public Ability goldShoot = new Ability(1, 1200, TimeUnit.MILLISECONDS);
	public Ability diamondShoot = new Ability(1, 1100, TimeUnit.MILLISECONDS);
	public Ability exp = new Ability(1, 50, TimeUnit.MILLISECONDS);

	@
			Override
	public void onEnable() {

		//Load Live Metrics
		try {
			Metrics metrics = new Metrics(this);
			metrics.start();
		} catch (IOException e) {

			System.out.println(Arrays.toString(e.getStackTrace()));
		}

		Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {

			@Override
			public void run() {
				loadConfig();
				// Define variables
				if (getConfig().getString("general.lobby.spawn") != null) {
					getLogger().info("Lobby spawn set!");
					String stringLoc = getConfig().getString("general.lobby.spawn");
					lobbyLoc = Utils.stringToLocation(stringLoc, false);
					if (getConfig().getBoolean("general.shop.emerald")) {
						emeraldEnabled = true;
					}
				} else {
					getLogger().info("Lobby spawn not found in config.yml. Automatically set to the main spawn.");
					lobbyLoc = getServer().getWorlds().get(0).getSpawnLocation();
					if (getConfig().getBoolean("general.shop.emerald")) {
						getServer().getLogger().log(Level.WARNING, "Plugin WILL NOT give emerald shop to players; lobby spawn not set. Use /quake setlobby to set one first.");
					}
				}
				// Set task for signs
				List<String> signs = getConfig().getStringList("general.lobby.signs");
				for (String sign : signs) {
					Location loc = Utils.stringToLocation(sign, true);
					signLocs.add(loc);
				}
				Utils.setupSignTimer();
			}

		}, 10);

		loadLang();

		// Load listeners
		new Listeners(this);
		// Load utils
		new Utils(this);
		// Set task for scoreboard
		Utils.setupScoreboardTimer();
		// Register command
		getCommand("quake").setExecutor(new QuakeCommand(this));
	}

	public void loadLang() {
		File lang = new File(getDataFolder(), "lang.yml");
		if (!lang.exists()) {
			try {
				getDataFolder().mkdir();
				lang.createNewFile();
				InputStream defConfigStream = this.getResource("lang.yml");
				if (defConfigStream != null) {
					YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
					defConfig.save(lang);
					Lang.setFile(defConfig);
				}
			} catch (IOException e) {
				e.printStackTrace();
				log.severe("Failed to save lang.yml.");
				this.setEnabled(false);
			}
		}
		YamlConfiguration conf = YamlConfiguration.loadConfiguration(lang);
		for (Lang item : Lang.values()) {
			if (conf.getString(item.getPath()) == null) {
				conf.set(item.getPath(), item.getDefault());
			}
		}
		Lang.setFile(conf);
		Main.LANG = conf;
		Main.LANG_FILE = lang;
		try {
			conf.save(getLangFile());
		} catch (IOException e) {
			log.log(Level.WARNING, "Failed to save lang.yml.");
			e.printStackTrace();
		}
	}

	public File getLangFile() {
		return LANG_FILE;
	}

	public void loadConfig() {

		if (!new File(getDataFolder(), "config.yml").exists()) {
			saveDefaultConfig();
		}


		loadPlayers();
		loadRailguns();
		// loadArenas() needs a delay anyway
		Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {

			@Override
			public void run() {

				loadArenas();

			}

		}, 10);

		vaultEnabled = getConfig().getBoolean("general.vault.enabled") && setupEconomy();
	}

	public void loadArenas() {
		// Load enabled arenas
		if (getConfig().getStringList("arenas.enabled-arenas") != null) {
			List<String> enabledArenas = getConfig().getStringList("arenas.enabled-arenas");
			for (String name : enabledArenas) {
				// Load arena from disk
				final QuakeArena arena = new QuakeArena(this, name);
				List<Location> spawns = new ArrayList<>();
				List<String> stringSpawns = getConfig().getStringList("arenas." + name + ".spawns");
				for (String stringSpawn : stringSpawns) {
					getLogger().info(stringSpawn);
					spawns.add(Utils.stringToLocation(stringSpawn, false));
				}
				arena.min = getConfig().getInt("arenas." + name + ".min");
				arena.max = getConfig().getInt("arenas." + name + ".max");
				arena.name = name;
				arena.displayName = getConfig().getString("arenas." + name + ".display-name");
				arena.players = new ArrayList<>();
				arena.spawns = spawns;
				arena.status = "waiting";
				arena.save();
				// Put into HashMap
				arenas.put(name, arena);
				getLogger().info("Arena " + name + " enabled!");
			}
		}
	}

	public void loadPlayers() {
		for (Player p : Bukkit.getOnlinePlayers()) {
			// Check if player in players HashMap
			if (!players.containsKey(p.getName())) {
				players.put(p.getName(), new QuakePlayer(this, p));
				// If player is not initialized
				if (getConfig().get("players." + p.getName() + ".points") == null) {
					Utils.setPoints(p.getName(), 0);
				}
				if (Utils.getHoe(p.getName()) == null) {
					Utils.setHoe(p.getName(), "wood");
				}
				if (getConfig().get("players." + p.getName() + ".coins") == null) {
					Utils.setCoins(p.getName(), 500);
				}
				if (getConfig().get("players." + p.getName() + ".kills") == null) {
					Utils.setKills(p.getName(), 0);
				}
				if (getConfig().get("players." + p.getName() + ".vip") != null) {
					if (getConfig().getBoolean("players." + p.getName() + ".vip")) {
						vips.add(p.getName());
					}
				}
			}
		}
	}

	public void loadRailguns() {
		woodShoot = new Ability(1, getConfig().getInt("railguns.wood.reload"), TimeUnit.MILLISECONDS);
	}

	// public Permission permission = null;
	public Economy economy = null;

    /*
    private boolean setupPermissions()
    {
        RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null) {
            permission = permissionProvider.getProvider();
        }
        return (permission != null);
    }
    */

	private boolean setupEconomy() {
		RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
		if (economyProvider != null) {
			economy = economyProvider.getProvider();
		}

		return (economy != null);
	}

}
