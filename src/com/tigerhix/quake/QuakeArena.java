package com.tigerhix.quake;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;

public class QuakeArena {
	
	public Main main;
	
    int min;
    int max;
    String name;
    String displayName;
    
    List <String> players;
    List <Location> spawns;
    String status; // 0. waiting; 1. started; 2. finished
    int seconds = 0;
    
    int waitingID = 0;
    int scoreboardID = 0;
    
    public void save() {
    	// arenas
    	main.getConfig().set("arenas." + name + ".min", min);
    	main.getConfig().set("arenas." + name + ".max", max);
    	main.getConfig().set("arenas." + name + ".display-name", displayName);
    	// spawns
    	List <String> stringSpawns = new ArrayList <String>();
    	for (int index = 0; index < spawns.size(); index ++ ) {
    		stringSpawns.add(Utils.locationToString(spawns.get(index), false));
    	}
    	main.getConfig().set("arenas." + name + ".spawns", stringSpawns);
    	// enabled-arenas
    	List <String> enabledArenas = main.getConfig().getStringList("arenas.enabled-arenas");
    	if (!enabledArenas.contains(name)) {
    		enabledArenas.add(name);
    		main.getConfig().set("arenas.enabled-arenas", enabledArenas);
    	}
    	main.saveConfig();
    }
    
    public QuakeArena(Main main, String name) {
    	this.main = main;
    	this.min = 2;
    	this.max = 8;
    	this.name = name;
    	this.displayName = name;
    	this.players = new ArrayList <String>();
    	this.spawns = new ArrayList <Location>();
    	this.status = "waiting";
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public String getName() {
        return name;
    }

    public void setMin(int newValue) {
        min = newValue;
    }

    public void setMax(int newValue) {
        max = newValue;
    }

    public void setName(String newValue) {
        name = newValue;
    }
    
    public List <String> getPlayers() {
    	return players;
    }
    
    public void addPlayer(String p) {
    	// Arena
    	players.add(p);
    	// Player
    	Utils.getQuakePlayer(p).arena = name;
    }
    
    public void removePlayer(String p) {
    	players.remove(p);
    }
    
}