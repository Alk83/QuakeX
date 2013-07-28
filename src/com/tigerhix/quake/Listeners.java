package com.tigerhix.quake;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;


import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
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
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;

import com.tigerhix.quake.Main;

public class Listeners implements Listener {

    public Main main;

    public Listeners(Main main) {
        this.main = main;
        main.getServer().getPluginManager().registerEvents(this, main);
    }

    // Lobby

    @
    EventHandler
    public void onSignBreak(BlockBreakEvent evt) {
        Block b = evt.getBlock();
        Player p = evt.getPlayer();
        // Check sign
        if (b.getType() == Material.WALL_SIGN || b.getType() == Material.SIGN_POST) { // Is sign
            List < String > signs = main.getConfig().getStringList("general.lobby.signs");
            Location loc = b.getLocation();
            String stringLoc = loc.getX() + "," + loc.getY() + "," + loc.getZ();
            if (signs.contains(stringLoc)) { // Is lobby sign
                if (b.getWorld() == main.lobbyLoc.getWorld()) { // In lobby world
                    // Save to config
                    signs.remove(stringLoc);
                    main.getConfig().set("general.lobby.signs", signs);
                    main.saveConfig();
                    p.sendMessage(Lang.JOIN_SIGN_REMOVED.toString());
                    // Update signLocs - signLocs is loaded in onEnable(), it won't update automatically until reload.
                    main.signLocs.remove(loc);
                }
            }
        }
    }

