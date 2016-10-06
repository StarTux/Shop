package com.winthier.shop;

import com.winthier.shop.chest.ChestDataStore;
import com.winthier.shop.listener.ChestListener;
import com.winthier.shop.listener.InventoryListener;
import com.winthier.shop.listener.MarketListener;
import com.winthier.shop.listener.SignListener;
import com.winthier.shop.playercache.PlayerCacheHandler;
import com.winthier.shop.sql.*;
import com.winthier.shop.vault.VaultHandler;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.persistence.PersistenceException;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class ShopPlugin extends JavaPlugin {
    @Getter static ShopPlugin instance;
    ChestDataStore chestDataStore = null;
    VaultHandler vaultHandler = null;
    PlayerCacheHandler playerCacheHandler = null;
    OfferScanner offerScanner = new OfferScanner();
    Market market = null;
    AdminCommand adminCommand = new AdminCommand();
    
    public boolean debugMode;

    @Override
    public void onEnable() {
        instance = this;
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            vaultHandler = new VaultHandler();
        }
        if (getServer().getPluginManager().getPlugin("PlayerCache") != null) {
            playerCacheHandler = new PlayerCacheHandler();
        }
        if (!probeDatabase()) {
            getLogger().info("Installing Shop database due to first time usage");
            installDDL();
        }
        getServer().getPluginManager().registerEvents(new SignListener(), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(), this);
        getServer().getPluginManager().registerEvents(new ChestListener(), this);
        getServer().getPluginManager().registerEvents(new MarketListener(), this);
        getCommand("shop").setExecutor(new ShopCommand());
        getCommand("shopadmin").setExecutor(adminCommand);
        offerScanner.start();
    }

    @Override
    public void onDisable() {
        offerScanner.stop();
    }

    public ChestDataStore getChestDataStore() {
        if (chestDataStore == null) {
            chestDataStore = new ChestDataStore();
            chestDataStore.load();
        }
        return chestDataStore;
    }

    public Market getMarket() {
        if (market == null) {
            market = new Market();
            market.load();
        }
        return market;
    }

    boolean probeDatabase() {
        try {
            for (Class<?> clazz : getDatabaseClasses()) {
                getDatabase().find(clazz).findRowCount();
            }
        } catch (PersistenceException ex) {
            return false;
        }
        return true;
    }

    @Override
    public List<Class<?>> getDatabaseClasses() {
        return Arrays.asList(
            SQLOffer.class,
            SQLLog.class
            );
    }

    public Shopper findShopper(UUID uuid) {
        if (playerCacheHandler != null) {
            String name = playerCacheHandler.nameForUuid(uuid);
            if (name != null) return new Shopper(uuid, name);
        }
        Player pl = getServer().getPlayer(uuid);
        if (pl != null) return Shopper.of(pl);
        return null;
    }

    public Shopper findShopper(String name) {
        if (playerCacheHandler != null) {
            UUID uuid = playerCacheHandler.uuidForName(name);
            if (uuid != null) {
                String nname = playerCacheHandler.nameForUuid(uuid);
                if (nname != null) name = nname;
                return new Shopper(uuid, name);
            }
        }
        Player pl = getServer().getPlayerExact(name);
        if (pl != null) return Shopper.of(pl);
        return null;
    }
}
