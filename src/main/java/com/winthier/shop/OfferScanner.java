package com.winthier.shop;

import com.winthier.shop.chest.ChestShop;
import com.winthier.shop.sql.SQLOffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import lombok.Getter;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public final class OfferScanner {
    @Getter private final Map<BlockLocation, Long> dirties = new HashMap<>();
    private BukkitRunnable task = null;

    public void setDirty(BlockLocation location) {
        dirties.put(location, System.currentTimeMillis());
    }

    public void setDirty(ChestShop chestShop) {
        setDirty(BlockLocation.of(chestShop.getLeft()));
        if (chestShop.getRight() != null) {
            setDirty(BlockLocation.of(chestShop.getRight()));
        }
    }

    protected void start() {
        task = new BukkitRunnable() {
            @Override public void run() {
                tick();
            }
        };
        task.runTaskTimer(ShopPlugin.getInstance(), 20, 20);
    }

    protected void stop() {
        if (task == null) return;
        try {
            task.cancel();
        } catch (IllegalStateException ise) {
        }
        task = null;
    }

    private void tick() {
        if (dirties.isEmpty()) {
            BlockLocation loc = SQLOffer.findExpiredLocation();
            if (loc != null) setDirty(loc);
        } else {
            Iterator<Map.Entry<BlockLocation, Long>> it = dirties.entrySet().iterator();
            int count = 0;
            while (it.hasNext()) {
                Map.Entry<BlockLocation, Long> e = it.next();
                if (e.getValue() + 10000 < System.currentTimeMillis()) {
                    it.remove();
                    scan(e.getKey());
                    count += 1;
                    if (count >= 20) break;
                }
            }
        }
    }

    private void scan(BlockLocation location) {
        World world = location.getBukkitWorld();
        if (world == null) return;
        world.getChunkAtAsync(location.getX() >> 4, location.getZ() >> 4,
                              (Consumer<Chunk>) c -> callback(location));
    }

    private void callback(BlockLocation location) {
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
        Chest chest = (Chest) state;
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
