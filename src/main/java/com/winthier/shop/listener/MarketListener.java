package com.winthier.shop.listener;

import com.winthier.shop.Market;
import com.winthier.shop.ShopPlugin;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class MarketListener implements Listener {
    void onMarketEvent(Player player, Block block, Cancellable event) {
        if (block == null) return;
        if (!block.getWorld().getName().equals(ShopPlugin.getInstance().getMarket().getWorld())) return;
        if (player.hasPermission("shop.admin")) {
            if (ShopPlugin.getInstance().getAdminCommand().bump(player, block)) {
                event.setCancelled(true);
                return;
            }
        }
        if (player.hasPermission("shop.market.override")) return;
        Market.Plot plot = ShopPlugin.getInstance().getMarket().plotAt(block);
        if (plot == null || !plot.isAllowed(player)) {
            event.setCancelled(true);
            return;
        }
    }

    void onMarketEvent(Player player, Location location, Cancellable event) {
        onMarketEvent(player, location.getBlock(), event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        onMarketEvent(event.getPlayer(), event.getClickedBlock(), event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        onMarketEvent(event.getPlayer(), event.getRightClicked().getLocation(), event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        onMarketEvent(event.getPlayer(), event.getRightClicked().getLocation(), event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Player player;
        if (event.getDamager() instanceof Player) {
            player = (Player)event.getDamager();
        } else if (event.getDamager() instanceof Projectile && ((Projectile)event.getDamager()).getShooter() instanceof Player) {
            player = (Player)((Projectile)event.getDamager()).getShooter();
        } else {
            return;
        }
        onMarketEvent(player, event.getEntity().getLocation(), event);
    }
}
