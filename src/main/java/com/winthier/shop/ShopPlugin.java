package com.winthier.shop;

import com.winthier.shop.chest.ChestDataStore;
import com.winthier.shop.chest.ChestShop;
import com.winthier.shop.listener.ChestListener;
import com.winthier.shop.listener.InventoryListener;
import com.winthier.shop.listener.MarketListener;
import com.winthier.shop.listener.SignListener;
import com.winthier.shop.sql.SQLChest;
import com.winthier.shop.sql.SQLLog;
import com.winthier.shop.sql.SQLOffer;
import com.winthier.shop.sql.SQLPlot;
import com.winthier.shop.sql.SQLPlotTrust;
import com.winthier.sql.SQLDatabase;
import java.util.List;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class ShopPlugin extends JavaPlugin {
    @Getter private static ShopPlugin instance;
    @Getter private final ChestDataStore chestDataStore = new ChestDataStore(this);
    private OfferScanner offerScanner = new OfferScanner();
    private final Market market = new Market(this);
    private MarketListener marketListener;
    private boolean debugMode;
    private final SQLDatabase db = new SQLDatabase(this);
    protected final ShopCommand shopCommand = new ShopCommand(this);
    protected final MarketCommand marketCommand = new MarketCommand(this);
    private final AdminCommand adminCommand = new AdminCommand(this);

    @Override
    public void onEnable() {
        instance = this;
        reloadConf();
        db.registerTables(List.of(SQLLog.class,
                                  SQLOffer.class,
                                  SQLChest.class,
                                  SQLPlot.class,
                                  SQLPlotTrust.class));
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
        market.load();
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

    public void reloadChests() {
        chestDataStore.load();
    }

    public void reloadConf() {
        reloadConfig();
        debugMode = getConfig().getBoolean("Debug");
        if (debugMode) getLogger().info("Debug mode enabled");
    }
}
