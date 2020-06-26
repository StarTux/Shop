package com.winthier.shop.listener;

import com.winthier.generic_events.GenericEvents;
import com.winthier.shop.ShopPlugin;
import com.winthier.shop.ShopType;
import com.winthier.shop.Shopper;
import com.winthier.shop.chest.ChestShop;
import com.winthier.shop.sql.SQLLog;
import com.winthier.shop.util.Item;
import com.winthier.shop.util.Msg;
import java.util.Map;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public final class InventoryListener implements Listener {
    /**
     * Allow dragging if it's only in the bottom inventory.
     * Ignore if player is owner or in creative.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        final ChestShop chestShop = ChestShop.getByInventory(event.getInventory());
        if (chestShop == null) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player)event.getWhoClicked();
        ShopPlugin.getInstance().getOfferScanner().setDirty(chestShop);
        if (player.getGameMode() == GameMode.CREATIVE) return;
        boolean isOwner = chestShop.getChestData().isOwner(player);
        boolean isAllowed = ShopPlugin.getInstance().getMarket().isAllowedAt(player, chestShop.getLeft());
        if (isOwner || isAllowed) {
            new BukkitRunnable() {
                @Override public void run() {
                    chestShop.getChestData().setSoldOut(chestShop.isSoldOut());
                    chestShop.getChestData().updateInWorld();
                }
            }.runTask(ShopPlugin.getInstance());
            return;
        }
        boolean isTopInventory = false;
        for (Integer slot : event.getRawSlots()) {
            if (slot < event.getView().getTopInventory().getSize()) {
                isTopInventory = true;
                break;
            }
        }
        if (!isTopInventory) return;
        event.setCancelled(true);
        event.getView().setCursor(event.getOldCursor());
    }

    private static boolean isAir(ItemStack item) {
        return item == null || item.getType() == Material.AIR;
    }

    /**
     * Shop activity is decided by click events in
     * inventories. As a general rule of thumb, when an event
     * causes a purchase, we do the work manually instead of
     * relying on vanilla logic.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getSlotType() == InventoryType.SlotType.OUTSIDE) return;
        if (event.getRawSlot() < 0) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player)event.getWhoClicked();
        final ChestShop chestShop = ChestShop.getByInventory(event.getInventory());
        if (chestShop == null) return;
        ShopPlugin.getInstance().getOfferScanner().setDirty(chestShop);
        if (player.getGameMode() == GameMode.CREATIVE) return;
        boolean isTopInventory = (event.getRawSlot() < event.getView().getTopInventory().getSize());
        boolean isOwner = chestShop.getChestData().isOwner(player);
        boolean isAllowed = ShopPlugin.getInstance().getMarket().isAllowedAt(player, chestShop.getLeft());
        if (isOwner || isAllowed) {
            new BukkitRunnable() {
                @Override public void run() {
                    chestShop.getChestData().setSoldOut(chestShop.isSoldOut());
                    chestShop.getChestData().updateInWorld();
                }
            }.runTask(ShopPlugin.getInstance());
            return;
        }
        // allow left or right clicking in your own inventory
        if (!event.isShiftClick() && !isTopInventory) {
            switch (event.getClick()) {
            case LEFT:
            case RIGHT:
                return;
            default:
                break;
            }
        }
        event.setCancelled(true);
        // deny clicking items in for non-owners
        if (!event.isShiftClick() && isTopInventory && !isAir(event.getCursor())) {
            if (chestShop.getChestData().getShopType() == ShopType.BUY) {
                Msg.warn(player, "You can't sell here.");
            }
            if (chestShop.getChestData().getShopType() == ShopType.SELL) {
                Msg.warn(player, "Use shift click to sell items.");
            }
            return;
        }
        // shift click item into chest
        if (event.isShiftClick() && !isTopInventory && !isAir(event.getCurrentItem())) {
            if (chestShop.getChestData().getShopType() == ShopType.BUY) {
                // deny shift clicking items in
                Msg.warn(player, "You can't put items in.");
                return;
            }
            // try to sell item to chest
            if (chestShop.getChestData().getShopType() == ShopType.SELL) {
                // todo implement sell chests with item economy
                double price = chestShop.getChestData().getPrice();
                if (Double.isNaN(price)) {
                    Msg.warn(player, "You can't sell here.");
                    return;
                }
                ItemStack buyItem = chestShop.getSellItem(event.getCurrentItem());
                if (buyItem == null) {
                    Msg.warn(player, "You can't sell this here.");
                    return;
                }
                int sold = 0;
                int restStack = event.getCurrentItem().getAmount();
            buyLoop:
                while (restStack >= buyItem.getAmount()) {
                    if (!chestShop.getChestData().isAdminShop() && GenericEvents.getPlayerBalance(chestShop.getChestData().getOwner().getUuid()) < price) {
                        Msg.warn(player, "%s has run out of money.", chestShop.getOwnerName());
                        break buyLoop;
                    }
                    if (!chestShop.getChestData().isAdminShop() && !chestShop.addSlot(buyItem.clone())) {
                        Msg.warn(player, "This chest is full.");
                        // chestShop.setSoldOut();
                        break buyLoop;
                    }
                    sold += 1;
                    restStack -= buyItem.getAmount();
                    if (!chestShop.getChestData().isAdminShop()) {
                        GenericEvents.takePlayerMoney(chestShop.getChestData().getOwner().getUuid(), price, ShopPlugin.getInstance(), player.getName() + " sold " + buyItem.getAmount() + "x" + Item.getItemName(buyItem) + " to chest");
                    }
                }
                if (sold > 0) {
                    double fullPrice = price * (double)sold;
                    ItemStack soldItem = buyItem.clone();
                    soldItem.setAmount(sold * buyItem.getAmount());
                    GenericEvents.givePlayerMoney(player.getUniqueId(), fullPrice, ShopPlugin.getInstance(), "Sell " + soldItem.getAmount() + "x" + Item.getItemName(soldItem) + " to " + chestShop.getChestData().getOwnerName() + " via chest shop");
                    if (restStack == 0) {
                        event.setCurrentItem(null);
                    } else {
                        event.getCurrentItem().setAmount(restStack);
                    }
                    Msg.info(player, "Sold for %s.", GenericEvents.formatMoney(fullPrice));
                    Player ownerPlayer = chestShop.getChestData().getPlayer();
                    if (ownerPlayer != null) {
                        Msg.info(ownerPlayer, "%s sold %dx%s for %s to you.", player.getName(), soldItem.getAmount(), Item.getItemName(soldItem), GenericEvents.formatMoney(fullPrice));
                    }
                    SQLLog.store(chestShop.getChestData(), Shopper.of(player), soldItem, fullPrice);
                    if (chestShop.isFull()) {
                        chestShop.getChestData().setSoldOut(true);
                        chestShop.getChestData().updateInWorld();
                    }
                }
                return;
            }
        }
        // single click chest slot to try to take item out
        if (!event.isShiftClick() && isTopInventory && !isAir(event.getCurrentItem())) {
            // deny taking items via single click for non-owners to avoid accidents
            double price = chestShop.getChestData().getPrice();
            if (Double.isNaN(price)) {
                if (chestShop.getChestData().getShopType() == ShopType.BUY) {
                    Msg.warn(player, "You can't buy here.");
                }
                if (chestShop.getChestData().getShopType() == ShopType.SELL) {
                    Msg.warn(player, "You can't sell here.");
                }
            } else {
                if (chestShop.getChestData().getShopType() == ShopType.BUY) {
                    Msg.info(player, "Buy this for %s by shift clicking.", GenericEvents.formatMoney(price));
                }
                if (chestShop.getChestData().getShopType() == ShopType.SELL) {
                    Msg.info(player, "Will pay %s for this item type.", GenericEvents.formatMoney(price));
                }
            }
            return;
        }
        // shift click item out of chest
        if (event.isShiftClick() && isTopInventory && !isAir(event.getCurrentItem())) {
            // make a purchase
            if (chestShop.getChestData().getShopType() == ShopType.BUY) {
                double price = chestShop.getChestData().getPrice();
                if (Double.isNaN(price)) {
                    Msg.warn(player, "You can't buy here.");
                    return;
                }
                ItemStack item = event.getCurrentItem();
                if (!GenericEvents.takePlayerMoney(player.getUniqueId(), chestShop.getChestData().getPrice(), ShopPlugin.getInstance(), "Buy " + item.getAmount() + "x" + Item.getItemName(item) + " from " + chestShop.getChestData().getOwnerName() + " via chest shop")) {
                    Msg.warn(player, "You don't have enough money");
                    return;
                } else {
                    // purchase made
                    Map<Integer, ItemStack> retours;
                    for (ItemStack drop: player.getInventory().addItem(item.clone()).values()) {
                        player.getWorld().dropItem(player.getEyeLocation(), drop);
                    }
                    Msg.info(player, "Bought for %s.", GenericEvents.formatMoney(price));
                    if (!chestShop.getChestData().isAdminShop()) {
                        GenericEvents.givePlayerMoney(chestShop.getChestData().getOwner().getUuid(), price, ShopPlugin.getInstance(), player.getName() + " bought " + item.getAmount() + "x" + Item.getItemName(item) + " via chest shop");
                        Player ownerPlayer = chestShop.getChestData().getPlayer();
                        if (ownerPlayer != null) Msg.info(ownerPlayer, "%s bought %dx%s for %s from you.", player.getName(), item.getAmount(), Item.getItemName(item), GenericEvents.formatMoney(price));
                        event.setCurrentItem(null);
                    } else {
                        event.setCurrentItem(event.getCurrentItem());
                    }
                    SQLLog.store(chestShop.getChestData(), Shopper.of(player), item);
                    if (chestShop.isEmpty()) {
                        chestShop.getChestData().setSoldOut(true);
                        chestShop.getChestData().updateInWorld();
                    }
                    return;
                }
            }
            if (chestShop.getChestData().getShopType() == ShopType.SELL) {
                Msg.warn(player, "You can't buy here.");
                return;
            }
        }
    }
}
