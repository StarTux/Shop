package com.winthier.shop;

import com.winthier.playercache.PlayerCache;
import com.winthier.shop.chest.ChestDataStore;
import com.winthier.shop.chest.ChestShop;
import com.winthier.shop.listener.ChestListener;
import com.winthier.shop.listener.InventoryListener;
import com.winthier.shop.listener.MarketListener;
import com.winthier.shop.listener.SignListener;
import com.winthier.shop.sql.SQLChest;
import com.winthier.shop.sql.SQLLog;
import com.winthier.shop.sql.SQLOffer;
import com.winthier.sql.SQLDatabase;
import java.io.File;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.plugin.java.JavaPlugin;
import com.cavetale.core.connect.NetworkServer;

@Getter
public final class ShopPlugin extends JavaPlugin {
    @Getter private static ShopPlugin instance;
    @Getter private final ChestDataStore chestDataStore = new ChestDataStore(this);
    private OfferScanner offerScanner = new OfferScanner();
    private Market market = new Market();
    private MarketListener marketListener;
    private AdminCommand adminCommand = new AdminCommand(this);
    private boolean debugMode;
    private SQLDatabase db;
    protected ShopCommand shopCommand = new ShopCommand(this);
    protected MarketCommand marketCommand = new MarketCommand(this);

    @Override
    public void onEnable() {
        instance = this;
        reloadConf();
        if (!new File(getDataFolder(), "market.yml").exists()) {
            saveResource("market.yml", false);
        }
        db = new SQLDatabase(this);
        db.registerTables(List.of(SQLLog.class, SQLOffer.class, SQLChest.class));
        db.createAllTables();
        getServer().getPluginManager().registerEvents(new SignListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
        getServer().getPluginManager().registerEvents(new ChestListener(), this);
        marketListener = new MarketListener(this);
        getServer().getPluginManager().registerEvents(marketListener, this);
        shopCommand.enable();
        marketCommand.enable();
        adminCommand.enable();
        SQLOffer.getCache();
        offerScanner.start();
        reloadMarket();
        reloadChests();
    }

    @Override
    public void onDisable() {
        offerScanner.stop();
        for (Player player : Bukkit.getOnlinePlayers()) {
            InventoryView inventoryView = player.getOpenInventory();
            if (inventoryView == null) continue;
            Inventory inventory = inventoryView.getBottomInventory();
            if (inventory == null) continue;
            ChestShop chestShop = ChestShop.getByInventory(inventory);
            if (chestShop == null) continue;
            player.closeInventory();
        }
    }

    public void reloadMarket() {
        market = new Market();
        market.load();
    }

    public void reloadChests() {
        chestDataStore.load();
    }

    public void reloadConf() {
        reloadConfig();
        debugMode = getConfig().getBoolean("Debug");
        if (debugMode) getLogger().info("Debug mode enabled");
    }

    public Shopper findShopper(UUID uuid) {
        String name = PlayerCache.nameForUuid(uuid);
        if (name != null) return new Shopper(uuid, name);
        return null;
    }

    public Shopper findShopper(String name) {
        UUID uuid = PlayerCache.uuidForName(name);
        if (uuid != null) {
            String nname = PlayerCache.nameForUuid(uuid);
            if (nname != null) name = nname;
            return new Shopper(uuid, name);
        }
        return null;
    }

    protected NetworkServer getMasterServer() {
        return switch (NetworkServer.current()) {
        case ALPHA -> NetworkServer.BETA;
        case BETA, CAVETALE -> null;
        default -> NetworkServer.CAVETALE;
        };
    }
}
