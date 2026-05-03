package com.svncho.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

public class RecycleTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase();

        if (name.equals("recycle")) {
            return List.of();
        }

        if (name.equals("rpadmin")) {
            if (args.length == 1 && sender.hasPermission("recycleplugin.reload")) {
                return "reload".startsWith(args[0].toLowerCase())
                        ? List.of("reload")
                        : List.of();
            }

            return List.of();
        }

        return List.of();
    }
}