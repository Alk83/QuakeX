package com.tigerhix.quake;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

public class QuakeCommand implements CommandExecutor {

	public Logger log;

	public Main main;

	public QuakeCommand(Main main) {
		this.main = main;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		Player p = (Player) sender;
		Boolean op = false;
		Boolean playing = false;
		if (p.isOp()) op = true;
		if (Utils.getQuakePlayer(p.getName()).arena != "") playing = true;
		if (args.length == 0) {
			p.sendMessage("This server is running Quake Deathmatch made by TigerHix and Relicum,\nversion " + main.getDescription().getVersion() + " (MC: 1.6.2)\n(Implementing API version 1.6.2-R0.1-SNAPSHOT)");
			return true;
		}
		if (args.length == 1) {
			String action = args[0];

			if (action.equalsIgnoreCase("buy") && !playing) {
				if (main.getConfig().getBoolean("general.shop.enabled")) {
					if (Utils.isQuakePlayer(p.getName())) {
						Utils.openMenu(p);
					}
				}
				return true;
			}
			if (action.equalsIgnoreCase("help")) {
				p.sendMessage(ChatColor.AQUA + "* Available commands:");
				p.sendMessage(ChatColor.GRAY + "    # /quake join [name] - Join the specified arena.");
				p.sendMessage(ChatColor.GRAY + "    # /quake leave - Leave the current arena.");
				p.sendMessage(ChatColor.GRAY + "    # /quake buy - Open the buy menu.");
				p.sendMessage(ChatColor.GRAY + "    # /quake stats - View your stats.");
				p.sendMessage(ChatColor.GRAY + "    # /quake help - Show this help page.");
				return true;
			}
			if (action.equalsIgnoreCase("stats")) {
				p.sendMessage(Lang.STATS.toString());
				p.sendMessage("  " + Lang.POINTS.toString() + ": " + ChatColor.GRAY + Utils.getPoints(p.getName()));
				p.sendMessage("  " + Lang.COINS.toString() + ": " + ChatColor.GRAY + Utils.getCoins(p.getName()));
				p.sendMessage("  " + Lang.KILLS.toString() + ": " + ChatColor.GRAY + Utils.getKills(p.getName()));
				return true;
			}
			if (action.equalsIgnoreCase("addspawn") && op && !playing) {
				if (Utils.getSelectedArena(p) == null) {
					p.sendMessage(Lang.ARENA_NOT_SELECTED.toString());
					return true;
				}
				QuakeArena arena = Utils.getQuakeArena(Utils.getSelectedArena(p));
				Location loc = p.getLocation();
				arena.spawns.add(loc);
				arena.save();
				p.sendMessage(Lang.SPAWN_SET.toString());
				return true;
			}
			if (action.equalsIgnoreCase("setlobby") && op && !playing) {
				main.lobbyLoc = p.getLocation();
				main.getConfig().set("general.lobby.spawn", Utils.locationToString(main.lobbyLoc, false));
				main.saveConfig();
				p.sendMessage(Lang.LOBBY_SET.toString());
				return true;
			}
			if (action.equalsIgnoreCase("lobby")) {
				if (Utils.isQuakePlayer(p.getName())) {
					if (Utils.getQuakePlayer(p.getName()).arena != "") {
						if (Utils.getQuakeArena(Utils.getQuakePlayer(p.getName()).arena).status != "finished") {
							Utils.leaveGame(p);
							p.sendMessage(Lang.ARENA_LEAVED.toString());
						} else {
							p.sendMessage(Lang.CANT_LEAVE_ARENA.toString());
							return true;
						}
					} else {
						Utils.lobbyTeleport(p);
						return true;
					}
				}
				return true;
			}

			if (action.equalsIgnoreCase("leave")) {
				if (Utils.isQuakePlayer(p.getName())) {
					if (Utils.getQuakePlayer(p.getName()).arena != "") {
						if (Utils.getQuakeArena(Utils.getQuakePlayer(p.getName()).arena).status != "finished") {
							Utils.leaveGame(p);
							p.sendMessage(Lang.ARENA_LEAVED.toString());
						} else {
							p.sendMessage(Lang.CANT_LEAVE_ARENA.toString());
							return true;
						}
					} else {
						p.sendMessage(Lang.ARENA_NOT_JOINED.toString());
						return true;
					}
				}
				return true;
			}
		}
		if (args.length == 2) {
			String action = args[0];
			String name = args[1];

			if (action.equalsIgnoreCase("vip") && op) {
				if (Bukkit.getPlayer(name) != null) {
					if (Utils.isVIP(Bukkit.getPlayer(name))) {
						p.sendMessage(Lang.VIP_ALREADY.toString());
						return true;
					}
					main.getConfig().set("players." + name + ".vip", true);
					main.saveConfig();
					main.vips.add(name);
					p.sendMessage(Lang.VIP_ADDED.toString());
					return true;
				}
			}

			if (action.equalsIgnoreCase("create") && op && !playing) {
				if (Utils.getQuakeArena(name) != null) {
					// If arena already exists
					p.sendMessage(Lang.ARENA_ALREADY_EXISTS.toString());
					return true;
				} else {
					// If not, create one
					main.arenas.put(name, new QuakeArena(main, name));
					Utils.getQuakeArena(name).save();
					p.sendMessage(Lang.ARENA_CREATED.toString());
					Utils.setSelectedArena(p.getName(), name);
				}
				return true;
			}
			if (action.equalsIgnoreCase("select") && op && !playing) {
				if (Utils.getQuakeArena(name) != null) {
					Utils.setSelectedArena(p.getName(), name);
					p.sendMessage(Lang.ARENA_SELECTED.toString());
				} else {
					p.sendMessage(Lang.ARENA_NOT_FOUND.toString());
					return true;
				}
				return true;
			}

			if (action.equalsIgnoreCase("start") && op) {
				Utils.startGame(name);
				return true;
			}

			if (action.equalsIgnoreCase("stop") && op) {
				Utils.stopGame(name, p.getName());
				return true;
			}

			if (action.equalsIgnoreCase("join")) {
				if (Utils.isQuakePlayer(p.getName())) {
					QuakeArena arena = Utils.getQuakeArena(name);
					QuakePlayer player = Utils.getQuakePlayer(p.getName());
					if (arena != null) { // Has arena
						if (player.arena == "") { // Player not playing
							if (arena.status == "waiting") { // Arena not playing
								if (Utils.getQuakeArena(name).players.size() < Utils.getQuakeArena(name).max) { // Not full
									Utils.joinGame(p, name);
								} else {
									p.sendMessage(Lang.ARENA_FULL.toString());
								}
							} else {
								p.sendMessage(Lang.ARENA_ALREADY_STARTED.toString());
							}
						} else {
							p.sendMessage(Lang.ALREADY_JOINED.toString());
						}
					} else {
						p.sendMessage(Lang.ARENA_NOT_FOUND.toString());
					}
				}
				return true;
			}
		}
		if (args.length == 3) {
			String action = args[0];
			String player = args[1];
			String amount = args[2];
			if (action.equalsIgnoreCase("setpoints") && op) {
				if (Utils.isQuakePlayer(player)) {
					Utils.setPoints(player, Integer.parseInt(amount));
					p.sendMessage(Lang.STATS_CHANGED.toString());
				}
				return true;
			}
			if (action.equalsIgnoreCase("setcoins") && op) {
				if (Utils.isQuakePlayer(player)) {
					Utils.setCoins(player, Integer.parseInt(amount));
					p.sendMessage(Lang.STATS_CHANGED.toString());
				}
				return true;
			}
		}
		if (args.length == 3) {
			String action = args[0];
			String arena = args[1];
			String amount = args[2];
			if (action.equalsIgnoreCase("setmin") && op && !playing) {
				if (Utils.getQuakeArena(arena) != null) {
					QuakeArena a = Utils.getQuakeArena(arena);
					a.min = Integer.parseInt(amount);
					a.save();
				}
				return true;
			}
			if (action.equalsIgnoreCase("setmax") && op && !playing) {
				if (Utils.getQuakeArena(arena) != null) {
					QuakeArena a = Utils.getQuakeArena(arena);
					a.max = Integer.parseInt(amount);
					a.save();
				}
				return true;
			}
		}
		if (playing) {
			p.sendMessage(Lang.DISABLED_COMMAND.toString());
		} else {
			p.sendMessage(Lang.WRONG_COMMAND.toString());
		}
		return true;
	}

}
