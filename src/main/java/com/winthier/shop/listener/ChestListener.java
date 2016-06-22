package com.winthier.shop.listener;

import com.winthier.shop.ShopPlugin;
import com.winthier.shop.ShopType;
import com.winthier.shop.chest.ChestData;
import com.winthier.shop.chest.ChestShop;
import com.winthier.shop.util.Msg;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class ChestListener implements Listener {
    /**
     * When a right clicked chest or sign is identified as
     * part of a shop chest, we want to send an informative
     * message and open the chest. The opening is done
     * manually and the event cancelled so protection plugins
     * don't get involved later.
     */
    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (player.isSneaking()) return;
        ChestShop chestShop = ChestShop.getByChest(event.getClickedBlock());
        if (chestShop == null) chestShop = ChestShop.getBySign(event.getClickedBlock());
        if (chestShop == null) return;
        if (chestShop.getChestData().isOwner(player) && player.hasPermission("shop.edit")) {
            // Double price = ShopPlugin.getInstance().getPriceMap().get(player.getName());
            // if (price != null) {
            //     if (chestShop.setPrice(price)) {
            //         Msg.send(player, "&bPrice of this shop chest is now %s.", ShopPlugin.getInstance().getVaultHandler().formatMoney(price));
            //     } else {
            //         Msg.send(player, "&cYou can't change the price of this chest");
            //     }
            //     ShopPlugin.getInstance().getPriceMap().remove(player.getName());
            //     event.setCancelled(true);
            //     return;
            // }
        }
        if (player.getGameMode() == GameMode.CREATIVE) return;
        // if (chestShop.isBlocked()) return;
        if (chestShop.getChestData().isOwner(player)) {
            Msg.send(player, "&bYour Shop Chest");
        } else {
            // if (chestShop.isEmpty()) {
            //     chestShop.getChestData().setSoldOut();
            // }
            Msg.send(player, "&b%s's Shop Chest", chestShop.getOwnerName());
            double price = chestShop.getChestData().getPrice();
            if (!Double.isNaN(price)) {
                if (chestShop.getChestData().getShopType() == ShopType.BUY) {
                    Msg.send(player, "&bYou can buy for %s.", ShopPlugin.getInstance().getVaultHandler().formatMoney(price));
                }
                if (chestShop.getChestData().getShopType() == ShopType.SELL) {
                    Msg.send(player, "&bYou can sell for %s.", ShopPlugin.getInstance().getVaultHandler().formatMoney(price));
                }
            }
        }
        event.setCancelled(true);
        player.openInventory(chestShop.getInventory().getHolder().getInventory());
    }

}
