package com.example.uncondense;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

public final class UncondenseCommand implements CommandExecutor {

    private static final String PERM = "uncondense.use";
    private final UncondensePlugin plugin;

    public UncondenseCommand(UncondensePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads mappings from config in the form:
     *   vanilla: "minecraft:diamond_block"
     *   custom:  "itemsadder:my_custom_item"
     */
    private Map<String, String> loadMappings() {
        Map<String, String> map = new HashMap<>();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("mappings");
        if (sec == null) return map;

        for (String key : sec.getKeys(false)) {
            String val = sec.getString(key);
            if (val == null) continue;
            map.put(key.toLowerCase(), val.toLowerCase());
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
            player.sendMessage(ChatColor.RED + "You don't have permission.");
            return true;
        }

        Map<String, String> mappings = loadMappings();
        if (mappings.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No mappings configured.");
            return true;
        }

        boolean handOnly = args.length > 0 && args[0].equalsIgnoreCase("hand");

        int totalInputs = 0;
        int totalOutputs = 0;
        int convertedStacks = 0;

        // =================================
        // --- HAND ONLY MODE
        // =================================
        if (handOnly) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand == null || hand.getType().isAir()) {
                player.sendMessage(ChatColor.RED + "Hold an item in your main hand.");
                return true;
            }

            // Determine ID (vanilla name or ItemsAdder namespace:id)
            String keyId = hand.getType().name().toLowerCase();
            CustomStack csHand = CustomStack.byItemStack(hand);
            if (csHand != null) {
                keyId = csHand.getNamespacedID().toLowerCase();
            }

            String outId = mappings.get(keyId);
            if (outId == null) {
                player.sendMessage(ChatColor.RED + "That item can't be uncondensed.");
                return true;
            }

            int amountIn = hand.getAmount();
            int amountOut = amountIn * 9;

            // Clear the hand
            player.getInventory().setItemInMainHand(null);

            // Determine output stack
            ItemStack outStack;
            if (outId.contains(":")) {
                outStack = CustomStack.get(outId).getItem().clone();
            } else {
                Material matOut = Material.matchMaterial(outId.toUpperCase());
                outStack = new ItemStack(matOut);
            }
            outStack.setAmount(amountOut);

            player.getInventory().addItem(outStack)
                  .values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));

            player.sendMessage(ChatColor.GREEN + "Uncondensed " + amountIn + " into " + amountOut + " items.");
            return true;
        }

        // =================================
        // --- FULL INVENTORY MODE
        // =================================
        for (ItemStack stack : player.getInventory().getContents()) {

            if (stack == null || stack.getType().isAir()) continue;

            // Get ID
            String keyId = stack.getType().name().toLowerCase();
            CustomStack custom = CustomStack.byItemStack(stack);
            if (custom != null) {
                keyId = custom.getNamespacedID().toLowerCase();
            }

            String outId = mappings.get(keyId);
            if (outId == null) continue;

            int inAmt  = stack.getAmount();
            int outAmt = inAmt * 9;

            // Remove the input
            stack.setAmount(0);

            // Prepare output
            ItemStack outStack;
            if (outId.contains(":")) {
                outStack = CustomStack.get(outId).getItem().clone();
            } else {
                Material matOut = Material.matchMaterial(outId.toUpperCase());
                outStack = new ItemStack(matOut);
            }
            outStack.setAmount(outAmt);

            player.getInventory().addItem(outStack)
                  .values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));

            totalInputs += inAmt;
            totalOutputs += outAmt;
            convertedStacks++;
        }

        if (convertedStacks == 0) {
            player.sendMessage(ChatColor.RED + "Nothing to uncondense.");
            return true;
        }

        player.sendMessage(ChatColor.GREEN +
                "Uncondensed " + totalInputs + " items across " + convertedStacks +
                " stacks into " + totalOutputs + " items.");
        return true;
    }
}
