package com.chilieutenant.handcuff;

import com.chilieutenant.handcuff.handcuffs.HandcuffMethods;
import com.chilieutenant.handcuff.handcuffs.HandcuffedPlayer;
import com.chilieutenant.handcuff.jail.JailMethods;
import de.leonhard.storage.Json;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainListener implements Listener {

    public static HashMap<Player, Player> handcufferPlayers = new HashMap<>();

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event){
        String arr[] = event.getMessage().split(" ", 2);

        String firstWord = arr[0];
        if(Config.getConfig().getStringList("jail.useablecommands").contains(firstWord)) return;
        if(JailMethods.getJailedPlayers().contains(event.getPlayer().getName())) event.setCancelled(true);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();
        Json playerData = new Json(player.getUniqueId().toString(), "plugins/Handcuff/players");
        playerData.setDefault("isJailed", false);
        playerData.setDefault("jailTime", 0);
        playerData.setDefault("jail", null);
        playerData.setDefault("kills", 0);
        playerData.setDefault("reason", null);
        if(JailMethods.isJailed(player)){
            JailMethods.timerPlayers.add(player);
            player.teleport(JailMethods.getJailLocation(JailMethods.getJail(player)));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event){
        Player player = event.getPlayer();
        if(JailMethods.timerPlayers.contains(player)){
            JailMethods.timerPlayers.remove(player);
        }
        if(HandcuffedPlayer.isPlayerHandcuffer(player)){
            HandcuffedPlayer hp = HandcuffedPlayer.getHandcuffedPlayerfromHandcuffer(player);
            if(hp != null){
                hp.unHandcuff();
            }
        }
        if(HandcuffedPlayer.isHandcuffed(player)){
            HandcuffedPlayer hp = HandcuffedPlayer.getHandcuffedPlayer(player);
            if(hp != null) {
                hp.penaltyJail();
            }
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event){
        if(event.getEntity() instanceof Player){
            Player player = (Player) event.getEntity();
            Json playerData = new Json(player.getUniqueId().toString(), "plugins/Handcuff/players");
            playerData.set("kills", 0);
            if(HandcuffedPlayer.isPlayerHandcuffer(player)){
                HandcuffedPlayer hp = HandcuffedPlayer.getHandcuffedPlayerfromHandcuffer(player);
                if(hp != null){
                    hp.unHandcuff();
                }
            }
        }
    }

    @EventHandler
    public void onKill(PlayerDeathEvent event){
        Player killer = event.getEntity().getKiller();
        if(killer == null) return;
        Json playerData = new Json(killer.getUniqueId().toString(), "plugins/Handcuff/players");
        playerData.set("kills", playerData.getInt("kills") + 1);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onMessage(AsyncPlayerChatEvent event){
        if(handcufferPlayers.containsKey(event.getPlayer())){
            event.getPlayer().sendMessage(ChatColor.GRAY + "You set the reason of jailing " + handcufferPlayers.get(event.getPlayer()).getName());
            setReason(handcufferPlayers.get(event.getPlayer()), event.getMessage());
            handcufferPlayers.remove(event.getPlayer(), handcufferPlayers.get(event.getPlayer()));
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractAtEntityEvent event) throws IOException {
        if(event.getRightClicked() instanceof Player){
            Player target = (Player) event.getRightClicked();
            Player player = event.getPlayer();
            if(!player.hasPermission("jail.handcuff")){
                return;
            }
            if(HandcuffedPlayer.isHandcuffed(target) || HandcuffedPlayer.isPlayerHandcuffer(player)){
                return;
            }
            ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
            if(item == null){
                return;
            }
            ItemStack configItem = Serialize.itemStackArrayFromBase64(Config.getConfig().getString("handcuff.item"))[0];
            if(!configItem.equals(item)){
                return;
            }
            HandcuffMethods.startHandcuffing(player, target);
        }
    }

    public void setReason(Player player, String reason){
        Json playerData = new Json(player.getUniqueId().toString(), "plugins/Handcuff/players");
        playerData.set("reason", reason);
    }

}
