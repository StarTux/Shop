package com.winthier.shop;

import com.winthier.generic_events.GenericEvents;
import com.winthier.shop.chest.ChestDataStore;
import com.winthier.shop.listener.ChestListener;
import com.winthier.shop.listener.InventoryListener;
import com.winthier.shop.listener.MarketListener;
import com.winthier.shop.listener.SignListener;
import com.winthier.shop.sql.SQLLog;
import com.winthier.shop.sql.SQLOffer;
import com.winthier.sql.SQLDatabase;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class ShopPlugin extends JavaPlugin {
    @Getter private static ShopPlugin instance;
    private ChestDataStore chestDataStore = null;
    private OfferScanner offerScanner = new OfferScanner();
    private Market market = null;
    private MarketListener marketListener;
    private AdminCommand adminCommand = new AdminCommand();
    private boolean debugMode;
    private SQLDatabase db;

    @Override
    public void onEnable() {
        instance = this;
        saveResource("market.yml", false);
        db = new SQLDatabase(this);
        db.registerTables(SQLLog.class, SQLOffer.class);
        db.createAllTables();
        getServer().getPluginManager().registerEvents(new SignListener(), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(), this);
        getServer().getPluginManager().registerEvents(new ChestListener(), this);
        marketListener = new MarketListener();
        getServer().getPluginManager().registerEvents(marketListener, this);
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
        String name = GenericEvents.cachedPlayerName(uuid);
        if (name != null) return new Shopper(uuid, name);
        return null;
    }

    public Shopper findShopper(String name) {
        UUID uuid = GenericEvents.cachedPlayerUuid(name);
        if (uuid != null) {
            String nname = GenericEvents.cachedPlayerName(uuid);
            if (nname != null) name = nname;
            return new Shopper(uuid, name);
        }
        return null;
    }

    public void flush() {
        market = null;
        chestDataStore = null;
    }
}
