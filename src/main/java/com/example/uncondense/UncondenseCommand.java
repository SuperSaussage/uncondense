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
                plugin.getLogger().warning("Invalid mapping in config.yml: " + key + " -> " + val);
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
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        // Optional: /uncondense hand (only main hand)
        boolean handOnly = args.length >= 1 && args[0].equalsIgnoreCase("hand");

        Map<Material, Material> mappings = loadMappings();
        if (mappings.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No mappings configured.");
            return true;
        }

        int totalInputsConsumed = 0;
        int totalOutputsGiven = 0;
        int totalConvertedStacks = 0;

        if (handOnly) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand == null || hand.getType().isAir()) {
                player.sendMessage(ChatColor.RED + "Hold an item in your main hand.");
                return true;
            }
            Material out = mappings.get(hand.getType());
            if (out == null) {
                player.sendMessage(ChatColor.RED + "That item can't be uncondensed on this server.");
                return true;
            }

            int inAmount = hand.getAmount();
            int outAmount = inAmount * 9;

            player.getInventory().setItemInMainHand(null);
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(new ItemStack(out, outAmount));
            leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));

            player.sendMessage(ChatColor.GREEN + "Uncondensed " + inAmount + "x " + hand.getType().name()
                    + " into " + outAmount + "x " + out.name() + ".");
            return true;
        }

        // Full inventory mode (default)
        ItemStack[] contents = player.getInventory().getContents();

        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack stack = contents[slot];
            if (stack == null || stack.getType().isAir()) continue;

            Material inType = stack.getType();
            Material outType = mappings.get(inType);
            if (outType == null) continue;

            int inAmount = stack.getAmount();
            int outAmount = inAmount * 9;

            // Remove the input stack entirely
            player.getInventory().setItem(slot, null);

            // Add output items; drop overflow
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(new ItemStack(outType, outAmount));
            leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));

            totalInputsConsumed += inAmount;
            totalOutputsGiven += outAmount;
            totalConvertedStacks++;
        }

        if (totalConvertedStacks == 0) {
            player.sendMessage(ChatColor.RED + "Nothing in your inventory can be uncondensed.");
            return true;
        }

        player.sendMessage(ChatColor.GREEN + "Uncondensed " + totalInputsConsumed + " block(s) across "
                + totalConvertedStacks + " stack(s) into " + totalOutputsGiven + " item(s).");
        player.sendMessage(ChatColor.GRAY + "Tip: /uncondense hand only converts your main-hand stack.");
        return true;
    }
}
