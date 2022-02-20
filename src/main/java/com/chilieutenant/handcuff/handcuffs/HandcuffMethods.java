package com.chilieutenant.handcuff.handcuffs;

import com.chilieutenant.handcuff.Config;
import com.chilieutenant.handcuff.main;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class HandcuffMethods {

    static List<Player> players = new ArrayList<>();

    public static void sendActionMessage(Player player, String message){
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }

    public static List<Entity> getEntitiesAroundPoint(final Location location, final double radius) {
        return new ArrayList<>(location.getWorld().getNearbyEntities(location, radius, radius, radius, entity -> !(entity.isDead() || (entity instanceof Player && ((Player) entity).getGameMode().equals(GameMode.SPECTATOR)))));
    }

    public static double getDistanceFromLine(final Vector line, final Location pointonline, final Location point) {
        final Vector AP = new Vector();
        double Ax, Ay, Az;
        Ax = pointonline.getX();
        Ay = pointonline.getY();
        Az = pointonline.getZ();

        double Px, Py, Pz;
        Px = point.getX();
        Py = point.getY();
        Pz = point.getZ();

        AP.setX(Px - Ax);
        AP.setY(Py - Ay);
        AP.setZ(Pz - Az);

        return (AP.crossProduct(line).length()) / (line.length());
    }

    public static boolean isObstructed(final Location location1, final Location location2) {
        final Vector loc1 = location1.toVector();
        final Vector loc2 = location2.toVector();

        final Vector direction = loc2.subtract(loc1);
        direction.normalize();

        Location loc;

        double max = 0;
        if (location1.getWorld().equals(location2.getWorld())) {
            max = location1.distance(location2);
        }

        for (double i = 0; i <= max; i++) {
            loc = location1.clone().add(direction.clone().multiply(i));
            final Material type = loc.getBlock().getType();
            if (type != Material.AIR && !(type.isTransparent() || loc.getBlock().getType() == Material.WATER)) {
                return true;
            }
        }
        return false;
    }

    public static Entity getTargetedEntity(final Player player, final double range, final List<Entity> avoid) {
        double longestr = range + 1;
        Entity target = null;
        final Location origin = player.getEyeLocation();
        final Vector direction = player.getEyeLocation().getDirection().normalize();
        for (final Entity entity : getEntitiesAroundPoint(origin, range)) {
            if (entity instanceof Player) {
                if (((Player) entity).isDead() || ((Player) entity).getGameMode().equals(GameMode.SPECTATOR)) {
                    continue;
                }
            }
            if (avoid.contains(entity)) {
                continue;
            }
            if (entity.getWorld().equals(origin.getWorld())) {
                if (entity.getLocation().distanceSquared(origin) < longestr * longestr && getDistanceFromLine(direction, origin, entity.getLocation()) < 2 && (entity instanceof LivingEntity) && entity.getEntityId() != player.getEntityId() && entity.getLocation().distanceSquared(origin.clone().add(direction)) < entity.getLocation().distanceSquared(origin.clone().add(direction.clone().multiply(-1)))) {
                    target = entity;
                    longestr = entity.getLocation().distance(origin);
                }
            }
        }
        if (target != null) {
            if (isObstructed(origin, target.getLocation())) {
                target = null;
            }
        }
        return target;
    }

    public static void startHandcuffing(Player player, Player target){
        if(players.contains(player) || players.contains(target)) return;
        players.add(player);
        players.add(target);
        String prefix = Config.getConfig().getString("messages.prefix") + " ";
        new BukkitRunnable(){
            long time = System.currentTimeMillis();
            int i = Config.getConfig().getInt("handcuff.handcufftime");
            double distance = Config.getConfig().getDouble("handcuff.distance");
            @Override
            public void run() {
                Entity entity = getTargetedEntity(player, 3, new ArrayList<Entity>());
                sendActionMessage(player, ChatColor.GRAY + "You are handcuffing " + target.getName() + " " + ((System.currentTimeMillis() - time)/1000) + "...");
                sendActionMessage(target, ChatColor.GRAY + player.getName() + " is handcuffing you");
                if(target.getLocation().distance(player.getLocation()) > distance){
                    player.sendMessage(main.replaceColorCode(prefix + Config.getConfig().getString("messages.cancelduetodistance")));
                    players.remove(player);
                    players.remove(target);
                    this.cancel();
                    return;
                }
                if(entity == null || entity != target){
                    player.sendMessage(main.replaceColorCode(prefix + Config.getConfig().getString("messages.cancelduetotime")));
                    players.remove(player);
                    players.remove(target);
                    this.cancel();
                    return;
                }
                if(System.currentTimeMillis() > time + (i * 1000)){
                    HandcuffedPlayer hp = new HandcuffedPlayer(target, player);
                    target.sendMessage(main.replaceColorCode(prefix + Config.getConfig().getString("messages.handcuffsucceeded")));
                    players.remove(player);
                    players.remove(target);
                    this.cancel();
                    return;
                }
            }
        }.runTaskTimer(main.getInstance(), 0, 1);
    }

}
