package com.winthier.shop.chest;

import com.winthier.shop.BlockLocation;
import com.winthier.shop.ShopPlugin;
import com.winthier.shop.sql.SQLChest;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.bukkit.block.Block;

@RequiredArgsConstructor
public final class ChestDataStore {
    private final ShopPlugin plugin;
    private final Map<BlockLocation, SQLChest> store = new HashMap<>();

    public SQLChest getByChest(Block left, Block right) {
        SQLChest result;
        result = store.get(BlockLocation.of(left));
        if (result != null) return result;
        if (right != null) {
            result = store.get(BlockLocation.of(right));
            if (result != null) return result;
        }
        result = store.get(BlockLocation.of(left.getRelative(0, 1, 0)));
        if (result != null) return result;
        if (right != null) {
            result = store.get(BlockLocation.of(right.getRelative(0, 1, 0)));
            if (result != null) return result;
        }
        return result;
    }

    public SQLChest getBySign(Block block) {
        return store.get(BlockLocation.of(block));
    }

    public SQLChest remove(Block block) {
        SQLChest removed = store.remove(BlockLocation.of(block));
        if (removed != null) {
            plugin.getDb().deleteAsync(removed, unused -> {
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("SQLChest deleted: " + removed.getId());
                    }
                });
        }
        return removed;
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
