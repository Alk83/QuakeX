package com.tigerhix.quake;

import org.bukkit.entity.Player;

public class QuakePlayer {
	
	public Main main;
	
	// Total stats
	
    String name;
    
    // Playing stats
    
    String arena;
    int score;
    Boolean died;
    
    public QuakePlayer(Main main, Player p) {
    	this.main = main;
    	// Setup stats
    	name = p.getName();
		arena = "";
		score = 0;
		died = false;
    }
    
}