package com.winthier.shop.vault;

import com.winthier.shop.ShopPlugin;
import com.winthier.shop.Shopper;
import java.util.UUID;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultHandler {
    Permission permission = null;
    Economy economy = null;

    public Permission getPermission() {
        if (permission == null) {
            RegisteredServiceProvider<Permission> permissionProvider = Bukkit.getServer().getServicesManager().getRegistration(Permission.class);
            if (permissionProvider != null) permission = permissionProvider.getProvider();
        }
        return permission;
    }

    public Economy getEconomy() {
        if (economy == null) {
            RegisteredServiceProvider<Economy> economyProvider = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
            if (economyProvider != null) economy = economyProvider.getProvider();
        }
        return economy;
    }

    // Permission
    
    public boolean hasPermission(UUID uuid, String permission) {
        return getPermission().playerHas((String)null, Bukkit.getServer().getOfflinePlayer(uuid), permission);
    }

    // Economy

    public boolean hasMoney(UUID uuid, double money) {
        if (money < 0.0) return false;
        return getEconomy().has(Bukkit.getServer().getOfflinePlayer(uuid), money);
    }

    public boolean giveMoney(Shopper player, double money) {
        if (money < 0.0) return false;
        EconomyResponse resp = getEconomy().depositPlayer(Bukkit.getServer().getOfflinePlayer(player.getUuid()), money);
        ShopPlugin.getInstance().getLogger().info(String.format("Give %s to %s: %s", formatMoney(money), player.getName(), resp.transactionSuccess()));
        return resp.transactionSuccess();
    }

    public boolean takeMoney(Shopper player, double money) {
        if (money < 0.0) return false;
        EconomyResponse resp = getEconomy().withdrawPlayer(Bukkit.getServer().getOfflinePlayer(player.getUuid()), money);
        ShopPlugin.getInstance().getLogger().info(String.format("Take %s from %s: %s", formatMoney(money), player.getName(), resp.transactionSuccess()));
        return resp.transactionSuccess();
    }

    public String formatMoney(double money) {
        return getEconomy().format(money);
    }
}
