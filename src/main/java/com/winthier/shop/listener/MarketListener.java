package com.winthier.shop.listener;

import com.winthier.generic_events.PlayerCanBuildEvent;
import com.winthier.shop.Market;
import com.winthier.shop.ShopPlugin;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.InventoryHolder;

@RequiredArgsConstructor
public final class MarketListener implements Listener {
    private final ShopPlugin plugin;

    public void onMarketEvent(Player player, Block block, Cancellable event) {
        if (block == null) return;
        if (!block.getWorld().getName().equals(plugin.getMarket().getWorld())) return;
        if (player.hasPermission("shop.market.override")) return;
        Market.Plot plot = plugin.getMarket().plotAt(block);
        if (plot != null && plot.isAllowed(player)) return;
        // plot == null
        if (!plugin.getMarket().isProtect()) return;
        if (event instanceof PlayerInteractEvent) {
            PlayerInteractEvent pie = (PlayerInteractEvent) event;
            if (pie.getAction() == Action.RIGHT_CLICK_BLOCK) {
                Material mat = block.getType();
                if (Tag.DOORS.isTagged(mat)) return;
                if (Tag.BUTTONS.isTagged(mat)) return;
                if (Tag.FENCE_GATES.isTagged(mat)) return;
                switch (mat) {
                case ENCHANTING_TABLE:
                case ENDER_CHEST:
                    return;
                default:
                    pie.setUseInteractedBlock(Event.Result.DENY);
                }
            }
            return;
        }
        event.setCancelled(true);
        return;
    }

    public void onMarketEvent(Player player, Location location, Cancellable event) {
        onMarketEvent(player, location.getBlock(), event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL) return;
        onMarketEvent(event.getPlayer(), event.getClickedBlock(), event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        onMarketEvent(event.getPlayer(), event.getBlock(), event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        onMarketEvent(event.getPlayer(), event.getBlock(), event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockDamage(BlockDamageEvent event) {
        onMarketEvent(event.getPlayer(), event.getBlock(), event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        onMarketEvent(event.getPlayer(), event.getBlock(), event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerBucketFill(PlayerBucketFillEvent event) {
        onMarketEvent(event.getPlayer(), event.getBlock(), event);
    }

    // Frost Walker
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityBlockForm(EntityBlockFormEvent event) {
        Player player = event.getEntity() instanceof Player ? (Player) event.getEntity() : null;
        if (player == null) return;
        onMarketEvent(player, event.getBlock(), event);
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
            player = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile && ((Projectile) event.getDamager()).getShooter() instanceof Player) {
            player = (Player) ((Projectile) event.getDamager()).getShooter();
        } else {
            return;
        }
        onMarketEvent(player, event.getEntity().getLocation(), event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!event.getEntity().getWorld().getName().equals(plugin.getMarket().getWorld())) return;
        if (!plugin.getMarket().isProtect()) return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockPistonExtend(BlockPistonExtendEvent event) {
        if (!event.getBlock().getWorld().getName().equals(plugin.getMarket().getWorld())) return;
        if (!plugin.getMarket().isProtect()) return;
        for (Block block: event.getBlocks()) {
            if (block != null && block.getType() != Material.AIR) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockPistonRetract(BlockPistonRetractEvent event) {
        if (!event.getBlock().getWorld().getName().equals(plugin.getMarket().getWorld())) return;
        if (!plugin.getMarket().isProtect()) return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onHangingBreakByEntity(HangingBreakByEntityEvent event) {
        if (!event.getEntity().getWorld().getName().equals(plugin.getMarket().getWorld())) return;
        if (event.getRemover() instanceof Player) {
            onMarketEvent((Player) event.getRemover(), event.getEntity().getLocation(), event);
        } else {
            if (!plugin.getMarket().isProtect()) return;
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onHangingPlace(HangingPlaceEvent event) {
        onMarketEvent(event.getPlayer(), event.getEntity().getLocation(), event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockFromTo(BlockFromToEvent event) {
        if (!plugin.getMarket().isProtect()) return;
        Block block = event.getBlock();
        if (!block.getWorld().getName().equals(plugin.getMarket().getWorld())) return;
        Market.Plot fromPlot = plugin.getMarket().plotAt(block);
        Market.Plot toPlot = plugin.getMarket().plotAt(event.getToBlock());
        if (fromPlot != toPlot) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerCanBuild(PlayerCanBuildEvent event) {
        onMarketEvent(event.getPlayer(), event.getBlock(), event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof BlockInventoryHolder) {
            BlockInventoryHolder bih = (BlockInventoryHolder) holder;
            onMarketEvent(player, bih.getBlock(), event);
        } else if (holder instanceof Entity) {
            Entity entity = (Entity) holder;
            onMarketEvent(player, entity.getLocation().getBlock(), event);
        }
    }
}
