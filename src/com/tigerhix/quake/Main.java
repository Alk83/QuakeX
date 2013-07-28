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
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    public static Logger log;
    public static PluginManager pm;
    public static YamlConfiguration LANG;
    public static File LANG_FILE;

    public HashMap < String, QuakePlayer > players = new HashMap < String, QuakePlayer > ();
    public HashMap < String, QuakeArena > arenas = new HashMap < String, QuakeArena > ();
    
    public HashMap <String, String> inventories = new HashMap<String, String>();

    public IconMenu buyMenu;
    
    public Boolean statsEnabled;
    public Boolean vaultEnabled = false;
    
    public Location lobbyLoc;

    public List < Location > signLocs = new ArrayList < Location > ();
    
    public Ability woodShoot = new Ability(1, 1500, TimeUnit.MILLISECONDS);
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
		    		// TODO: try catch
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
        // Setup the buy menu
        buyMenu = new IconMenu(Lang.BUY_MENU.toString(), 27, new IconMenu.OptionClickEventHandler() {@
            Override
            public void onOptionClick(IconMenu.OptionClickEvent event) {
                event.getPlayer().sendMessage("You have chosen " + event.getName());
                event.setWillClose(true);
            }
        }, this)
            .setOption(0, new ItemStack(Material.WOOD_HOE, 1), Lang.WOOD_HOE.toString(), Lang.WOOD_HOE_DESCRIPTION.toString())
            .setOption(1, new ItemStack(Material.STONE_HOE, 1), Lang.STONE_HOE.toString(), Lang.STONE_HOE_DESCRIPTION.toString())
            .setOption(2, new ItemStack(Material.IRON_HOE, 1), Lang.IRON_HOE.toString(), Lang.IRON_HOE_DESCRIPTION.toString())
            .setOption(3, new ItemStack(Material.GOLD_HOE, 1), Lang.GOLD_HOE.toString(), Lang.GOLD_HOE_DESCRIPTION.toString())
            .setOption(4, new ItemStack(Material.DIAMOND_HOE, 1), Lang.DIAMOND_HOE.toString(), Lang.DIAMOND_HOE_DESCRIPTION.toString());
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