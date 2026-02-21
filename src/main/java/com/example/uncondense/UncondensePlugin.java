package com.example.uncondense;

import org.bukkit.plugin.java.JavaPlugin;

public final class UncondensePlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        saveDefaultConfig();
        var cmd = getCommand("uncondense");
        if (cmd != null) cmd.setExecutor(new UncondenseCommand(this));
    }
}
