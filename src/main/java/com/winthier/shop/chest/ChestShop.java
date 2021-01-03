package com.winthier.shop.chest;

import com.winthier.shop.ShopPlugin;
import com.winthier.shop.ShopType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Holder of non-unique data related to one chest representing a
 * shop.
 */
@Getter
@RequiredArgsConstructor
public final class ChestShop {
    final Inventory inventory;
    final ChestData chestData;
    final Block left;
    final Block right;

    public static ChestShop getByChest(Block block) {
        if (block.getType() != Material.CHEST && block.getType() != Material.TRAPPED_CHEST) return null;
        Chest chest = (Chest) block.getState();
        Inventory inventory = chest.getBlockInventory();
        Block left = null;
        Block right = null;
        ChestData chestData = null;
        inventory = inventory.getHolder().getInventory();
        if (inventory instanceof DoubleChestInventory) {
            DoubleChest doubleChest = ((DoubleChestInventory) inventory).getHolder();
            left = ((Chest) doubleChest.getLeftSide()).getBlock();
            right = ((Chest) doubleChest.getRightSide()).getBlock();
        } else {
            left = block;
        }
        chestData = ShopPlugin.getInstance().getChestDataStore().getByChest(left, right);
        if (chestData == null) return null;
        return new ChestShop(inventory, chestData, left, right);
    }

    public static ChestShop getBySign(Block block) {
        BlockData blockData = block.getBlockData();
        if (blockData instanceof Sign || blockData instanceof WallSign) {
            return getByChest(block.getRelative(0, -1, 0));
        }
        return null;
    }

    public static ChestShop getByBlock(Block block) {
        ChestShop result = getByChest(block);
        if (result != null) return result;
        result = getBySign(block);
        return result;
    }

    public static ChestShop getByInventory(Inventory inventory) {
        if (inventory.getHolder() instanceof Chest) {
            return getByChest(((Chest) inventory.getHolder()).getBlock());
        }
        if (inventory instanceof DoubleChestInventory) {
            return getByChest(((DoubleChestInventory) inventory).getHolder().getLocation().getBlock());
        }
        return null;
    }

    public boolean isEmpty() {
        for (ItemStack item : getInventory().getContents()) {
            if (item != null) return false;
        }
        return true;
    }

    public boolean isFull() {
        for (ItemStack item : getInventory().getContents()) {
            if (item == null) return false;
        }
        return true;
    }

    public boolean isSoldOut() {
        switch (getChestData().getShopType()) {
        case BUY: return isEmpty();
        case SELL: return isFull();
        default: return false;
        }
    }

    public String getOwnerName() {
        return getChestData().getOwnerName();
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
        Inventory inv = getInventory();
        for (int i = 0; i < inv.getSize(); ++i) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, item);
                return true;
            }
        }
        return false;
    }
}
