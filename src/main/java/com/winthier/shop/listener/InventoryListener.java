package com.winthier.shop.listener;

import com.cavetale.core.money.Money;
import com.winthier.shop.ShopPlugin;
import com.winthier.shop.ShopType;
import com.winthier.shop.Shopper;
import com.winthier.shop.chest.ChestShop;
import com.winthier.shop.sql.SQLChest;
import com.winthier.shop.sql.SQLLog;
import com.winthier.shop.util.Item;
import com.winthier.shop.util.Msg;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
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
import org.bukkit.scheduler.BukkitTask;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@RequiredArgsConstructor
public final class InventoryListener implements Listener {
    private final ShopPlugin plugin;
    /**
     * Warts: Several players purchasing at once could override each
     * other, cancelling out the 1-second delay. This is tolerable.
     * When the plugin is disabled, will scheduled tasks still
     * execute, or will the be cancelled? This merits a follow-up but
     * is tolerable for the time being.
     */
    private Purchase lastPurchase;

    /**
     * Allow dragging if it's only in the bottom inventory.
     * Ignore if player is owner or in creative.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        final ChestShop chestShop = ChestShop.getByInventory(event.getInventory());
        if (chestShop == null) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        plugin.getOfferScanner().setDirty(chestShop);
        if (player.getGameMode() == GameMode.CREATIVE) return;
        boolean isOwner = chestShop.getChestData().isOwner(player);
        boolean isAllowed = plugin.getMarket().isAllowedAt(player, chestShop.getLeft());
        if (isOwner || isAllowed) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                    chestShop.getChestData().setSoldOut(chestShop.isSoldOut());
                    chestShop.getChestData().updateInWorld();
                });
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
        Player player = (Player) event.getWhoClicked();
        final ChestShop chestShop = ChestShop.getByInventory(event.getInventory());
        if (chestShop == null) return;
        SQLChest chestData = chestShop.getChestData();
        plugin.getOfferScanner().setDirty(chestShop);
        if (player.getGameMode() == GameMode.CREATIVE) return;
        boolean isTopInventory = (event.getRawSlot() < event.getView().getTopInventory().getSize());
        boolean isOwner = chestData.isOwner(player);
        boolean isAllowed = plugin.getMarket().isAllowedAt(player, chestShop.getLeft());
        if (isOwner || isAllowed) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                    chestData.setSoldOut(chestShop.isSoldOut());
                    chestData.updateInWorld();
                });
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
            if (chestData.getShopType() == ShopType.BUY) {
                Msg.warn(player, "You can't sell here.");
            }
            if (chestData.getShopType() == ShopType.SELL) {
                Msg.warn(player, "Use shift click to sell items.");
            }
            return;
        }
        // shift click item into chest
        if (event.isShiftClick() && !isTopInventory && !isAir(event.getCurrentItem())) {
            if (chestData.getShopType() == ShopType.BUY) {
                // deny shift clicking items in
                Msg.warn(player, "You can't put items in.");
                return;
            }
            // try to sell item to chest
            if (chestData.getShopType() == ShopType.SELL) {
                // todo implement sell chests with item economy
                double price = chestData.getPrice();
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
                    if (!chestData.isAdminShop() && Money.get().get(chestData.getOwner()) < price) {
                        Msg.warn(player, "%s has run out of money.", chestShop.getOwnerName());
                        break buyLoop;
                    }
                    if (!chestData.isAdminShop() && !chestShop.addSlot(buyItem.clone())) {
                        Msg.warn(player, "This chest is full.");
                        // chestShop.setSoldOut();
                        break buyLoop;
                    }
                    sold += 1;
                    restStack -= buyItem.getAmount();
                    if (!chestData.isAdminShop()) {
                        String msg = player.getName() + " sold " + buyItem.getAmount() + "x" + Item.getItemName(buyItem);
                        Money.get().take(chestData.getOwner(), price, plugin, msg);
                    }
                }
                if (sold > 0) {
                    double fullPrice = price * (double) sold;
                    ItemStack soldItem = buyItem.clone();
                    soldItem.setAmount(sold * buyItem.getAmount());
                    String msg = "Sell " + soldItem.getAmount() + "x" + Item.getItemName(soldItem)
                        + " to " + chestData.getOwnerName();
                    Money.get().give(player.getUniqueId(), fullPrice, plugin, msg);
                    if (restStack == 0) {
                        event.setCurrentItem(null);
                    } else {
                        event.getCurrentItem().setAmount(restStack);
                    }
                    Msg.info(player, "Sold for %s.", Money.get().format(fullPrice));
                    Player ownerPlayer = chestData.getPlayer();
                    if (ownerPlayer != null) {
                        Msg.info(ownerPlayer, "%s sold %dx%s for %s to you.",
                                 player.getName(), soldItem.getAmount(), Item.getItemName(soldItem),
                                 Money.get().format(fullPrice));
                    }
                    SQLLog.store(chestData, Shopper.of(player), soldItem, fullPrice, soldItem.getAmount());
                    if (chestShop.isFull()) {
                        chestData.setSoldOut(true);
                        chestData.updateInWorld();
                    }
                }
                return;
            }
        }
        // single click chest slot to try to take item out
        if (!event.isShiftClick() && isTopInventory && !isAir(event.getCurrentItem())) {
            // deny taking items via single click for non-owners to avoid accidents
            double price = chestData.getPrice();
            if (Double.isNaN(price)) {
                if (chestData.getShopType() == ShopType.BUY) {
                    Msg.warn(player, "You can't buy here.");
                }
                if (chestData.getShopType() == ShopType.SELL) {
                    Msg.warn(player, "You can't sell here.");
                }
            } else {
                if (chestData.getShopType() == ShopType.BUY) {
                    Msg.info(player, "Buy this for %s by shift clicking.", Money.get().format(price));
                }
                if (chestData.getShopType() == ShopType.SELL) {
                    Msg.info(player, "Will pay %s for this item type.", Money.get().format(price));
                }
            }
            return;
        }
        // shift click item out of chest
        if (event.isShiftClick() && isTopInventory && !isAir(event.getCurrentItem())) {
            // make a purchase
            if (chestData.getShopType() == ShopType.BUY) {
                double price = chestData.getPrice();
                if (Double.isNaN(price)) {
                    Msg.warn(player, "You can't buy here.");
                    return;
                }
                ItemStack item = event.getCurrentItem();
                if (price >= 0.01 && !Money.get().take(player.getUniqueId(), price)) {
                    Msg.warn(player, "You don't have enough money");
                    return;
                }
                if (!chestData.isAdminShop()) {
                    if (!Money.get().give(chestData.getOwner(), price)) {
                        plugin.getLogger().info("Could not give " + price + " to " + chestData.getOwnerName());
                    }
                }
                // purchase made
                for (ItemStack drop: player.getInventory().addItem(item.clone()).values()) {
                    player.getWorld().dropItem(player.getEyeLocation(), drop);
                }
                final Purchase newPurchase = new Purchase(player, chestData, item);
                final Purchase purchase = lastPurchase != null && lastPurchase.canStackWith(newPurchase)
                    ? lastPurchase
                    : newPurchase;
                lastPurchase = purchase;
                purchase.amount += item.getAmount();
                purchase.price += price;
                // Purchase the old task, if any, and schedule a new one.
                if (purchase.task != null) {
                    purchase.task.cancel();
                    purchase.task = null;
                }
                if (!chestData.isAdminShop()) {
                    event.setCurrentItem(null);
                } else {
                    event.setCurrentItem(event.getCurrentItem());
                }
                if (chestShop.isEmpty()) {
                    chestData.setSoldOut(true);
                    chestData.updateInWorld();
                }
                purchase.task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        SQLLog.store(chestData, Shopper.of(player), item, purchase.price, purchase.amount);
                        Money.get().log(player.getUniqueId(), -purchase.price, plugin,
                                  "Buy " + purchase.amount + "x" + Item.getItemName(purchase.item) + " from " + chestData.getOwnerName());
                        String priceFormat = Money.get().format(purchase.price);
                        player.sendMessage(join(noSeparators(), new Component[] {
                                    Component.text("Bought "),
                                    text(purchase.amount, YELLOW),
                                    text("x"),
                                    text(Item.getItemName(item), YELLOW),
                                    Component.text(" for "),
                                    Component.text(priceFormat, GOLD),
                                }));
                        if (chestData.isAdminShop()) return;
                        Money.get().log(chestData.getOwner(), purchase.price, plugin,
                                  player.getName() + " bought " + purchase.amount + "x" + Item.getItemName(purchase.item));
                        Player ownerPlayer = chestData.getPlayer();
                        if (ownerPlayer == null) return;
                        ownerPlayer.sendMessage(join(noSeparators(), new Component[] {
                                    text(player.getName()),
                                    text(" bought "),
                                    text(purchase.amount, YELLOW),
                                    text("x"),
                                    text(Item.getItemName(item), YELLOW),
                                    text(" for "),
                                    text(Money.get().format(purchase.price), GOLD),
                                }));
                        if (purchase == lastPurchase) {
                            lastPurchase = null;
                        }
                    }, 20L);
                return;
            }
            if (chestData.getShopType() == ShopType.SELL) {
                Msg.warn(player, "You can't buy here.");
                return;
            }
        }
    }

    /**
     * We store the purchase for almost a tick to reduce spam in chat
     * and the money logs. Quick consecutive purchases get added up.
     */
    @RequiredArgsConstructor
    private static final class Purchase {
        private final Player player;
        private final SQLChest chestData;
        private final ItemStack item;
        private double price = 0.0;
        private int amount = 0;
        private BukkitTask task;

        public boolean canStackWith(Purchase other) {
            return player == other.player
                && chestData == other.chestData
                && item.isSimilar(other.item);
        }
    }
}