    @
    EventHandler(priority = EventPriority.MONITOR)
    public void onSignChange(SignChangeEvent evt) {
        Block b = evt.getBlock();
        BlockState state = b.getState();
        Sign s = (Sign) state;
        Player p = evt.getPlayer();
        // Check sign
        if (evt.getLine(0) != null || (!(evt.getLine(0).equals("")))) { // Not null
            if (evt.getLine(0).equalsIgnoreCase("[Quake]")) { // Has keyword
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
                        List < String > signs = main.getConfig().getStringList("general.lobby.signs");
                        Location loc = s.getLocation();
                        String stringLoc = Utils.locationToString(loc, true);
                        signs.add(stringLoc);
                        main.getConfig().set("general.lobby.signs", signs);
                        main.saveConfig();
                        // Update signLocs - signLocs is loaded in onEnable(), it won't update automatically until reload.
                        main.signLocs.add(loc);
                    } else {
                        p.sendMessage(Lang.NOT_IN_LOBBY_WORLD.toString());
                    }
                } else {
                    p.sendMessage(Lang.ARENA_NOT_FOUND_CANT_CREATE.toString());
                }
            }
        }
    }

    @
    EventHandler
    public void onRightClick(PlayerInteractEvent evt) {
        if (evt.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block b = evt.getClickedBlock();
            Player p = evt.getPlayer();
            if ((b.getType() == Material.SIGN_POST) || (b.getType() == Material.WALL_SIGN)) {
                Sign s = (Sign) b.getState();
                // Check sign
                if (b.getWorld() == main.lobbyLoc.getWorld()) { // In lobby world
                    String name = ChatColor.stripColor(s.getLine(1));
                    QuakeArena arena = Utils.getQuakeArena(name);
                    QuakePlayer player = Utils.getQuakePlayer(p.getName());
                    if (arena != null) { // Has arena
                        if (player.arena == "") { // Player not playing
                            if (arena.status == "waiting") { // Arena not playing
                                if (Utils.getQuakeArena(name).players.size() < Utils.getQuakeArena(name).max) { // Not full
                                    Utils.joinGame(p, name);
                                    // TODO: Inventory clear
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
            if (main.getConfig().get("players." + p.getName() + ".coins") == null) {
                Utils.setCoins(p.getName(), 100);
            }
        }
    }

    @
    EventHandler
    public void onQuit(PlayerQuitEvent evt) {

        Player p = evt.getPlayer();
        QuakePlayer player = Utils.getQuakePlayer(p.getName());

        if (player.arena != "") { // Playing
            // TODO: Returnable
            Utils.leaveGame(p);
        }

    }
    
    // Set to monitor; because Essentials use HIGHEST
    
    @
    EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace(final BlockPlaceEvent evt) {
    	QuakePlayer player = Utils.getQuakePlayer(evt.getPlayer().getName());
        // If player is playing
        if (player.arena != "") {
        	evt.setCancelled(true);
        }
    }
    
    @
    EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(final BlockBreakEvent evt) {
    	QuakePlayer player = Utils.getQuakePlayer(evt.getPlayer().getName());
        // If player is playing
        if (player.arena != "") {
        	evt.setCancelled(true);
        }
    }

    @
    EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(final PlayerRespawnEvent evt) {
        QuakePlayer player = Utils.getQuakePlayer(evt.getPlayer().getName());
        // If player is playing
        if (player.arena != "") {
            player.died = false;
            // Teleport
            main.getLogger().info(Utils.locationToString(Utils.getRandomTeleport(evt.getPlayer()), false));
            evt.setRespawnLocation(Utils.getRandomTeleport(evt.getPlayer()));
            // Potion effects
            new BukkitRunnable() {@
                Override
                public void run() {
                    Utils.setPotionEffects(evt.getPlayer());
                }
            }.runTaskLater(main, 3L);
            // Give back inventory
            Inventory i = Utils.StringToInventory(main.inventories.get(evt.getPlayer().getName()));
            evt.getPlayer().getInventory().setContents(i.getContents());
        }
    }

    @
    EventHandler(priority = EventPriority.MONITOR)
    public void onFallDamage(EntityDamageEvent evt) {
        if (evt.getEntity() instanceof Player) {
            Player p = (Player) evt.getEntity();
            QuakePlayer player = Utils.getQuakePlayer(p.getName());
            // If player is playing
            if (player.arena != "") {
                // Cancel fall damage!
                if (evt.getEntity() instanceof Player && evt.getCause() == DamageCause.FALL) {
                    evt.setCancelled(true);
                }
            }
        }
    }

    @
    EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent evt) {
        QuakePlayer player = Utils.getQuakePlayer(evt.getEntity().getName());
        // If player is playing
        if (player.arena != "") {
            // Cancel death messages! Use Utils.broadcastMessage instead.
            evt.setDeathMessage(null);
            // Save inventory
            main.inventories.put(player.name, Utils.InventoryToString(evt.getEntity().getInventory()));
        }
    }
    
    @
    EventHandler
    public void onCommand(PlayerCommandPreprocessEvent evt) {
    	QuakePlayer player = Utils.getQuakePlayer(evt.getPlayer().getName());
        // If player is playing
        if (player.arena != "") {
        	if (!evt.getMessage().equalsIgnoreCase("/quake leave")) { // You can only leave
        		// Cancel command
        		evt.setCancelled(true);
        		// Send message
        		evt.getPlayer().sendMessage(Lang.NO_COMMANDS.toString());
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
            	
                if (evt.getAction().equals(Action.RIGHT_CLICK_AIR) || evt.getAction().equals(Action.RIGHT_CLICK_BLOCK)) { // Right-clicking
                    
                	if (p.getItemInHand().getType() == Material.WOOD_HOE) { // Using wooden hoe
                        if (main.woodShoot.tryUse(p)) { // Can use
                            shooted = true;
                            hoe = "wood";
                        }
                    }
                	
                	if (p.getItemInHand().getType() == Material.STONE_HOE) { // Using wooden hoe
                        if (main.stoneShoot.tryUse(p)) { // Can use
                            shooted = true;
                            hoe = "stone";
                        }
                    }
                	
                	if (p.getItemInHand().getType() == Material.IRON_HOE) { // Using wooden hoe
                        if (main.ironShoot.tryUse(p)) { // Can use
                            shooted = true;
                            hoe = "iron";
                        }
                    }
                	
                	if (p.getItemInHand().getType() == Material.GOLD_HOE) { // Using wooden hoe
                        if (main.goldShoot.tryUse(p)) { // Can use
                            shooted = true;
                            hoe = "gold";
                        }
                    }
                	
                	if (p.getItemInHand().getType() == Material.DIAMOND_HOE) { // Using wooden hoe
                        if (main.diamondShoot.tryUse(p)) { // Can use
                            shooted = true;
                            hoe = "diamond";
                        }
                    }
                	
                	// Detect target
                	
                	if (shooted) {
                		int range = main.getConfig().getInt("railguns" + hoe + "range");

                        Location pPos = p.getEyeLocation();
                        Vector3D pDir = new Vector3D(pPos.getDirection());

                        Vector3D pStart = new Vector3D(pPos);
                        Vector3D pEnd = pStart.add(pDir.multiply(range));

                        // EXP Animation

                        p.setExp(1);

                        // Play particles - like Quakecraft does
                        for (Block loc: evt.getPlayer().getLineOfSight(null, 100)) {
                            try {
                                playParticles(loc.getLocation());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        // Get nearby entities
                        for (Player target: p.getWorld().getPlayers()) {
                            // Bounding box of the given player
                            Vector3D targetPos = new Vector3D(target.getLocation());
                            Vector3D minimum = targetPos.add(-1.5, -1, -1.5);
                            Vector3D maximum = targetPos.add(1.5, 2.67, 1.5);

                            if (target != p && hasIntersection(pStart, pEnd, minimum, maximum)) {
                                if (hit == null ||
                                    hit.getLocation().distanceSquared(pPos) >
                                    target.getLocation().distanceSquared(pPos)) {

                                    hit = target;
                                }
                            }
                        }

                        if (hit != null) { // Target found
                            if (!Utils.getQuakePlayer(hit.getName()).died) { // Target alive
                                // Kill player
                                Utils.killPlayer(p.getName(), hit.getName());
                            }
                        }
                	}
                    
                    
                    // Sound, explosion, firework..

                    if (shooted) { // If shooted
                        // Play shoot sound
                    	Utils.playSound(p, "fireworks.blast", p.getLocation(), (float) Utils.randomInt(1, 100)/100, 2F);
                    	if (hit != null) { // Target found
                    		// Play death sound
                        	Utils.playSound(p, "fireworks.twinkle", p.getLocation(), 1F, 2F);
                    	}
                        // Play explosion
                        if (main.getConfig().getBoolean("railguns." + hoe + ".explosion.enabled")) { // Explosion enabled
                            if (main.getConfig().getBoolean("railguns." + hoe + ".explosion.only-when-hit")) { // Explosion only on targets
                                if (hit != null) { // Target found
                                    w.createExplosion(p.getTargetBlock(null, 100).getLocation().getX(), p.getTargetBlock(null, 100).getLocation().getY(), p.getTargetBlock(null, 100).getLocation().getZ(), (float) main.getConfig().getInt("railguns." + hoe + ".explosion.power"), false, false); // Play explosion
                                }
                            } else {
                                w.createExplosion(p.getTargetBlock(null, 100).getLocation().getX(), p.getTargetBlock(null, 100).getLocation().getY(), p.getTargetBlock(null, 100).getLocation().getZ(), (float) main.getConfig().getInt("railguns." + hoe + ".explosion.power"), false, false); // Play explosion
                            }
                        }
                        // Play firework
                        if (main.getConfig().getBoolean("railguns." + hoe + ".firework.enabled")) { // Firework enabled
                            if (hit != null) {
                                FireworkEffectPlayer fp = new FireworkEffectPlayer();
                                try {
                                    Color color = Utils.getColor(main.getConfig().getString("railguns." + hoe + ".firework.color").toUpperCase());
                                    Type type = Utils.getType(main.getConfig().getString("railguns." + hoe + ".firework.type").toUpperCase());
                                    fp.playFirework(hit.getWorld(), hit.getLocation(), FireworkEffect.builder().withColor(color).with(type).build());
                                } catch (IllegalArgumentException e) {
                                    e.printStackTrace();
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

    private boolean hasIntersection(Vector3D p1, Vector3D p2, Vector3D min, Vector3D max) {
        final double epsilon = 0.0001f;

        Vector3D d = p2.subtract(p1).multiply(0.5);
        Vector3D e = max.subtract(min).multiply(0.5);
        Vector3D c = p1.add(d).subtract(min.add(max).multiply(0.5));
        Vector3D ad = d.abs();

        if (Math.abs(c.x) > e.x + ad.x)
            return false;
        if (Math.abs(c.y) > e.y + ad.y)
            return false;
        if (Math.abs(c.z) > e.z + ad.z)
            return false;

        if (Math.abs(d.y * c.z - d.z * c.y) > e.y * ad.z + e.z * ad.y + epsilon)
            return false;
        if (Math.abs(d.z * c.x - d.x * c.z) > e.z * ad.x + e.x * ad.z + epsilon)
            return false;
        if (Math.abs(d.x * c.y - d.y * c.x) > e.x * ad.y + e.y * ad.x + epsilon)
            return false;

        return true;
    }

    Random gen = new Random();

    private Object[] dataStore = new Object[5];

    public void playParticles(Location loc) throws Exception {
        Firework fw = (Firework) loc.getWorld().spawn(loc, Firework.class);
        if (dataStore[0] == null) dataStore[0] = getMethod(loc.getWorld().getClass(), "getHandle");
        if (dataStore[2] == null) dataStore[2] = getMethod(fw.getClass(), "getHandle");
        dataStore[3] = ((Method) dataStore[0]).invoke(loc.getWorld(), (Object[]) null);
        dataStore[4] = ((Method) dataStore[2]).invoke(fw, (Object[]) null);
        if (dataStore[1] == null) dataStore[1] = getMethod(dataStore[3].getClass(), "addParticle");
        ((Method) dataStore[1]).invoke(dataStore[3], new Object[] {
            "fireworksSpark", loc.getX(), loc.getY(), loc.getZ(), gen.nextGaussian() * 0.05D, -(loc.getZ() * 1.15D) * 0.5D, gen.nextGaussian() * 0.05D
        });
        fw.remove();
    }

    private Method getMethod(Class <? > cl, String method) {
        for (Method m: cl.getMethods())
            if (m.getName().equals(method)) return m;
        return null;
    }

}