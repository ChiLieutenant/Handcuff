package com.chilieutenant.handcuff;

import com.chilieutenant.handcuff.jail.JailMethods;
import com.sun.org.apache.xerces.internal.xs.StringList;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JailTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        List<String> commands = new ArrayList<>();

        if (args.length == 1) {
            if(sender.hasPermission("jail.admin")){
                commands.add("setdrop");
                commands.add("setcell");
                commands.add("delcell");
                commands.add("list");
                commands.add("setrelease");
                commands.add("setitem");
            }
            commands.add("help");
            commands.add("struggle");
            commands.add("unhandcuff");
            commands.add("reason");
            commands.add("checktime");
            commands.add("addtime");
            commands.add("removetime");
            commands.add("pardon");
            StringUtil.copyPartialMatches(args[0], commands, completions);
        } else if (args.length == 2) {
            if(args[0].equalsIgnoreCase("setcell") || args[0].equalsIgnoreCase("delcell")){
                commands.addAll(JailMethods.data.getData().keySet());
            }
            StringUtil.copyPartialMatches(args[1], commands, completions);
        }
        Collections.sort(completions);
        return completions;
    }

}
