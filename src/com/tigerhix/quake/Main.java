package com.tigerhix.quake;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
	
	/*
	 * CHANGELOG
	 * - Add shop; buy 5 railguns from shop using coins you got from matches!
	 * - Add Vault-based economy system; if you enable it, coins will be removed and player use points from Vault to buy things.
	 * - Add new commands:
	 * # /quake - Teleport you to main lobby
	 * # /quake setmin [arena] [min] - Set minimum player requirement for arena
	 * # /quake setmax [arena] [max] - Set maximum player requirement for arena
	 * # /quake start [arena] - Force start an arena
	 * # /quake stop [arena] - Force stop an arena
	 * - New sounds for shooting and player death; shooting sound has random pitch too, which imitate Hypixel's one.
	 * - Now keep inventory on death in matches
	 * - Now player can't break/place blocks in matches
	 * - Now player can't use commands in arenas except /quake leave
	 * - Fix bugs:
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
	 * - Buy & use railguns
	 * - Hats and kits
	 * - Permissions
	 * - Regioned arena stats
	 * - Stop players from respawning all in the same area
	 * - No damage from lava
	 * - Must buy first
	 * - Add sound to countdown
	 * - Inventory toggle bug
	 */

    public static Logger log;
    public static PluginManager pm;
    public static YamlConfiguration LANG;
    public static File LANG_FILE;

    public HashMap < String, QuakePlayer > players = new HashMap < String, QuakePlayer > ();
    public HashMap < String, QuakeArena > arenas = new HashMap < String, QuakeArena > ();
    
    public HashMap <String, String> inventories = new HashMap<String, String>();
    
    public Boolean statsEnabled;
    public Boolean vaultEnabled = false;
    
    public Location lobbyLoc;

    public List < Location > signLocs = new ArrayList < Location > ();
    
    public Ability woodShoot = new Ability(1, 1500, TimeUnit.MILLISECONDS);
    public Ability stoneShoot = new Ability(1, 1400, TimeUnit.MILLISECONDS);
    public Ability ironShoot = new Ability(1, 1300, TimeUnit.MILLISECONDS);
    public Ability goldShoot = new Ability(1, 1200, TimeUnit.MILLISECONDS);
    public Ability diamondShoot = new Ability(1, 1100, TimeUnit.MILLISECONDS);
    public Ability exp = new Ability(1, 50, TimeUnit.MILLISECONDS);

    @
    Override
    public void onEnable() {
        
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {

			@Override
			public void run() {
				loadConfig();
				// Define variables
				if (getConfig().getString("general.lobby.spawn") != null) {
		    		getLogger().info("Lobby spawn set!");
		    		String stringLoc = getConfig().getString("general.lobby.spawn");
		    		lobbyLoc = Utils.stringToLocation(stringLoc, false);
		    	} else {
		    		getLogger().info("Lobby spawn not found in config.yml. Automatically set to the main spawn.");
		    		lobbyLoc = getServer().getWorlds().get(0).getSpawnLocation();
		    	}
				// Set task for signs
		        List < String > signs = getConfig().getStringList("general.lobby.signs");
		        for (int index = 0; index < signs.size(); index++) {
		            Location loc = Utils.stringToLocation(signs.get(index), true);
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
        for (Lang item: Lang.values()) {
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

    public YamlConfiguration getLang() {
        return LANG;
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
        
        if (getConfig().getBoolean("general.vault.enabled") == true) { // Vault enabled
        	if (!setupEconomy()) {
        		vaultEnabled = false;
        	} else {
        		vaultEnabled = true;
        	}
        } else {
        	vaultEnabled = false;
        }
    }

    public void loadArenas() {
    	// Load enabled arenas
        if (getConfig().getStringList("arenas.enabled-arenas") != null) {
            List < String > enabledArenas = getConfig().getStringList("arenas.enabled-arenas");
            for (int index = 0; index < enabledArenas.size(); index++) {
                // Load arena from disk
                String name = enabledArenas.get(index);
                final QuakeArena arena = new QuakeArena(this, name);
                List < Location > spawns = new ArrayList < Location > ();
                List < String > stringSpawns = getConfig().getStringList("arenas." + name + ".spawns");
                for (int index2 = 0; index2 < stringSpawns.size(); index2++) {
                	getLogger().info(stringSpawns.get(index2));
                    spawns.add(Utils.stringToLocation(stringSpawns.get(index2), false));
                }
                arena.min = getConfig().getInt("arenas." + name + ".min");
                arena.max = getConfig().getInt("arenas." + name + ".max");
                arena.name = name;
                arena.displayName = getConfig().getString("arenas." + name + ".display-name");
                arena.players = new ArrayList < String > ();
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
    	for (Player p: Bukkit.getOnlinePlayers()) {
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
                	Utils.setCoins(p.getName(), 100);
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
    
    private boolean setupEconomy()
    {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }

        return (economy != null);
    }
    
}