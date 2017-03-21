package com.winthier.shop;

import com.winthier.shop.chest.ChestDataStore;
import com.winthier.shop.listener.ChestListener;
import com.winthier.shop.listener.InventoryListener;
import com.winthier.shop.listener.MarketListener;
import com.winthier.shop.listener.SignListener;
import com.winthier.shop.playercache.PlayerCacheHandler;
import com.winthier.shop.sql.SQLLog;
import com.winthier.shop.sql.SQLOffer;
import com.winthier.shop.vault.VaultHandler;
import com.winthier.sql.SQLDatabase;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class ShopPlugin extends JavaPlugin {
    @Getter private static ShopPlugin instance;
    private ChestDataStore chestDataStore = null;
    private VaultHandler vaultHandler = null;
    private PlayerCacheHandler playerCacheHandler = null;
    private OfferScanner offerScanner = new OfferScanner();
    private Market market = null;
    private AdminCommand adminCommand = new AdminCommand();
    private boolean debugMode;
    private SQLDatabase db;
    private GenericEventsHandler genericEventsHandler;

    @Override
    public void onEnable() {
        instance = this;
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            vaultHandler = new VaultHandler();
        }
        if (getServer().getPluginManager().getPlugin("PlayerCache") != null) {
            playerCacheHandler = new PlayerCacheHandler();
        }
        if (getServer().getPluginManager().getPlugin("GenericEvents") != null) {
            genericEventsHandler = new GenericEventsHandler();
        }
        db = new SQLDatabase(this);
        db.registerTables(SQLLog.class, SQLOffer.class);
        db.createAllTables();
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

    public void flush() {
        market = null;
        chestDataStore = null;
    }
}
