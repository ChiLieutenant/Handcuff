package com.chilieutenant.handcuff;

import com.chilieutenant.handcuff.handcuffs.HandcuffMethods;
import com.chilieutenant.handcuff.handcuffs.HandcuffedPlayer;
import com.chilieutenant.handcuff.jail.JailMethods;
import com.chilieutenant.handcuff.jail.Time;
import de.leonhard.storage.Json;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class main extends JavaPlugin implements Listener {

    public static main getInstance(){
        return instance;
    }
    static main instance;

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(new MainListener(), this);
        this.getCommand("jail").setTabCompleter(new JailTabCompleter());
        Config.loadDefaultConfig();
        new BukkitRunnable(){
            @Override
            public void run() {
                try {
                    JailMethods.manageTimers();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskTimer(this, 0, 20);
        new BukkitRunnable(){
            @Override
            public void run() {
                HandcuffedPlayer.manageHandcuffs();
            }
        }.runTaskTimer(this, 0, 1);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private List<Location> findLocations(Location start, Location end) {
        List<Location> locations = new ArrayList<>();

        int topBlockX = (start.getBlockX() < end.getBlockX() ? end.getBlockX() : start.getBlockX());
        int bottomBlockX = (start.getBlockX() > end.getBlockX() ? end.getBlockX() : start.getBlockX());

        int topBlockY = (start.getBlockY() < end.getBlockY() ? end.getBlockY() : start.getBlockY());
        int bottomBlockY = (start.getBlockY() > end.getBlockY() ? end.getBlockY() : start.getBlockY());

        int topBlockZ = (start.getBlockZ() < end.getBlockZ() ? end.getBlockZ() : start.getBlockZ());
        int bottomBlockZ = (start.getBlockZ() > end.getBlockZ() ? end.getBlockZ() : start.getBlockZ());

        for(int x = bottomBlockX; x <= topBlockX; x++)
        {
            for(int z = bottomBlockZ; z <= topBlockZ; z++)
            {
                for(int y = bottomBlockY; y <= topBlockY; y++)
                {
                    Block block = start.getWorld().getBlockAt(x, y, z);
                    locations.add(block.getLocation());
                }
            }
        }
        return locations;
    }

    public static String replaceColorCode(String string){
        return ChatColor.translateAlternateColorCodes('&', string);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(sender instanceof Player){
            Player player = (Player) sender;
            String prefix = replaceColorCode(Config.getConfig().getString("messages.prefix") + " ");
            if(label.equalsIgnoreCase("handcuffs")){
                if(args.length != 0){
                    player.sendMessage(ChatColor.RED + "Correct usage: /handcuffs");
                    return false;
                }
                ItemStack[] item = null;
                try {
                    item = Serialize.itemStackArrayFromBase64(Config.getConfig().getString("handcuff.item"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(item == null){
                    player.sendMessage(prefix + ChatColor.RED + "The handcuff item is not set, please contact with staffs.");
                }
                if(JailMethods.isOnCooldown(player, "handcuffs")){
                    player.sendMessage(prefix + ChatColor.RED + "This command is on cooldown.");
                    return false;
                }
                JailMethods.addCooldown(player, 300, "handcuffs");
                player.getInventory().addItem(item);
            }
            if(label.equalsIgnoreCase("unhandcuff")){
                if(!HandcuffedPlayer.isPlayerHandcuffer(player)){
                    player.sendMessage(prefix + ChatColor.RED + "You are not handcuffing someone.");
                    return false;
                }
                HandcuffedPlayer hp = HandcuffedPlayer.getHandcuffedPlayerfromHandcuffer(player);
                if(hp == null){
                    return false;
                }
                hp.unHandcuff();
                player.sendMessage(prefix + ChatColor.GRAY + "You unhandcuffed " + hp.getPlayer().getName() + ".");
                hp.getPlayer().sendMessage(prefix + ChatColor.GRAY + "You are unhandcuffed.");
            }
            if(label.equalsIgnoreCase("jail")) {
                if(args.length <= 0){
                    return false;
                }
                if (args[0].equalsIgnoreCase("struggle")) {
                    if(!HandcuffedPlayer.isHandcuffed(player)){
                        player.sendMessage(prefix + ChatColor.RED + "You are not handcuffed!");
                        return false;
                    }
                    if(JailMethods.isOnCooldown(player, "struggle")){
                        player.sendMessage(prefix + ChatColor.RED + "This command is on cooldown.");
                        return false;
                    }
                    JailMethods.addCooldown(player, 300, "struggle");
                    if(Math.random() < Config.getConfig().getDouble("handcuff.chance")){
                        HandcuffedPlayer hp = HandcuffedPlayer.getHandcuffedPlayer(player);
                        hp.unHandcuff();
                        player.sendMessage(prefix + ChatColor.GRAY + "You have successfully broken free of the handcuffs.");
                    }else{
                        player.sendMessage(prefix + ChatColor.RED + "You have failed breaking free of the handcuffs.");
                    }
                }

                if (args[0].equalsIgnoreCase("help")) {
                    player.sendMessage(prefix + ChatColor.DARK_GRAY + "Jail Commands:");
                    player.sendMessage(replaceColorCode("&8/jail struggle - &7Gives the player hand cuffed a configurable chance breaking free of the handcuffs. (Cooldown of 5 minutes)."));
                    player.sendMessage(replaceColorCode("&8/jail reason <player> - &7Check the reason listed for a player being jailed."));
                    player.sendMessage(replaceColorCode("&8/handcuffs - &8Gives the player the handcuff item (cooldown of 5 minutes)."));
                    player.sendMessage(replaceColorCode("&8/jail checktime <player> - &7Check the amount of time a player has remaining in jail."));
                    player.sendMessage(replaceColorCode("&8/jail addtime <player> - &7Adds 5 minutes to a player’s jail time. (Cooldown of 15 minutes)"));
                    player.sendMessage(replaceColorCode("&8/jail removetime <player> - &7Removes 2 minutes from a player’s jail time. (Cooldown of 15 minutes)"));
                    player.sendMessage(replaceColorCode("&8/jail pardon <player> - &7This will release a player immediately from prison."));
                    if(player.hasPermission("jail.admin")) {
                        player.sendMessage(replaceColorCode("&8/jail setdrop <x1> <y1> <z1> <x2> <y2> <z2> - &7Sets up the jail drop off area where the player is sent to jail when they enter the area while handcuffed."));
                        player.sendMessage(replaceColorCode("&8/jail setcell <cellName> - &7Sets a jail cell with the name provided in the location the player is standing when running the command."));
                        player.sendMessage(replaceColorCode("&8/jail delcell <cellName> - &7Deletes a specific cell."));
                        player.sendMessage(replaceColorCode("&8/jail list - &7Lists all cells that are set."));
                        player.sendMessage(replaceColorCode("&8/jail setrelease - &7Sets the release point where the command sender is standing. This is where players who are released from jail will be teleported."));
                    }

                }
                if (player.hasPermission("jail.admin")) {
                    if (args[0].equalsIgnoreCase("setdrop")) {
                        if (args.length != 7) {
                            player.sendMessage(prefix + ChatColor.RED + "Correct usage: /jail setdrop <x1> <y1> <z1> <x2> <y2> <z2>");
                            return false;
                        }
                        Location firstLoc = new Location(player.getWorld(), Double.parseDouble(args[1]), Double.parseDouble(args[2]), Double.parseDouble(args[3]));
                        Location secondLoc = new Location(player.getWorld(), Double.parseDouble(args[4]), Double.parseDouble(args[5]), Double.parseDouble(args[6]));
                        if (firstLoc == null || secondLoc == null) {
                            player.sendMessage(prefix + ChatColor.RED + "Correct usage: /jail setdrop <x1> <y1> <z1> <x2> <y2> <z2>");
                            return false;
                        }
                        List<String> locs = new ArrayList<>();
                        for (Location l : findLocations(firstLoc, secondLoc))
                            locs.add(JailMethods.getStringLocation(l));
                        Config.getConfig().set("handcuff.location", locs);
                        player.sendMessage(prefix + ChatColor.GRAY + "You created the jail drop off area.");
                    }
                    if (args[0].equalsIgnoreCase("setcell")){
                        if (args.length != 2) {
                            player.sendMessage(prefix + ChatColor.RED + "Correct usage: /jail setcell (cellname)");
                            return false;
                        }
                        String jail = args[1];
                        if (JailMethods.data.getData().keySet().contains(jail)) {
                            player.sendMessage(prefix + ChatColor.RED + "A cell named " + jail + " already exists.");
                            return false;
                        }
                        JailMethods.createJail(jail);
                        JailMethods.setLocation(jail, player.getLocation());
                        player.sendMessage(prefix + ChatColor.GRAY + "You created a cell named " + jail);
                    }
                    if (args[0].equalsIgnoreCase("delcell")) {
                        if (args.length != 2) {
                            player.sendMessage(prefix + ChatColor.RED + "Correct usage: /jail delcell (cellname)");
                            return false;
                        }
                        String jail = args[1];
                        if (!JailMethods.data.getData().keySet().contains(jail)) {
                          player.sendMessage(prefix + ChatColor.RED + "A cell named " + jail + " doesn't exist.");
                          return false;
                        }
                        JailMethods.data.remove(jail);
                        player.sendMessage(prefix + ChatColor.GRAY + "You removed the cell named " + jail);
                    }
                    if (args[0].equalsIgnoreCase("list")) {
                        player.sendMessage(prefix + ChatColor.DARK_GRAY + "Cells: ");
                        for(String s : JailMethods.getAllJails()){
                            player.sendMessage(ChatColor.GRAY + s);
                        }
                    }
                    if (args[0].equalsIgnoreCase("setrelease")) {
                        Config.getConfig().set("jail.outlocation", JailMethods.getStringLocation(player.getLocation()));
                        player.sendMessage(prefix + ChatColor.GRAY + "You set the release point.");
                    }
                    if (args[0].equalsIgnoreCase("setitem")) {
                        ItemStack item = player.getInventory().getItemInMainHand();
                        if(item == null){
                            player.sendMessage(prefix + ChatColor.RED + "You don't have any item in your hand.");
                            return false;
                        }
                        Config.getConfig().set("handcuff.item", Serialize.itemStackArrayToBase64(new ItemStack[]{item}));
                        player.sendMessage(prefix + ChatColor.GRAY + "You set the handcuff item to the item in your hand.");
                    }
                }
                if(player.hasPermission("jail.police")){
                    if(args[0].equalsIgnoreCase("reason")){
                        if(args.length != 2){
                            player.sendMessage(prefix + ChatColor.RED + "Correct usage: /jail reason (player)");
                            return false;
                        }
                        Player target = Bukkit.getPlayer(args[1]);
                        if(target == null){
                            player.sendMessage(prefix + ChatColor.RED + "The target is null or offline.");
                            return false;
                        }
                        if(!JailMethods.isJailed(target)){
                            player.sendMessage(prefix + ChatColor.RED + "The target is not jailed.");
                            return false;
                        }
                        player.sendMessage(prefix + ChatColor.RED + "Jail reason: " + ChatColor.GRAY + JailMethods.getJailReason(target));
                    }
                    if(args[0].equalsIgnoreCase("handcuffs")){
                        //add configurable item
                    }
                }
                if(player.hasPermission("jail.doc")){
                    if(args[0].equalsIgnoreCase("checktime")){
                        if(args.length != 2){
                            player.sendMessage(prefix + ChatColor.RED + "Correct usage: /jail checktime (player)");
                            return false;
                        }
                        Player target = Bukkit.getPlayer(args[1]);
                        if(target == null){
                            player.sendMessage(prefix + ChatColor.RED + "The target is null or offline.");
                            return false;
                        }
                        if(!JailMethods.isJailed(target)){
                            player.sendMessage(prefix + ChatColor.RED + "The target is not jailed.");
                            return false;
                        }
                        player.sendMessage(prefix + ChatColor.RED + "Remaining jail time for " + target.getName() + ":" + ChatColor.GRAY + Time.getDateBySeconds(JailMethods.jailSeconds(target)));
                    }
                    if(args[0].equalsIgnoreCase("addtime")){
                        if(args.length != 2){
                            player.sendMessage(prefix + ChatColor.RED + "Correct usage: /jail addtime (player)");
                            return false;
                        }
                        Player target = Bukkit.getPlayer(args[1]);
                        if(target == null){
                            player.sendMessage(prefix + ChatColor.RED + "The target is null or offline.");
                            return false;
                        }
                        if(!JailMethods.isJailed(target)){
                            player.sendMessage(prefix + ChatColor.RED + "The target is not jailed.");
                            return false;
                        }
                        if(JailMethods.isOnCooldown(player, "addtime")){
                            player.sendMessage(prefix + ChatColor.RED + "This command is on cooldown.");
                            return false;
                        }
                        JailMethods.addCooldown(player, 900, "addtime");
                        player.sendMessage(prefix + ChatColor.RED + "You added 5 minutes " + target.getName() + "'s jail time.");
                        Json playerData = new Json(target.getUniqueId().toString(), "plugins/Handcuff/players");
                        playerData.set("jailTime", playerData.getInt("jailTime") + 300);
                    }
                    if(args[0].equalsIgnoreCase("removetime")){
                        if(args.length != 2){
                            player.sendMessage(prefix + ChatColor.RED + "Correct usage: /jail removetime (player)");
                            return false;
                        }
                        Player target = Bukkit.getPlayer(args[1]);
                        if(target == null){
                            player.sendMessage(prefix + ChatColor.RED + "The target is null or offline.");
                            return false;
                        }
                        if(!JailMethods.isJailed(target)){
                            player.sendMessage(prefix + ChatColor.RED + "The target is not jailed.");
                            return false;
                        }
                        if(JailMethods.isOnCooldown(player, "removetime")){
                            player.sendMessage(prefix + ChatColor.RED + "This command is on cooldown.");
                            return false;
                        }
                        JailMethods.addCooldown(player, 900, "removetime");
                        player.sendMessage(prefix + ChatColor.RED + "You removed 2 minutes " + target.getName() + "'s jail time.");
                        Json playerData = new Json(target.getUniqueId().toString(), "plugins/Handcuff/players");
                        playerData.set("jailTime", playerData.getInt("jailTime") - 120);
                    }
                }
                if(player.hasPermission("jail.doc")){
                    if(args[0].equalsIgnoreCase("pardon")) {
                        if (args.length != 2) {
                            player.sendMessage(prefix + ChatColor.RED + "Correct usage: /jail pardon (player)");
                            return false;
                        }
                        Player target = Bukkit.getPlayer(args[1]);
                        if (target == null) {
                            player.sendMessage(prefix + ChatColor.RED + "The target is null or offline.");
                            return false;
                        }
                        if (!JailMethods.isJailed(target)) {
                            player.sendMessage(prefix + ChatColor.RED + "The target is not jailed.");
                            return false;
                        }
                        Json playerData = new Json(target.getUniqueId().toString(), "plugins/Handcuff/players");
                        playerData.set("jailTime", 1);
                        Bukkit.broadcastMessage(prefix + ChatColor.GRAY + target.getName() + " was pardoned by " + player.getName());

                    }
                }
            }
        }
        return false;
    }
}
