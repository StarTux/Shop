package com.winthier.shop.chest;
 
import com.winthier.shop.ShopPlugin;
import com.winthier.shop.ShopType;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;

/**
 * Holder of non-unique data related to one chest representing a
 * shop.
 */
@Getter
@RequiredArgsConstructor
public class ChestShop {
    final Inventory inventory;
    final ChestData chestData;
    final Block left, right;

    public static ChestShop getByChest(Block block) {
        if (block.getType() != Material.CHEST && block.getType() != Material.TRAPPED_CHEST) return null;
        Chest chest = (Chest)block.getState();
        Inventory inventory = chest.getBlockInventory();
        Block left = null, right = null;
        ChestData chestData = null;
        inventory = inventory.getHolder().getInventory();
        if (inventory instanceof DoubleChestInventory) {
            DoubleChest doubleChest = ((DoubleChestInventory)inventory).getHolder();
            left = ((Chest)doubleChest.getLeftSide()).getBlock();
            right = ((Chest)doubleChest.getRightSide()).getBlock();
        } else {
            left = block;
        }
        chestData = ShopPlugin.getInstance().getChestDataStore().getByChest(left, right);
        if (chestData == null) return null;
        return new ChestShop(inventory, chestData, left, right);
    }

    public static ChestShop getBySign(Block block) {
        if (block.getType() != Material.SIGN_POST && block.getType() != Material.WALL_SIGN) return null;
        return getByChest(block.getRelative(0, -1, 0));
    }

    public static ChestShop getByInventory(Inventory inventory) {
        if (inventory.getHolder() instanceof Chest) {
            return getByChest(((Chest)inventory.getHolder()).getBlock());
        }
        if (inventory instanceof DoubleChestInventory) {
            return getByChest(((DoubleChestInventory)inventory).getHolder().getLocation().getBlock());
        }
        return null;
    }

    public boolean isEmpty() {
        for (ItemStack item : getInventory().getContents()) {
            if (item != null) return false;
        }
        return true;
    }

    public String getOwnerName() {
        if (getChestData().isAdminShop()) return "The Bank";
        return getChestData().getOwner().getName();
    }

    // Selling Chests

    public ItemStack getSellItem(ItemStack offer) {
        if (getChestData().getShopType() != ShopType.SELL) return null;
        for (ItemStack item : getInventory()) {
            if (item != null && item.isSimilar(offer)) return item.clone();
        }
        return null;
    }

    public boolean addSlot(ItemStack item) {
        Inventory inventory = getInventory();
        for (int i = 0; i < inventory.getSize(); ++i) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, item);
                return true;
            }
        }
        return false;
    }
}
