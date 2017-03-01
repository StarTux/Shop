package com.winthier.shop.chest;

import com.winthier.shop.BlockLocation;
import com.winthier.shop.ShopPlugin;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;

public final class ChestDataStore {
    final Map<BlockLocation, ChestData> store = new HashMap<>();

    // public static ChestData getByChest(Block block) {
    //     return getByInventory(inventory);
    // }

    // public static ChestData getByInventory(Inventory inventory) {
    //     inventory = inventory.getHolder().getInventory();
    //     Block left, right;
    //     if (inventory instanceof DoubleChestInventory) {
    //         DoubleChest doubleChest = ((DoubleChestInventory)inventory).getHolder();
    //         left = ((Chest)doubleChest.getLeftSide()).getBlock();
    //         right = ((Chest)doubleChest.getRightSide()).getBlock();
    //     } else {
    //         left = block;
    //         right = null;
    //     }
    //     return getByChest(left, right);
    // }

    public ChestData getByChest(Block left, Block right) {
        ChestData result;
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

    public ChestData getBySign(Block block) {
        return store.get(BlockLocation.of(block));
    }

    public ChestData remove(Block block) {
        return store.remove(BlockLocation.of(block));
    }

    public void store(ChestData data) {
        store.put(data.getLocation(), data);
    }

    public void load() {
        store.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(getSaveFile());
        for (Map<?, ?> m: yaml.getMapList("store")) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>)m;
                ChestData chestData = ChestData.deserialize(map);
                store.put(chestData.getLocation(), chestData);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        List<Object> list = new ArrayList<>();
        for (ChestData chestData: store.values()) {
            list.add(chestData.serialize());
        }
        yaml.set("store", list);
        try {
            yaml.save(getSaveFile());
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    File getSaveFile() {
        ShopPlugin.getInstance().getDataFolder().mkdirs();
        return new File(ShopPlugin.getInstance().getDataFolder(), "chests.yml");
    }
}
