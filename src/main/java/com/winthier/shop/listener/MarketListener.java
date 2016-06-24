package com.winthier.shop.listener;

import com.winthier.shop.Market;
import com.winthier.shop.ShopPlugin;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

public class MarketListener implements Listener {
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) return;
        if (!block.getWorld().getName().equals(ShopPlugin.getInstance().getMarket().getWorld())) return;
        Player player = event.getPlayer();
        if (event.getPlayer().hasPermission("shop.admin")) {
            if (ShopPlugin.getInstance().getAdminCommand().bump(player, block)) {
                event.setCancelled(true);
                return;
            }
        }
        if (player.hasPermission("shop.market.override")) return;
        Market.Plot plot = ShopPlugin.getInstance().getMarket().plotAt(block);
        if (plot == null || !plot.isAllowed(player)) {
            player.sendMessage("NUT UR PLOT!!!");
            event.setCancelled(true);
            return;
        }
    }
}
