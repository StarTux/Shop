package com.winthier.shop.listener;

import com.winthier.shop.BlockLocation;
import com.winthier.shop.ShopPlugin;
import com.winthier.shop.ShopType;
import com.winthier.shop.Shopper;
import com.winthier.shop.chest.ChestData;
import com.winthier.shop.util.Msg;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public class SignListener implements Listener {
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onSignChange(SignChangeEvent event) {
        final Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;
        String title = event.getLine(0);
        if (!title.startsWith("[")) return;
        if (!title.endsWith("]")) return;
        title = title.substring(1, title.length() - 1);
        ShopType shopType = ShopType.of(title);
        if (shopType == null) return;
        if (!player.hasPermission("shop.create")) {
            Msg.send(player, "&cYou don't have permission to create a shop");
            event.setCancelled(true);
            return;
        }
        double price = 10.0;
        try {
            String priceLine = event.getLine(1);
            price = Double.parseDouble(event.getLine(1));
            if (price < 0.0) {
                price = 10.0;
            }
        } catch (NumberFormatException nfe) {
        }
        String nameLine = event.getLine(3);
        boolean adminShop = false;
        if ("admin".equalsIgnoreCase(nameLine)) {
            if (!player.hasPermission("shop.create.admin")) {
                Msg.send(player, "&cYou don't have permission");
                event.setCancelled(true);
                return;
            } else {
                adminShop = true;
            }
        }
        Shopper owner = adminShop ? null : Shopper.of(player);
        BlockLocation location = BlockLocation.of(event.getBlock());
        ChestData chestData = new ChestData(ChestData.Type.SIGN, shopType, location, owner, price, adminShop);
        ShopPlugin.getInstance().getChestDataStore().store(chestData);
        ShopPlugin.getInstance().getChestDataStore().save();
        if (event.getBlock().getRelative(0, -1, 0).getType() != Material.CHEST) {
            Msg.send(player, "&bYou created a shop sign. Put a chest underneath to sell things.");
        } else {
            Msg.send(player, "&bYou created a shop chest. Type &f/shop price [price]&b to change the price");
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        if (null != ShopPlugin.getInstance().getChestDataStore().remove(event.getBlock())) {
            ShopPlugin.getInstance().getChestDataStore().save();
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockExplode(BlockExplodeEvent event) {
        boolean shouldSave = false;
        for (Block block: event.blockList()) {
            if (null != ShopPlugin.getInstance().getChestDataStore().remove(block)) {
                shouldSave = true;
            }
        }
        if (shouldSave) ShopPlugin.getInstance().getChestDataStore().save();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityExplode(EntityExplodeEvent event) {
        boolean shouldSave = false;
        for (Block block: event.blockList()) {
            if (null != ShopPlugin.getInstance().getChestDataStore().remove(block)) {
                shouldSave = true;
            }
        }
        if (shouldSave) ShopPlugin.getInstance().getChestDataStore().save();
    }
}
