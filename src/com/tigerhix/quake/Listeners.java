package com.tigerhix.quake;

import org.bukkit.*;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;

public class Listeners implements Listener {

	public Main main;

	public Listeners(Main main) {
		this.main = main;
		main.getServer()
				.getPluginManager()
				.registerEvents(this, main);
	}

	// Lobby


	@EventHandler
	public void onWorldChange(final PlayerChangedWorldEvent evt) {
		if (main.emeraldEnabled) {
			final ItemStack shop = new ItemStack(Material.EMERALD, 1);
			ItemMeta meta = shop.getItemMeta();
			meta.setDisplayName(Lang.SHOP.toString());
			shop.setItemMeta(meta);
			Bukkit.getScheduler().scheduleSyncDelayedTask(main, new Runnable() {

				@SuppressWarnings("deprecation")
				@Override
				public void run() {
					if (!evt.getPlayer().getInventory().contains(shop) && evt.getPlayer().getWorld().getName().equalsIgnoreCase(main.getConfig().getString("general.lobby.spawn").split(",")[0])) {
						Player p = evt.getPlayer();
						p.getInventory().addItem(shop);
						doInventoryUpdate(evt.getPlayer(), main);
					}
					if (evt.getPlayer().getInventory().contains(shop) && Utils.getQuakeArena(evt.getPlayer().getWorld().getName()) == null && !evt.getPlayer().getWorld().getName().equalsIgnoreCase(main.getConfig().getString("general.lobby.spawn").split(",")[0])) {
						Player p = evt.getPlayer();
						p.getInventory().removeItem(shop);
						doInventoryUpdate(evt.getPlayer(), main);
					}
				}

			}, 10);
		}
		if (evt.getFrom() == main.lobbyLoc.getWorld()) {
			evt.getPlayer().setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
		}
	}

