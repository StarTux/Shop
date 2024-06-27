package com.winthier.shop.chest;

import com.winthier.shop.BlockLocation;
import com.winthier.shop.ShopPlugin;
import com.winthier.shop.sql.SQLChest;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;

@RequiredArgsConstructor
public final class ChestDataStore {
    private final ShopPlugin plugin;
    /**
     * The store maps sign locations to chest rows.
     */
    private final Map<BlockLocation, SQLChest> store = new HashMap<>();

    public SQLChest getByChest(Block left, Block right) {
        SQLChest result;
        // Assume block is sign
        result = store.get(BlockLocation.of(left));
        if (result != null) return result;
        if (right != null) {
            result = store.get(BlockLocation.of(right));
            if (result != null) return result;
        }
        // Assume block is chest, possibly below sign
        result = store.get(BlockLocation.of(left.getRelative(0, 1, 0)));
        if (result != null) return result;
        if (right != null) {
            result = store.get(BlockLocation.of(right.getRelative(0, 1, 0)));
            if (result != null) return result;
        }
        return result;
    }

    public SQLChest getByChest(Block block) {
        if (block.getState() instanceof Chest chest) {
            final Inventory inventory = chest.getInventory().getHolder().getInventory();
            if (inventory instanceof DoubleChestInventory doubleChestInventory) {
                final DoubleChest doubleChest = doubleChestInventory.getHolder();
                final Block left = ((Chest) doubleChest.getLeftSide()).getBlock();
                final Block right = ((Chest) doubleChest.getRightSide()).getBlock();
                return getByChest(left, right);
            } else {
                return getByChest(block, null);
            }
        } else {
            return getByChest(block, null);
        }
    }

    public SQLChest getBySign(Block block) {
        return store.get(BlockLocation.of(block));
    }

    public SQLChest remove(Block block) {
        SQLChest toRemove = getByChest(block);
        if (toRemove != null) {
            final SQLChest removed = store.remove(toRemove.getLocation());
            if (toRemove != removed) {
                plugin.getLogger().warning("[ChestDataStore] Unexpected remove result: " + toRemove + " != " + removed);
            }
            plugin.getDb().deleteAsync(toRemove, unusedResult -> {
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("SQLChest deleted: " + toRemove.getId());
                    }
                });
        }
        return toRemove;
    }

    public void store(SQLChest data) {
        store.put(data.getLocation(), data);
        plugin.getDb().saveAsync(data, unused -> {
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("SQLChest saved: " + data.getId());
                }
            });
    }

    public void load() {
        store.clear();
        for (SQLChest chestData : plugin.getDb().find(SQLChest.class).findList()) {
            store.put(chestData.getLocation(), chestData);
        }
        if (plugin.isDebugMode()) {
            plugin.getLogger().info(store.size() + " chest data loaded");
        }
    }
}
