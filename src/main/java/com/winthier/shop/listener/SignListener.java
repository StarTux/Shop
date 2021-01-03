package com.winthier.shop.listener;

import com.winthier.generic_events.GenericEvents;
import com.winthier.shop.BlockLocation;
import com.winthier.shop.ShopPlugin;
import com.winthier.shop.ShopType;
import com.winthier.shop.Shopper;
import com.winthier.shop.chest.ChestData;
import com.winthier.shop.chest.ChestShop;
import com.winthier.shop.util.Msg;
import java.util.UUID;
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

public final class SignListener implements Listener {
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onSignChange(SignChangeEvent event) {
        final Player player = event.getPlayer();
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
        switch (event.getBlock().getRelative(0, -1, 0).getType()) {
        case CHEST:
        case TRAPPED_CHEST:
            break;
        default:
            Msg.warn(player, "There is no chest under this sign");
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
        Shopper owner;
        if ("admin".equalsIgnoreCase(nameLine)) {
            if (!player.hasPermission("shop.create.admin")) {
                Msg.warn(player, "You don't have permission");
                event.setCancelled(true);
                return;
            }
            owner = null;
        } else if (nameLine != null && !nameLine.isEmpty() && !nameLine.equalsIgnoreCase(player.getName())) {
            if (!player.hasPermission("shop.create.other")) {
                Msg.warn(player, "You don't have permission");
                event.setCancelled(true);
                return;
            }
            UUID uuid = GenericEvents.cachedPlayerUuid(nameLine);
            if (uuid == null) {
                event.setCancelled(true);
                Msg.warn(player, "Not found: " + nameLine);
                return;
            }
            owner = new Shopper(uuid, nameLine);
        } else {
            owner = Shopper.of(player);
        }
        BlockLocation location = BlockLocation.of(event.getBlock());
        final ChestData chestData = new ChestData(ChestData.Type.SIGN, shopType, location, owner, price, owner == null);
        ShopPlugin.getInstance().getChestDataStore().store(chestData);
        String priceFormat = GenericEvents.formatMoney(price);
        Msg.info(player, "You created a shop %s items for %s.", (shopType == ShopType.BUY ? "selling" : "buying"), priceFormat);
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
        String priceFormat = GenericEvents.formatMoney(price);
        ChestData chestData = new ChestData(ChestData.Type.NAMED_CHEST, shopType, location, owner, price, false);
        ShopPlugin.getInstance().getChestDataStore().store(chestData);
        Msg.info(player, "You created a shop chest %s items for %s.", (shopType == ShopType.BUY ? "selling" : "buying"), priceFormat);
        ShopPlugin.getInstance().getOfferScanner().setDirty(BlockLocation.of(block));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        checkBrokenBlock(event.getBlock());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockExplode(BlockExplodeEvent event) {
        boolean shouldSave = false;
        for (Block block: event.blockList()) {
            checkBrokenBlock(block);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityExplode(EntityExplodeEvent event) {
        boolean shouldSave = false;
        for (Block block: event.blockList()) {
            checkBrokenBlock(block);
        }
    }

    boolean checkBrokenBlock(Block block) {
        ChestData data = ShopPlugin.getInstance().getChestDataStore().remove(block);
        if (data == null) return false;
        ChestShop shop = ChestShop.getByBlock(block);
        if (shop != null) {
            ShopPlugin.getInstance().getOfferScanner().setDirty(shop);
        } else {
            ShopPlugin.getInstance().getOfferScanner().setDirty(BlockLocation.of(block));
        }
        return true;
    }
}
