package com.winthier.shop.listener;

import com.cavetale.core.command.RemotePlayer;
import com.cavetale.core.item.ItemKinds;
import com.cavetale.core.money.Money;
import com.cavetale.mytems.item.coin.Coin;
import com.winthier.shop.ShopPlugin;
import com.winthier.shop.ShopType;
import com.winthier.shop.chest.ChestShop;
import com.winthier.shop.sql.SQLChest;
import com.winthier.shop.sql.SQLLog;
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
    private static final String DOT = "\u00D7";

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
                player.sendMessage(text("You cannot sell here", RED));
            }
            if (chestData.getShopType() == ShopType.SELL) {
                player.sendMessage(text("Use shift click to sell items", RED));
            }
            return;
        }
        // shift click item into chest
        if (event.isShiftClick() && !isTopInventory && !isAir(event.getCurrentItem())) {
            if (chestData.getShopType() == ShopType.BUY) {
                // deny shift clicking items in
                player.sendMessage(text("You cannot put items in", RED));
                return;
            }
            // try to sell item to chest
            if (chestData.getShopType() == ShopType.SELL) {
                // todo implement sell chests with item economy
                double price = chestData.getPrice();
                if (Double.isNaN(price)) {
                    player.sendMessage(text("You cannot sell here", RED));
                    return;
                }
                ItemStack buyItem = chestShop.getSellItem(event.getCurrentItem());
                if (buyItem == null) {
                    player.sendMessage(text("You cannot sell this here", RED));
                    return;
                }
                int sold = 0;
                int restStack = event.getCurrentItem().getAmount();
                buyLoop:
                while (restStack >= buyItem.getAmount()) {
                    if (!chestData.isAdminShop() && Money.get().get(chestData.getOwner()) < price) {
                        player.sendMessage(text(chestShop.getOwnerName() + " has run out of money", RED));
                        break buyLoop;
                    }
                    if (!chestData.isAdminShop() && !chestShop.addSlot(buyItem.clone())) {
                        player.sendMessage(text("This chest is full", RED));
                        // chestShop.setSoldOut();
                        break buyLoop;
                    }
                    sold += 1;
                    restStack -= buyItem.getAmount();
                    if (!chestData.isAdminShop()) {
                        Money.get().take(chestData.getOwner(), price, plugin, "Sell Chest");
                    }
                }
                if (sold > 0) {
                    double fullPrice = price * (double) sold;
                    ItemStack soldItem = buyItem.clone();
                    soldItem.setAmount(sold * buyItem.getAmount());
                    Money.get().give(player.getUniqueId(), fullPrice, plugin, "Sold to Chest");
                    if (restStack == 0) {
                        event.setCurrentItem(null);
                    } else {
                        event.getCurrentItem().setAmount(restStack);
                    }
                    player.sendMessage(join(noSeparators(),
                                            text("Sold for ", GREEN),
                                            Coin.format(fullPrice)));
                    SQLLog.store(chestData, player, soldItem, fullPrice, soldItem.getAmount());
                    if (chestShop.isFull()) {
                        chestData.setSoldOut(true);
                        chestData.updateInWorld();
                    }
                    RemotePlayer ownerPlayer = chestData.getRemotePlayer();
                    if (ownerPlayer != null) {
                        ownerPlayer.sendMessage(join(noSeparators(),
                                                     text(player.getName()),
                                                     text(" sold "),
                                                     ItemKinds.chatDescription(soldItem),
                                                     text(" for "),
                                                     Coin.format(fullPrice),
                                                     text(" to you")));
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
                    player.sendMessage(text("You cannot buy here", RED));
                }
                if (chestData.getShopType() == ShopType.SELL) {
                    player.sendMessage(text("You cannot sell here", RED));
                }
            } else {
                if (chestData.getShopType() == ShopType.BUY) {
                    player.sendMessage(join(noSeparators(),
                                            text("Buy this for "),
                                            Coin.format(price),
                                            text(" by shift clicking")));
                }
                if (chestData.getShopType() == ShopType.SELL) {
                    player.sendMessage(join(noSeparators(),
                                            text("Sell this for "),
                                            Coin.format(price),
                                            text(" by shift clicking")));
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
                    player.sendMessage(text("You cannot buy here", RED));
                    return;
                }
                ItemStack item = event.getCurrentItem();
                if (price >= 0.01 && !Money.get().take(player.getUniqueId(), price)) {
                    player.sendMessage(text("You do not have enough money", RED));
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
                        SQLLog.store(chestData, player, item, purchase.price, purchase.amount);
                        Money.get().log(player.getUniqueId(), -purchase.price, plugin, "Buy from Chest");
                        player.sendMessage(join(noSeparators(),
                                                Component.text("Bought "),
                                                text(purchase.amount, YELLOW),
                                                text(DOT, GRAY),
                                                ItemKinds.chatDescription(item.asOne()),
                                                Component.text(" for "),
                                                Coin.format(purchase.price)));
                        if (chestData.isAdminShop()) return;
                        Money.get().log(chestData.getOwner(), purchase.price, plugin, "Sold via Chest");
                        if (purchase == lastPurchase) {
                            lastPurchase = null;
                        }
                        RemotePlayer ownerPlayer = chestData.getRemotePlayer();
                        if (ownerPlayer != null) {
                            ownerPlayer.sendMessage(join(noSeparators(),
                                                         text(player.getName()),
                                                         text(" bought "),
                                                         text(purchase.amount, GREEN),
                                                         text(DOT, GRAY),
                                                         ItemKinds.chatDescription(item.asOne()),
                                                         text(" for "),
                                                         Coin.format(purchase.price)));
                        }
                    }, 20L);
                return;
            }
            if (chestData.getShopType() == ShopType.SELL) {
                player.sendMessage(text("You cannot buy here", RED));
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
