package com.winthier.shop.listener;

import com.winthier.shop.BlockLocation;
import com.winthier.shop.ShopPlugin;
import com.winthier.shop.ShopType;
import com.winthier.shop.Shopper;
import com.winthier.shop.chest.ChestData;
import com.winthier.shop.util.Msg;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class SignListener implements Listener {
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onSignChange(SignChangeEvent event) {
        final Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;
        String title = event.getLine(0);
        if (!title.startsWith("[")) return;
        if (!title.endsWith("]")) return;
        title = title.substring(1, title.length() - 1);
        ShopType shopType = ShopType.of(title);
        if (shopType == null) return;
        if (!player.hasPermission("shop.create")) {
            Msg.warn(player, "You don't have permission to create a shop");
            event.setCancelled(true);
            return;
        }
        double price = 10.0;
        try {
            String priceLine = event.getLine(1);
            price = Double.parseDouble(event.getLine(1));
            if (price < 0.0) {
                price = 10.0;
            }
        } catch (NumberFormatException nfe) {
        }
        String nameLine = event.getLine(3);
        boolean adminShop = false;
        if ("admin".equalsIgnoreCase(nameLine)) {
            if (!player.hasPermission("shop.create.admin")) {
                Msg.warn(player, "You don't have permission");
                event.setCancelled(true);
                return;
            } else {
                adminShop = true;
            }
        }
        Shopper owner = adminShop ? null : Shopper.of(player);
        BlockLocation location = BlockLocation.of(event.getBlock());
        final ChestData chestData = new ChestData(ChestData.Type.SIGN, shopType, location, owner, price, adminShop);
        ShopPlugin.getInstance().getChestDataStore().store(chestData);
        ShopPlugin.getInstance().getChestDataStore().save();
        String priceFormat = ShopPlugin.getInstance().getVaultHandler().formatMoney(price);
        Msg.info(player, "You created a shop %s items for %s.", shopType.gerund(), priceFormat);
        new BukkitRunnable() {
            @Override public void run() {
                chestData.updateInWorld();
            }
        }.runTask(ShopPlugin.getInstance());
        ShopPlugin.getInstance().getOfferScanner().setDirty(BlockLocation.of(event.getBlock().getRelative(0, -1, 0)));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {
        final Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;
        Block block = event.getBlock();
        if (block.getType() != Material.CHEST && block.getType() != Material.TRAPPED_CHEST) return;
        ItemStack item = event.getItemInHand();
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;
        String[] arr = meta.getDisplayName().split("\\s+", 3);
        if (arr.length != 2) return;
        String shopCode = arr[0];
        String priceCode = arr[1];
        // Shop Code
        if (!shopCode.startsWith("[") || !shopCode.endsWith("]")) return;
        shopCode = shopCode.substring(1, shopCode.length() - 1);
        ShopType shopType = ShopType.of(shopCode);
        if (shopType == null) return;
        // Price Code
        double price;
        try {
            price = Double.parseDouble(priceCode);
        } catch (NumberFormatException nfe) {
            return;
        }
        if (price < 0.0) return;
        // Permission
        if (!player.hasPermission("shop.create")) {
            Msg.warn(player, "You don't have permission to create a shop.");
            event.setCancelled(true);
            return;
        }
        BlockLocation location = BlockLocation.of(event.getBlock());
        Shopper owner = Shopper.of(event.getPlayer());
        String priceFormat = ShopPlugin.getInstance().getVaultHandler().formatMoney(price);
        ChestData chestData = new ChestData(ChestData.Type.NAMED_CHEST, shopType, location, owner, price, false);
        ShopPlugin.getInstance().getChestDataStore().store(chestData);
        ShopPlugin.getInstance().getChestDataStore().save();
        Msg.info(player, "You created a shop chest %s items for %s.", shopType.gerund(), priceFormat);
        ShopPlugin.getInstance().getOfferScanner().setDirty(BlockLocation.of(block));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        if (null != ShopPlugin.getInstance().getChestDataStore().remove(event.getBlock())) {
            ShopPlugin.getInstance().getChestDataStore().save();
            ShopPlugin.getInstance().getOfferScanner().setDirty(BlockLocation.of(event.getBlock()));
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockExplode(BlockExplodeEvent event) {
        boolean shouldSave = false;
        for (Block block: event.blockList()) {
            if (null != ShopPlugin.getInstance().getChestDataStore().remove(block)) {
                shouldSave = true;
                ShopPlugin.getInstance().getOfferScanner().setDirty(BlockLocation.of(block));
            }
        }
        if (shouldSave) ShopPlugin.getInstance().getChestDataStore().save();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityExplode(EntityExplodeEvent event) {
        boolean shouldSave = false;
        for (Block block: event.blockList()) {
            if (null != ShopPlugin.getInstance().getChestDataStore().remove(block)) {
                shouldSave = true;
                ShopPlugin.getInstance().getOfferScanner().setDirty(BlockLocation.of(block));
            }
        }
        if (shouldSave) ShopPlugin.getInstance().getChestDataStore().save();
    }
}
