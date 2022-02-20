package com.chilieutenant.handcuff.handcuffs;

import com.chilieutenant.handcuff.Config;
import com.chilieutenant.handcuff.MainListener;
import com.chilieutenant.handcuff.jail.JailMethods;
import com.chilieutenant.handcuff.jail.Time;
import com.chilieutenant.handcuff.main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class HandcuffedPlayer {

    static List<HandcuffedPlayer> handcuffedPlayers = new ArrayList<>();
    static String prefix = main.replaceColorCode(Config.getConfig().getString("messages.prefix") + " ");
    private Player player;
    private Player handcuffer;
    private boolean isHandcuffed;

    public HandcuffedPlayer(Player player, Player handcuffer){
        this.player = player;
        this.handcuffer = handcuffer;
        this.isHandcuffed = true;
        handcuffedPlayers.add(this);
    }

    public Player getPlayer(){
        return player;
    }

    public Player getHandcuffer(){
        return handcuffer;
    }

    public static boolean isHandcuffed(Player p){
        for(HandcuffedPlayer hp : handcuffedPlayers){
            if(hp.getPlayer().equals(p)) return hp.isHandcuffed();
        }
        return false;
    }

    public void normalJail(){
        int basetime = Config.getConfig().getInt("handcuff.time.basetime");
        int perkill = Config.getConfig().getInt("handcuff.time.perkill");
        int killtime = perkill * JailMethods.getKillCount(player);
        int time = basetime + killtime;
        unHandcuff();
        if(JailMethods.jail(player, time)){
            handcuffer.sendMessage(prefix + ChatColor.GRAY + "You jailed " + player.getName() + " for " + Time.getDateBySeconds(time));
            handcuffer.sendMessage(prefix + ChatColor.GRAY + "Please type the reason why " + player.getName() + " is jailed.");
            MainListener.handcufferPlayers.put(handcuffer, player);
        }else{
            handcuffer.sendMessage(prefix + ChatColor.RED + "The prison is full.");
        }
    }

    public void penaltyJail(){
        int basetime = Config.getConfig().getInt("handcuff.time.basetime");
        int leave = Config.getConfig().getInt("handcuff.time.leave");
        int perkill = Config.getConfig().getInt("handcuff.time.perkill");
        int killtime = perkill * JailMethods.getKillCount(player);
        int time = basetime + killtime + leave;
        unHandcuff();
        if(JailMethods.jail(player, time)){
            handcuffer.sendMessage(prefix + ChatColor.GRAY + "You jailed " + player.getName() + " for " + Time.getDateBySeconds(time));
            handcuffer.sendMessage(prefix + ChatColor.GRAY + "Please type the reason why " + player.getName() + " is jailed.");
            MainListener.handcufferPlayers.put(handcuffer, player);
        }else{
            handcuffer.sendMessage(prefix + ChatColor.RED + "The prison is full.");
        }
    }

    public boolean isHandcuffed(){
        return this.isHandcuffed;
    }

    public void unHandcuff(){
        this.isHandcuffed = false;
    }

    public static boolean isPlayerHandcuffer(Player player){
        for(HandcuffedPlayer hp : handcuffedPlayers){
            if(hp.getHandcuffer().equals(player)) return hp.isHandcuffed();
        }
        return false;
    }

    public static HandcuffedPlayer getHandcuffedPlayerfromHandcuffer(Player player){
        for(HandcuffedPlayer hp : handcuffedPlayers){
            if(hp.getHandcuffer().equals(player)) return hp;
        }
        return null;
    }

    public static HandcuffedPlayer getHandcuffedPlayer(Player player){
        for(HandcuffedPlayer hp : handcuffedPlayers){
            if(hp.getPlayer().equals(player)) return hp;
        }
        return null;
    }

    public static void manageHandcuffs(){
        if(handcuffedPlayers.isEmpty()){
            return;
        }
        for(HandcuffedPlayer hp : handcuffedPlayers){
            if(!hp.isHandcuffed()){
                handcuffedPlayers.remove(hp);
                return;
            }
            hp.progressHandcuff();
        }
    }

    public void progressHandcuff(){
        if(!isHandcuffed){
            return;
        }
        Location frontloc = handcuffer.getLocation().add(handcuffer.getLocation().getDirection().multiply(1));
        Block topblock = JailMethods.getTopBlock(frontloc, 5, 5);
        if(topblock != null){
            if(topblock.getType() == Material.GRASS) topblock = topblock.getRelative(BlockFace.DOWN);
            frontloc.setY(topblock.getY() + 1);
        }
        player.teleport(frontloc);
        List<Location> locs = new ArrayList<>();
        for(String s : Config.getConfig().getStringList("handcuff.location")) {
            locs.add(JailMethods.getLocationString(s));
        }
        for(Location loc : locs) {
            if (player.getLocation().distance(loc) < 1.5) {
                normalJail();
            }
        }
    }

}
