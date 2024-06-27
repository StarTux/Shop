package com.winthier.shop.listener;

import com.cavetale.core.event.block.PlayerBlockAbilityQuery;
import com.cavetale.mytems.item.coin.Coin;
import com.winthier.shop.ShopPlugin;
import com.winthier.shop.ShopType;
import com.winthier.shop.chest.ChestShop;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class ChestListener implements Listener {
    private boolean allowEvent;

    /**
     * When a right clicked chest or sign is identified as
     * part of a shop chest, we want to send an informative
     * message and open the chest. The opening is done
     * manually and the event cancelled so protection plugins
     * do not get involved later.
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
        if (chestShop.getChestData().isOwner(player)) {
            if (chestShop.getChestData().getShopType() == ShopType.BUY) {
                player.sendMessage(join(noSeparators(),
                                        text("Your shop chest sells items for "),
                                        Coin.format(price)));
            } else if (chestShop.getChestData().getShopType() == ShopType.SELL) {
                player.sendMessage(join(noSeparators(),
                                        text("Your shop chest buys items for "),
                                        Coin.format(price)));
            }
            chestShop.getChestData().setSoldOut(chestShop.isEmpty());
            chestShop.getChestData().updateInWorld();
        } else {
            if (chestShop.getChestData().getShopType() == ShopType.BUY) {
                player.sendMessage(join(noSeparators(),
                                        text(chestShop.getOwnerName() + "'s shop sells items for "),
                                        Coin.format(price)));
            } else if (chestShop.getChestData().getShopType() == ShopType.SELL) {
                player.sendMessage(join(noSeparators(),
                                        text(chestShop.getOwnerName() + "'s shop buys items for "),
                                        Coin.format(price)));
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

    /**
     * Deny OPEN to guard against MassStorage drain.
     * Include INVENTORY for potential future uses.  Right now, it
     * only covers lecterns.
     */
    @EventHandler(ignoreCancelled = true)
    private void onPlayerBlockAbility(PlayerBlockAbilityQuery query) {
        switch (query.getAction()) {
        case OPEN: case INVENTORY:
            ChestShop chestShop = ChestShop.getByChest(query.getBlock());
            if (chestShop != null && !chestShop.getChestData().isOwner(query.getPlayer())) {
                query.setCancelled(true);
            }
        default: break;
        }
    }
}