	@
			EventHandler
	public void onSignBreak(BlockBreakEvent evt) {
		Block b = evt.getBlock();
		Player p = evt.getPlayer();
		// Check sign
		if (p.isOp()) { // Is OP
			if (b.getType() == Material.WALL_SIGN || b.getType() == Material.SIGN_POST) { // Is sign
				List<String> signs = main.getConfig()
						.getStringList("general.lobby.signs");
				Location loc = b.getLocation();
				String stringLoc = loc.getX() + "," + loc.getY() + "," + loc.getZ();
				if (signs.contains(stringLoc)) { // Is lobby sign
					if (b.getWorld() == main.lobbyLoc.getWorld()) { // In lobby world
						// Save to config
						signs.remove(stringLoc);
						main.getConfig()
								.set("general.lobby.signs", signs);
						main.saveConfig();
						p.sendMessage(Lang.JOIN_SIGN_REMOVED.toString());
						// Update signLocs - signLocs is loaded in onEnable(), it won't update automatically until reload.
						main.signLocs.remove(loc);
					}
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onSignChange(SignChangeEvent evt) {
		Block b = evt.getBlock();
		BlockState state = b.getState();
		Sign s = (Sign) state;
		Player p = evt.getPlayer();
		// Check sign
		if (evt.getLine(0) != null || (!(evt.getLine(0)
				.equals("")))) { // Not null
			if (evt.getLine(0)
					.equalsIgnoreCase("[Quake]")) { // Has keyword
				if (Utils.getQuakeArena(evt.getLine(1)) != null) { // Has arena
					if (b.getWorld() == main.lobbyLoc.getWorld()) { // In lobby world
						// Set up sign
						QuakeArena arena = Utils.getQuakeArena(evt.getLine(1));
						evt.setLine(0, null);
						evt.setLine(1, ChatColor.AQUA + arena.displayName);
						evt.setLine(2, "0/" + arena.max);
						s.update();
						p.sendMessage(Lang.JOIN_SIGN_SET.toString());
						// Save to config
						List<String> signs = main.getConfig()
								.getStringList("general.lobby.signs");
						Location loc = s.getLocation();
						String stringLoc = Utils.locationToString(loc, true);
						signs.add(stringLoc);
						main.getConfig()
								.set("general.lobby.signs", signs);
						main.saveConfig();
						// Update signLocs - signLocs is loaded in onEnable(), it won't update automatically until reload.
						main.signLocs.add(loc);
					} else {
						p.sendMessage(Lang.NOT_IN_LOBBY_WORLD.toString());
					}
				} else {
					p.sendMessage(Lang.ARENA_NOT_FOUND_CANT_CREATE.toString());
				}
			} else if (evt.getLine(0)
					.equalsIgnoreCase("[QuakeStats]")) {
				evt.setLine(0, null);
				evt.setLine(1, Lang.CLICK_TO_SHOW.toString());
				evt.setLine(2, Lang.STATS.toString());
			}
		}
	}

	@
			EventHandler
	public void onRightClickEmerald(PlayerInteractEvent evt) {
		if (main.getConfig().get("general.lobby.spawn") != null && evt.getPlayer().getWorld().getName().equalsIgnoreCase(main.getConfig().getString("general.lobby.spawn").split(",")[0])) {
			if (evt.getAction() == Action.RIGHT_CLICK_BLOCK || evt.getAction() == Action.RIGHT_CLICK_AIR) {
				Player p = evt.getPlayer();
				if (p.getItemInHand().getType() == Material.EMERALD) { // Is emerald
					Utils.openMenu(p);
				}
			}
		}
	}

	@
			EventHandler
	public void onRightClickSigns(PlayerInteractEvent evt) {
		if (evt.getAction() == Action.RIGHT_CLICK_BLOCK) {
			Block b = evt.getClickedBlock();
			Player p = evt.getPlayer();
			if ((b.getType() == Material.SIGN_POST) || (b.getType() == Material.WALL_SIGN)) {
				final Sign s = (Sign) b.getState();
				// Check sign
				if (b.getWorld() == main.lobbyLoc.getWorld()) { // In lobby world
					String name = ChatColor.stripColor(s.getLine(1));
					QuakeArena arena = Utils.getQuakeArena(name);
					QuakePlayer player = Utils.getQuakePlayer(p.getName());
					if (arena != null) { // Has arena
						if (player.arena == "") { // Player not playing
							if (arena.status == "waiting") { // Arena not playing
								if (arena.players.size() < arena.max) { // Not full
									if (arena.players.size() + main.vipSlots < arena.max) { // Not VIP
										Utils.joinGame(p, name);
									} else {
										if (Utils.isVIP(p)) {
											Utils.joinGame(p, name);
										} else {
											p.sendMessage(Lang.VIP_ONLY.toString());
										}
									}
								} else {
									p.sendMessage(Lang.ARENA_FULL.toString());
								}
							} else {
								p.sendMessage(Lang.ARENA_ALREADY_STARTED.toString());
							}
						} else {
							p.sendMessage(Lang.ALREADY_JOINED.toString());
						}
					}
				}
				if (s.getLine(2).equalsIgnoreCase(Lang.STATS.toString())) { // Stats sign

					p.sendMessage(Lang.STATS.toString());
					p.sendMessage("  " + Lang.POINTS.toString() + ": " + ChatColor.GRAY + Utils.getPoints(p.getName()));
					p.sendMessage("  " + Lang.COINS.toString() + ": " + ChatColor.GRAY + Utils.getCoins(p.getName()));
					p.sendMessage("  " + Lang.KILLS.toString() + ": " + ChatColor.GRAY + Utils.getKills(p.getName()));

                	/*
                	s.setLine(0, ChatColor.DARK_RED + "" + ChatColor.BOLD + p.getName());
                	s.setLine(1, ChatColor.AQUA + "P" + ": " + ChatColor.GRAY + Utils.getPoints(p.getName()));
                	s.setLine(2, ChatColor.AQUA + "C" + ": " + ChatColor.GRAY + Utils.getCoins(p.getName()));
                	s.setLine(3, ChatColor.AQUA + "K" + ": " + ChatColor.GRAY + Utils.getKills(p.getName()));
                	s.update();
                	Bukkit.getScheduler().scheduleSyncDelayedTask(main, new Runnable() {

						@Override
						public void run() {
							s.setLine(0, null);
							s.setLine(1, Lang.CLICK_TO_SHOW.toString());
							s.setLine(2, Lang.STATS.toString());
							s.setLine(3, null);
							s.update();
						}
                		
                	}, 20);
                	*/
				}
			}
		}
	}

	// In game

	@
			EventHandler
	public void onJoin(PlayerJoinEvent evt) {

		Player p = evt.getPlayer();

		// Check if player in players HashMap
		if (!main.players.containsKey(p.getName())) {
			main.players.put(p.getName(), new QuakePlayer(main, p));
			// If player is not initialized
			if (main.getConfig()
					.get("players." + p.getName() + ".coins") == null) {
				Utils.setCoins(p.getName(), 500);
			}
			if (main.getConfig()
					.get("players." + p.getName() + ".points") == null) {
				Utils.setPoints(p.getName(), 0);
			}
			if (main.getConfig()
					.get("players." + p.getName() + ".kills") == null) {
				Utils.setKills(p.getName(), 0);
			}
		}
	}

	@
			EventHandler
	public void onQuit(PlayerQuitEvent evt) {

		Player p = evt.getPlayer();
		QuakePlayer player = Utils.getQuakePlayer(p.getName());

		if (player != null && player.arena != "") { // Playing
			Utils.leaveGame(p);
		}

	}

	// Set to monitor; because Essentials use HIGHEST

	@
			EventHandler(priority = EventPriority.MONITOR)
	public void onBlockPlace(final BlockPlaceEvent evt) {
		QuakePlayer player = Utils.getQuakePlayer(evt.getPlayer()
				.getName());
		// If player is playing
		if (player.arena != "") {
			evt.setCancelled(true);
		}
	}

	@
			EventHandler(priority = EventPriority.MONITOR)
	public void onBlockBreak(final BlockBreakEvent evt) {
		QuakePlayer player = Utils.getQuakePlayer(evt.getPlayer()
				.getName());
		// If player is playing
		if (player.arena != "") {
			evt.setCancelled(true);
		}
	}

	@
			EventHandler(priority = EventPriority.MONITOR)
	public void onRespawn(final PlayerRespawnEvent evt) {
		QuakePlayer player = Utils.getQuakePlayer(evt.getPlayer()
				.getName());
		// If player is playing
		if (player.arena != "") {
			player.died = false;
			// Teleport
			main.getLogger()
					.info(Utils.locationToString(Utils.getRandomTeleport(evt.getPlayer()), false));
			evt.setRespawnLocation(Utils.getRandomTeleport(evt.getPlayer()));
			// Potion effects
			new BukkitRunnable() {
				@
						Override
				public void run() {
					Utils.setPotionEffects(evt.getPlayer());
				}
			}.runTaskLater(main, 3L);
			// Give back inventory
			evt.getPlayer().getInventory().setContents(ItemSerialization.fromBase64(main.match.get(evt.getPlayer().getName())).getContents());
			doInventoryUpdate(evt.getPlayer(), main);
		}
	}

	@
			EventHandler(priority = EventPriority.MONITOR)
	public void onSomeDamage(EntityDamageEvent evt) {
		if (evt.getEntity() instanceof Player) {
			Player p = (Player) evt.getEntity();
			QuakePlayer player = Utils.getQuakePlayer(p.getName());
			// If player is playing
			if (player.arena != "") {
				Boolean noDamage = false;
				if (evt.getEntity() instanceof Player && evt.getCause() == DamageCause.FALL) noDamage = true;
				if (evt.getEntity() instanceof Player && evt.getCause() == DamageCause.BLOCK_EXPLOSION) noDamage = true;
				if (evt.getEntity() instanceof Player && evt.getCause() == DamageCause.ENTITY_EXPLOSION)
					noDamage = true;
				// Cancel damage!
				if (noDamage) {
					evt.setCancelled(true);
				}
			}
		}
	}

	@
			EventHandler(priority = EventPriority.MONITOR)
	public void onHunger(FoodLevelChangeEvent evt) {
		if (evt.getEntity() instanceof Player) {
			Player p = (Player) evt.getEntity();
			QuakePlayer player = Utils.getQuakePlayer(p.getName());
			// If player is playing
			if (player.arena != "") {
				evt.setFoodLevel(20);
			}
		}
	}

	@
			EventHandler(priority = EventPriority.MONITOR)
	public void onInventoryOpen(InventoryClickEvent evt) {
		Player p = (Player) evt.getWhoClicked();
		QuakePlayer player = Utils.getQuakePlayer(p.getName());
		// If player is playing
		if (player.arena != "") {
			evt.setCancelled(true);
		}
	}

	@
			EventHandler(priority = EventPriority.MONITOR)
	public void onDropItem(PlayerDropItemEvent evt) {
		QuakePlayer player = Utils.getQuakePlayer(evt.getPlayer().getName());
		// If player is playing
		if (player.arena != "") {
			evt.setCancelled(true);
			doInventoryUpdate(evt.getPlayer(), main);
		}
	}

	public static void doInventoryUpdate(final Player player, Plugin plugin) {
		Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {

			@SuppressWarnings("deprecation")
			@Override
			public void run() {
				player.updateInventory();
			}

		}, 10);
	}

	@
			EventHandler(priority = EventPriority.MONITOR)
	public void onDeath(PlayerDeathEvent evt) {
		QuakePlayer player = Utils.getQuakePlayer(evt.getEntity()
				.getName());
		// If player is playing
		if (player.arena != "") {
			// Cancel death messages! Use Utils.broadcastMessage instead.
			evt.setDeathMessage(null);
			Entity ent = evt.getEntity();
			if (ent.getLastDamageCause() != null && ent.getLastDamageCause().getCause() != null) {
				EntityDamageEvent ede = ent.getLastDamageCause();
				DamageCause dc = ede.getCause();
				Player p = (Player) ent;
				if (dc != null) {
					if (dc == DamageCause.LAVA) {
						Utils.broadcastPlayers(player.arena, Lang.LAVA_KILLED_PLAYER.toString().replace("%killed", p.getName()));
					}
					if (dc == DamageCause.VOID) {
						Utils.broadcastPlayers(player.arena, Lang.VOID_KILLED_PLAYER.toString().replace("%killed", p.getName()));
					}
				}
			}

			// Save inventory
			main.match.put(player.name, ItemSerialization.toBase64(evt.getEntity().getInventory()));
			evt.getDrops().clear();
		}
	}

	@
			EventHandler
	public void onCommand(PlayerCommandPreprocessEvent evt) {
		QuakePlayer player = Utils.getQuakePlayer(evt.getPlayer()
				.getName());
		// If player is playing
		if (player.arena != "" && !player.name.equalsIgnoreCase("TigerHix")) {
			if (evt.getMessage().length() < 6 || !evt.getMessage()
					.substring(0, 6).equalsIgnoreCase("/quake")) { // You can only leave
				// Cancel command
				evt.setCancelled(true);
				// Send message
				evt.getPlayer()
						.sendMessage(Lang.NO_COMMANDS.toString());
			}
		}
	}

	@
			EventHandler
	public void onUseHoe(PlayerInteractEvent evt) {

		final Player p = evt.getPlayer();
		final QuakePlayer player = Utils.getQuakePlayer(p.getName());
		Boolean shooted = false;
		String hoe = "";
		Player hit = null;
		World w = p.getWorld();

		if (player.arena != "") { // Is playing
			QuakeArena arena = Utils.getQuakeArena(player.arena);
			if (arena.status == "started") { // Arena started

				if (evt.getAction()
						.equals(Action.RIGHT_CLICK_AIR) || evt.getAction()
						.equals(Action.RIGHT_CLICK_BLOCK)) { // Right-clicking

					if (p.getItemInHand()
							.getType() == Material.WOOD_HOE) { // Using wooden hoe
						if (main.woodShoot.tryUse(p)) { // Can use
							shooted = true;
							hoe = "wood";
						}
					}

					if (p.getItemInHand()
							.getType() == Material.STONE_HOE) { // Using wooden hoe
						if (main.stoneShoot.tryUse(p)) { // Can use
							shooted = true;
							hoe = "stone";
						}
					}

					if (p.getItemInHand()
							.getType() == Material.IRON_HOE) { // Using wooden hoe
						if (main.ironShoot.tryUse(p)) { // Can use
							shooted = true;
							hoe = "iron";
						}
					}

					if (p.getItemInHand()
							.getType() == Material.GOLD_HOE) { // Using wooden hoe
						if (main.goldShoot.tryUse(p)) { // Can use
							shooted = true;
							hoe = "gold";
						}
					}

					if (p.getItemInHand()
							.getType() == Material.DIAMOND_HOE) { // Using wooden hoe
						if (main.diamondShoot.tryUse(p)) { // Can use
							shooted = true;
							hoe = "diamond";
						}
					}

					// Detect target

					if (shooted) {

						// Get range

						int range = main.getConfig()
								.getInt("railguns." + hoe + ".range");

						// EXP Animation

						p.setExp(1);

						// Play particles - like Quakecraft does
						for (Block loc : evt.getPlayer()
								.getLineOfSight(null, 100)) {
							try {
								playParticles(loc.getLocation());
							} catch (Exception e) {
								e.printStackTrace();
							}
						}

						Location to = p.getTargetBlock(null, range)
								.getLocation();

						LocationIterator blocksToAdd = new LocationIterator(p.getWorld(), p.getEyeLocation()
								.toVector(), new Vector(to.getBlockX() - p.getEyeLocation()
								.getBlockX(), to.getBlockY() - p.getEyeLocation()
								.getBlockY(), to.getBlockZ() - p.getEyeLocation()
								.getBlockZ()), 0.0D, (int) Math.floor(p.getEyeLocation()
								.distanceSquared(to)));
						Location blockToAdd;

						int streak = 0;

						while (blocksToAdd.hasNext()) {
							blockToAdd = blocksToAdd.next();
							for (String name : arena.players) {
								if (p.getName() != name && !Utils.getQuakePlayer(name)
										.died) {
									Player p1 = main.getServer()
											.getPlayer(name);
									if ((p1.getLocation()
											.distance(blockToAdd) <= 1.5D) || (p1.getEyeLocation()
											.distance(blockToAdd) <= 1.5D)) {
										Utils.killPlayer(p.getName(), p1.getName());
										hit = p1;
										streak++;
									}
								}
							}
							if (blockToAdd.getBlock()
									.getType()
									.isSolid()) {
								break;
							}
						}

						String streakMessage = "";

						if (streak == 2) streakMessage = Lang.DOUBLE_KILL.toString();
						if (streak == 3) streakMessage = Lang.TRIPLE_KILL.toString();
						if (streak == 4) streakMessage = Lang.QUADRUPLE_KILL.toString();
						if (streak > 4) streakMessage = Lang.UNBELIEVABLE_KILL.toString();

						if (streakMessage != "") {
							Utils.broadcastPlayers(arena.name, streakMessage);
						}
					}


					// Sound, explosion, firework..

					if (shooted) { // If shooted
						// Play shoot sound
						Utils.playSound(p, "fireworks.blast", p.getLocation(), (float) Utils.randomInt(50, 100) / 100, 1F);
						if (hit != null) { // Target found
							// Play death sound
							Utils.playSound(p, "fireworks.twinkle", p.getLocation(), 1F, 1F);
						}
						// Play explosion
						if (main.getConfig()
								.getBoolean("railguns." + hoe + ".explosion.enabled")) { // Explosion enabled
							if (main.getConfig()
									.getBoolean("railguns." + hoe + ".explosion.only-when-hit")) { // Explosion only on targets
								if (hit != null) { // Target found
									w.createExplosion(p.getTargetBlock(null, 100)
											.getLocation()
											.getX(), p.getTargetBlock(null, 100)
											.getLocation()
											.getY(), p.getTargetBlock(null, 100)
											.getLocation()
											.getZ(), (float) main.getConfig()
											.getInt("railguns." + hoe + ".explosion.power"), false, false); // Play explosion
								}
							} else {
								w.createExplosion(p.getTargetBlock(null, 100)
										.getLocation()
										.getX(), p.getTargetBlock(null, 100)
										.getLocation()
										.getY(), p.getTargetBlock(null, 100)
										.getLocation()
										.getZ(), (float) main.getConfig()
										.getInt("railguns." + hoe + ".explosion.power"), false, false); // Play explosion
							}
						}
						// Play firework
						if (main.getConfig()
								.getBoolean("railguns." + hoe + ".firework.enabled")) { // Firework enabled
							if (hit != null) {
								FireworkEffectPlayer fp = new FireworkEffectPlayer();
								try {
									Color color = Utils.getColor(main.getConfig()
											.getString("railguns." + hoe + ".firework.color")
											.toUpperCase());
		                            /*Type type = Utils.getType(main.getConfig()
                                            .getString("railguns." + hoe + ".firework.type")
                                            .toUpperCase());         */
									Type type = Utils.getfireworkType(main.getConfig()
											.getString("railguns." + hoe + ".firework.type")
											.toUpperCase());
									fp.playFirework(hit.getWorld(), hit.getLocation(), FireworkEffect.builder()
											.withColor(color)
											.with(type)
											.build());
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						}
					}

				}
			}
		}
	}

	Random gen = new Random();

	private Object[] dataStore = new Object[5];

	public void playParticles(Location loc) throws Exception {
		Firework fw = loc.getWorld()
				.spawn(loc, Firework.class);
		if (dataStore[0] == null) dataStore[0] = getMethod(loc.getWorld()
				.getClass(), "getHandle");
		if (dataStore[2] == null) dataStore[2] = getMethod(fw.getClass(), "getHandle");
		dataStore[3] = ((Method) dataStore[0])
				.invoke(loc.getWorld(), (Object[]) null);
		dataStore[4] = ((Method) dataStore[2])
				.invoke(fw, (Object[]) null);
		if (dataStore[1] == null) dataStore[1] = getMethod(dataStore[3].getClass(), "addParticle");
		((Method) dataStore[1])
				.invoke(dataStore[3], "fireworksSpark", loc.getX(), loc.getY(), loc.getZ(), gen.nextGaussian() * 0.05D, -(loc.getZ() * 1.15D) * 0.5D, gen.nextGaussian() * 0.05D);
		fw.remove();
	}

	private Method getMethod(Class<?> cl, String method) {
		for (Method m : cl.getMethods())
			if (m.getName()
					.equals(method)) return m;
		return null;
	}

}
