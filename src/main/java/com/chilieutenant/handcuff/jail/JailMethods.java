package com.chilieutenant.handcuff.jail;

import com.chilieutenant.handcuff.Config;
import com.chilieutenant.handcuff.Serialize;
import de.leonhard.storage.Json;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JailMethods {

    public static Json data = new Json("Jails", "plugins/Handcuff/Jail");
    public static List<Player> timerPlayers = new ArrayList<>();

    public static List<String> getAllJails(){
        List<String> jails = new ArrayList<>();
        for(String s : data.getData().keySet()){
            if(getJailLocation(s) != null) jails.add(s);
        }
        return jails;
    }

    public static List<String> getJailedPlayers(){
        List<String> players = new ArrayList<>();
        for(String jail : getAllJails()){
            String player = jailplayerName(jail);
            if(player != null && !player.equalsIgnoreCase("")) players.add(player);
        }
        return players;
    }

    public static Location getJailLocation(String name){
        return getLocationString(data.getString(name + ".location"));
    }

    public static String jailplayerName(String name){
        return data.getString(name + ".player");
    }

    public static boolean isAvailable(String name){
        return data.getBoolean(name + ".isAvailable");
    }

    public static void createJail(String name){
        data.set(name + ".player", null);
        data.set(name + ".location", null);
        data.set(name + ".isAvailable", true);
    }

    static public String getStringLocation(final Location l) {
        if (l == null) {
            return "";
        }
        return l.getWorld().getName() + ":" + l.getBlockX() + ":" + l.getBlockY() + ":" + l.getBlockZ();
    }

    static public Location getLocationString(final String s) {
        if (s == null || s.trim() == "") {
            return null;
        }
        final String[] parts = s.split(":");
        if (parts.length == 4) {
            final World w = Bukkit.getServer().getWorld(parts[0]);
            final int x = Integer.parseInt(parts[1]);
            final int y = Integer.parseInt(parts[2]);
            final int z = Integer.parseInt(parts[3]);
            return new Location(w, x, y, z);
        }
        return null;
    }

    public static String getAvailableJail(){
        for(String jail : getAllJails()){
            if(isAvailable(jail)) return jail;
        }
        return null;
    }

    public static void manageTimers() throws IOException {
        if(timerPlayers.isEmpty()){
            if (!getJailedPlayers().isEmpty()) {
                for (String name : getJailedPlayers()) {
                    Player player = Bukkit.getPlayer(name);
                    if (player != null && isJailed(player)) timerPlayers.add(player);
                }
            }
            return;
        }
        for(Player p : timerPlayers){
            Json playerData = new Json(p.getUniqueId().toString(), "plugins/Handcuff/players");
            playerData.set("jailTime", jailSeconds(p) - 1);
            if(jailSeconds(p) <= 0){
                unJail(p);
                timerPlayers.remove(p);
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.GRAY + "You are released from prison."));
                return;
            }else{
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.GRAY + "You will be released from prison in " + ChatColor.DARK_GRAY + Time.getDateBySeconds(jailSeconds(p))));
            }
        }

    }

    public static void unJail(Player player) throws IOException {
        Json playerData = new Json(player.getUniqueId().toString(), "plugins/Handcuff/players");
        String jail = playerData.getString("jail");
        playerData.set("isJailed", false);
        playerData.set("jailTime", 0);
        playerData.set("jail", null);
        data.set(jail + ".isAvailable", true);
        data.set(jail + ".player", null);
        for(int i = 0; i < getSavedInventory(player).size(); i++){
            if(getSavedInventory(player).get(i) != null) player.getInventory().setItem(i, getSavedInventory(player).get(i));
        }
        player.getInventory().setArmorContents(new ItemStack[]{});
        player.updateInventory();
        player.teleport(getLocationString(Config.getConfig().getString("jail.outlocation")));
    }

    public static boolean jail(Player player, int seconds){
        Json playerData = new Json(player.getUniqueId().toString(), "plugins/Handcuff/players");
        String jail = getAvailableJail();
        if(jail == null || jail.equalsIgnoreCase("")){
            return false;
        }
        playerData.set("isJailed", true);
        playerData.set("jailTime", seconds);
        playerData.set("jail", jail);
        playerData.set("kills", 0);
        data.set(jail + ".isAvailable", false);
        data.set(jail + ".player", player.getName());
        timerPlayers.add(player);
        player.teleport(getJailLocation(jail));
        saveInventory(player);
        player.getInventory().clear();
        player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 4));
        return true;
    }

    public static boolean isJailed(Player player){
        Json playerData = new Json(player.getUniqueId().toString(), "plugins/Handcuff/players");
        return playerData.getBoolean("isJailed");
    }

    public static String getJailReason(Player player){
        Json playerData = new Json(player.getUniqueId().toString(), "plugins/Handcuff/players");
        return playerData.getString("reason");
    }

    public static String getJail(Player player){
        Json playerData = new Json(player.getUniqueId().toString(), "plugins/Handcuff/players");
        return playerData.getString("jail");
    }

    public static int jailSeconds(Player player){
        Json playerData = new Json(player.getUniqueId().toString(), "plugins/Handcuff/players");
        return playerData.getInt("jailTime");
    }

    public static int getKillCount(Player player){
        Json playerData = new Json(player.getUniqueId().toString(), "plugins/Handcuff/players");
        return playerData.getInt("kills");
    }

    public static void setLocation(String name, Location location){
        data.set(name + ".location", getStringLocation(location));
    }

    public static boolean isAir(final Material material) {
        return material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR;
    }

    public static Block getTopBlock(final Location loc, final int positiveY, final int negativeY) {
        Block blockHolder = loc.getBlock();
        int y = 0;
        // Only one of these while statements will go
        while (!isAir(blockHolder.getType()) && Math.abs(y) < Math.abs(positiveY)) {
            y++;
            final Block tempBlock = loc.clone().add(0, y, 0).getBlock();
            if (isAir(tempBlock.getType())) {
                return blockHolder;
            }
            blockHolder = tempBlock;
        }

        while (isAir(blockHolder.getType()) && Math.abs(y) < Math.abs(negativeY)) {
            y--;
            blockHolder = loc.clone().add(0, y, 0).getBlock();
            if (!isAir(blockHolder.getType())) {
                return blockHolder;
            }
        }
        return blockHolder;
    }

    public static void addCooldown(Player player, int seconds, String key){
        Json playerData = new Json(player.getUniqueId().toString(), "plugins/Handcuff/players");
        playerData.set(key + "cooldown", System.currentTimeMillis() + (seconds * 1000));
    }

    public static boolean isOnCooldown(Player player, String key){
        Json playerData = new Json(player.getUniqueId().toString(), "plugins/Handcuff/players");
        return System.currentTimeMillis() < playerData.getLong(key + "cooldown");
    }

    public static void saveInventory(Player player){
        Json playerData = new Json(player.getUniqueId().toString(), "plugins/Handcuff/players");
        //playerData.set("inventory", Serialize.toBase64(player.getInventory()));
        playerData.set("inventory", Serialize.serializeItems(player.getInventory().getContents()));
    }

    public static List<ItemStack> getSavedInventory(Player player) throws IOException {
        Json playerData = new Json(player.getUniqueId().toString(), "plugins/Handcuff/players");
        return Serialize.deserializeItems(playerData.getString("inventory"));
    }

}
