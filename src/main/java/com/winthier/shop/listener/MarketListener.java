package com.winthier.shop.listener;

import com.winthier.generic_events.PlayerCanBuildEvent;
import com.winthier.shop.Market;
import com.winthier.shop.ShopPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
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

public final class MarketListener implements Listener {
    public void onMarketEvent(Player player, Block block, Cancellable event) {
        if (block == null) return;
        if (!block.getWorld().getName().equals(ShopPlugin.getInstance().getMarket().getWorld())) return;
        if (player.hasPermission("shop.market.override")) return;
        Market.Plot plot = ShopPlugin.getInstance().getMarket().plotAt(block);
        if (plot != null && plot.isAllowed(player)) return;
        if (event instanceof PlayerInteractEvent) {
            PlayerInteractEvent pie = (PlayerInteractEvent) event;
            if (pie.getAction() == Action.RIGHT_CLICK_BLOCK) {
                switch (block.getType()) {
                case ENCHANTING_TABLE:
                case ENDER_CHEST:
                case ACACIA_DOOR:
                case BIRCH_DOOR:
                case DARK_OAK_DOOR:
                case IRON_DOOR:
                case JUNGLE_DOOR:
                case OAK_DOOR:
                case SPRUCE_DOOR:
                case ACACIA_BUTTON:
                case BIRCH_BUTTON:
                case DARK_OAK_BUTTON:
                case JUNGLE_BUTTON:
                case OAK_BUTTON:
                case SPRUCE_BUTTON:
                case STONE_BUTTON:
                case ACACIA_FENCE_GATE:
                case BIRCH_FENCE_GATE:
                case DARK_OAK_FENCE_GATE:
                case JUNGLE_FENCE_GATE:
                case OAK_FENCE_GATE:
                case SPRUCE_FENCE_GATE:
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
        Player player = event.getEntity() instanceof Player ? (Player)event.getEntity() : null;
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
            player = (Player)event.getDamager();
        } else if (event.getDamager() instanceof Projectile && ((Projectile)event.getDamager()).getShooter() instanceof Player) {
            player = (Player)((Projectile)event.getDamager()).getShooter();
        } else {
            return;
        }
        onMarketEvent(player, event.getEntity().getLocation(), event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!event.getEntity().getWorld().getName().equals(ShopPlugin.getInstance().getMarket().getWorld())) return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockPistonExtend(BlockPistonExtendEvent event) {
        if (!event.getBlock().getWorld().getName().equals(ShopPlugin.getInstance().getMarket().getWorld())) return;
        for (Block block: event.getBlocks()) {
            if (block != null && block.getType() != Material.AIR) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockPistonRetract(BlockPistonRetractEvent event) {
        if (!event.getBlock().getWorld().getName().equals(ShopPlugin.getInstance().getMarket().getWorld())) return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onHangingBreakByEntity(HangingBreakByEntityEvent event) {
        if (!event.getEntity().getWorld().getName().equals(ShopPlugin.getInstance().getMarket().getWorld())) return;
        if (event.getRemover() instanceof Player) {
            onMarketEvent((Player)event.getRemover(), event.getEntity().getLocation(), event);
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onHangingPlace(HangingPlaceEvent event) {
        onMarketEvent(event.getPlayer(), event.getEntity().getLocation(), event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockFromTo(BlockFromToEvent event) {
        Block block = event.getBlock();
        if (!block.getWorld().getName().equals(ShopPlugin.getInstance().getMarket().getWorld())) return;
        Market.Plot fromPlot = ShopPlugin.getInstance().getMarket().plotAt(block);
        Market.Plot toPlot = ShopPlugin.getInstance().getMarket().plotAt(event.getToBlock());
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
