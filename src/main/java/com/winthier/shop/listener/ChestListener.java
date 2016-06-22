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
        if (player.getGameMode() == GameMode.CREATIVE) return;
        // if (chestShop.isBlocked()) return;
        double price = chestShop.getChestData().getPrice();
        String priceFormat = ShopPlugin.getInstance().getVaultHandler().formatMoney(price);
        if (chestShop.getChestData().isOwner(player)) {
            if (chestShop.getChestData().getShopType() == ShopType.BUY) {
                Msg.info(player, "Your shop chest sells items for %s.", priceFormat);
            } else if (chestShop.getChestData().getShopType() == ShopType.SELL) {
                Msg.info(player, "Your shop chest buys items for %s.", priceFormat);
            }
            chestShop.getChestData().setSoldOut(chestShop.isEmpty());
            chestShop.getChestData().updateInWorld();
        } else {
            if (chestShop.getChestData().getShopType() == ShopType.BUY) {
                Msg.info(player, "%s's shop sells items for %s.", chestShop.getOwnerName(), priceFormat);
            } else if (chestShop.getChestData().getShopType() == ShopType.SELL) {
                Msg.info(player, "%s's shop buys items for %s.", chestShop.getOwnerName(), priceFormat);
            }
        }
        event.setCancelled(true);
        player.openInventory(chestShop.getInventory());
    }
}
