package com.winthier.shop.listener;

import com.cavetale.core.money.Money;
import com.winthier.shop.ShopPlugin;
import com.winthier.shop.ShopType;
import com.winthier.shop.chest.ChestShop;
import com.winthier.shop.util.Msg;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public final class ChestListener implements Listener {
    private boolean allowEvent;

    /**
     * When a right clicked chest or sign is identified as
     * part of a shop chest, we want to send an informative
     * message and open the chest. The opening is done
     * manually and the event cancelled so protection plugins
     * don't get involved later.
     */
    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOW)
    public void onPlayerInteract(PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        if (player.isSneaking()) return;
        ChestShop chestShop = ChestShop.getByBlock(block);
        if (chestShop == null) return;
        ShopPlugin.getInstance().getOfferScanner().setDirty(chestShop);
        double price = chestShop.getChestData().getPrice();
        String priceFormat = Money.get().format(price);
        if (chestShop.getChestData().isOwner(player)) {
            if (chestShop.getChestData().getShopType() == ShopType.BUY) {
                Msg.info(player, "Your shop chest sells items for &3%s&r.", priceFormat);
            } else if (chestShop.getChestData().getShopType() == ShopType.SELL) {
                Msg.info(player, "Your shop chest buys items for &3%s&r.", priceFormat);
            }
            chestShop.getChestData().setSoldOut(chestShop.isEmpty());
            chestShop.getChestData().updateInWorld();
        } else {
            if (chestShop.getChestData().getShopType() == ShopType.BUY) {
                Msg.info(player, "%s's shop sells items for &3%s&r.", chestShop.getOwnerName(), priceFormat);
            } else if (chestShop.getChestData().getShopType() == ShopType.SELL) {
                Msg.info(player, "%s's shop buys items for &3%s&r.", chestShop.getOwnerName(), priceFormat);
            }
        }
        event.setCancelled(true);
        allowEvent = true;
        player.openInventory(chestShop.getInventory());
        allowEvent = false;
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (allowEvent) {
            allowEvent = false;
            event.setCancelled(false);
        }
    }
}
