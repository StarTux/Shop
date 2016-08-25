package com.winthier.shop;

import com.winthier.shop.chest.ChestShop;
import com.winthier.shop.sql.SQLOffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class OfferScanner {
    Map<BlockLocation, Long> dirties = new HashMap<>();
    BukkitRunnable task = null;
    final long INTERVAL = 10;
    long counter = 0;
    
    public void setDirty(BlockLocation location) {
        dirties.put(location, System.currentTimeMillis());
    }

    public void setDirty(ChestShop chestShop) {
        setDirty(BlockLocation.of(chestShop.getLeft()));
        if (chestShop.getRight() != null) {
            setDirty(BlockLocation.of(chestShop.getRight()));
        }
    }

    void start() {
        task = new BukkitRunnable() {
            @Override public void run() {
                tick();
            }
        };
        task.runTaskTimer(ShopPlugin.getInstance(), 20*INTERVAL, 20*INTERVAL);
    }

    void stop() {
        if (task == null) return;
        try {
            task.cancel();
        } catch (IllegalStateException ise) {
        }
        task = null;
    }

    void tick() {
        long now = System.currentTimeMillis();
        if (counter > 60*5) {
            counter = 0;
            for (SQLOffer offer: SQLOffer.getAllOffers()) {
                if (offer.getTime().getTime() + 1000*60*60 > now) {
                    setDirty(offer.getBlockLocation());
                }
            }
        }
        counter += INTERVAL;
        Iterator<Map.Entry<BlockLocation, Long>> it = dirties.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockLocation, Long> e = it.next();
            if (e.getValue() + 10000 < System.currentTimeMillis()) {
                it.remove();
                scan(e.getKey());
                return;
            }
        }
    }

    void scan(BlockLocation location) {
        ChestShop chestShop = ChestShop.getByChest(location.getBlock());
        if (chestShop == null) {
            SQLOffer.deleteAt(location);
            return;
        }
        BlockState state = location.getBlock().getState();
        if (!(state instanceof Chest)) {
            SQLOffer.deleteAt(location);
            return;
        }
        Chest chest = (Chest)state;
        Inventory inv = chest.getBlockInventory();
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item: inv) {
            if (item != null && item.getType() != Material.AIR) {
                items.add(item);
            }
        }
        SQLOffer.store(location, chestShop.getChestData(), items);
    }
}
