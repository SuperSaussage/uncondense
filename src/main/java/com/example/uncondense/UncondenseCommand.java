package com.example.uncondense;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public final class UncondenseCommand implements CommandExecutor {
    private static final String PERM = "uncondense.use";
    private final UncondensePlugin plugin;

    public UncondenseCommand(UncondensePlugin plugin) {
        this.plugin = plugin;
    }

    private Map<Material, Material> loadMappings() {
        Map<Material, Material> map = new HashMap<>();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("mappings");
        if (sec == null) return map;

        for (String key : sec.getKeys(false)) {
            String val = sec.getString(key);
            if (val == null) continue;

            Material in = Material.matchMaterial(key);
            Material out = Material.matchMaterial(val);

            if (in != null && out != null) {
                map.put(in, out);
            } else {
                plugin.getLogger().warning("Invalid mapping: " + key + " -> " + val);
            }
        }
        return map;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }
        if (!player.hasPermission(PERM)) {
            player.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) {
            player.sendMessage(ChatColor.RED + "Hold a block in your main hand.");
            return true;
        }

        Material outType = loadMappings().get(hand.getType());
        if (outType == null) {
            player.sendMessage(ChatColor.RED + "That block can't be uncondensed here.");
            return true;
        }

        int inAmount = hand.getAmount();
        int toConvert = inAmount;

        if (args.length >= 1) {
            try {
                toConvert = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Usage: /uncondense [amount]");
                return true;
            }
            if (toConvert <= 0) {
                player.sendMessage(ChatColor.RED + "Amount must be >= 1.");
                return true;
            }
            toConvert = Math.min(toConvert, inAmount);
        }

        int outAmount = toConvert * 9;

        // remove from hand
        if (toConvert == inAmount) {
            player.getInventory().setItemInMainHand(null);
        } else {
            hand.setAmount(inAmount - toConvert);
            player.getInventory().setItemInMainHand(hand);
        }

        // add outputs (drop overflow)
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(new ItemStack(outType, outAmount));
        leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));

        player.sendMessage(ChatColor.GREEN + "Uncondensed into " + outAmount + " " + outType.name() + ".");
        return true;
    }
}
