package com.winthier.shop.listener;

import com.cavetale.core.event.player.PluginPlayerEvent.Detail;
import com.cavetale.core.event.player.PluginPlayerEvent;
import com.cavetale.money.Money;
import com.winthier.playercache.PlayerCache;
import com.winthier.shop.BlockLocation;
import com.winthier.shop.ShopPlugin;
import com.winthier.shop.ShopType;
import com.winthier.shop.Shopper;
import com.winthier.shop.chest.ChestShop;
import com.winthier.shop.sql.SQLChest;
import com.winthier.shop.util.Msg;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

@RequiredArgsConstructor
public final class SignListener implements Listener {
    private final ShopPlugin plugin;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onSignChange(SignChangeEvent event) {
        final Player player = event.getPlayer();
        String title = PlainTextComponentSerializer.plainText().serialize(event.line(0));
        if (!title.startsWith("[")) return;
        if (!title.endsWith("]")) return;
        title = title.substring(1, title.length() - 1);
        ShopType shopType = ShopType.of(title);
        if (shopType == null) return;
        if (!player.hasPermission("shop.create")) {
            Msg.warn(player, "You don't have permission to create a shop");
            event.setCancelled(true);
            return;
        }
        switch (event.getBlock().getRelative(0, -1, 0).getType()) {
        case CHEST:
        case TRAPPED_CHEST:
            break;
        default:
            Msg.warn(player, "There is no chest under this sign");
            event.setCancelled(true);
            return;
        }
        double price = 10.0;
        try {
            String priceLine = PlainTextComponentSerializer.plainText().serialize(event.line(1));
            price = Double.parseDouble(priceLine);
            if (price < 0.0) {
                price = 10.0;
            }
        } catch (NumberFormatException nfe) {
        }
        String nameLine = PlainTextComponentSerializer.plainText().serialize(event.line(3));
        Shopper owner;
        if ("admin".equalsIgnoreCase(nameLine)) {
            if (!player.hasPermission("shop.create.admin")) {
                Msg.warn(player, "You don't have permission");
                event.setCancelled(true);
                return;
            }
            owner = null;
        } else if (nameLine != null && !nameLine.isEmpty() && !nameLine.equalsIgnoreCase(player.getName())) {
            if (!player.hasPermission("shop.create.other")) {
                Msg.warn(player, "You don't have permission");
                event.setCancelled(true);
                return;
            }
            UUID uuid = PlayerCache.uuidForName(nameLine);
            if (uuid == null) {
                event.setCancelled(true);
                Msg.warn(player, "Not found: " + nameLine);
                return;
            }
            owner = new Shopper(uuid, nameLine);
        } else {
            owner = Shopper.of(player);
        }
        BlockLocation location = BlockLocation.of(event.getBlock());
        final SQLChest chestData = new SQLChest(SQLChest.Type.SIGN, shopType, location, owner, price, owner == null);
        plugin.getChestDataStore().store(chestData);
        String priceFormat = Money.format(price);
        Msg.info(player, "You created a shop %s items for %s.", (shopType == ShopType.BUY ? "selling" : "buying"), priceFormat);
        Bukkit.getScheduler().runTask(plugin, chestData::updateInWorld);
        plugin.getOfferScanner().setDirty(BlockLocation.of(event.getBlock().getRelative(0, -1, 0)));
        PluginPlayerEvent.Name.MAKE_SHOP_CHEST.ultimate(plugin, player)
            .detail(Detail.BLOCK, event.getBlock()).call();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {
        final Player player = event.getPlayer();
        Block block = event.getBlock();
        if (block.getType() != Material.CHEST && block.getType() != Material.TRAPPED_CHEST) return;
        ItemStack item = event.getItemInHand();
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;
        String displayName = PlainTextComponentSerializer.plainText().serialize(meta.displayName());
        String[] arr = displayName.split("\\s+", 3);
        if (arr.length != 2) return;
        String shopCode = arr[0];
        String priceCode = arr[1];
        // Shop Code
        if (!shopCode.startsWith("[") || !shopCode.endsWith("]")) return;
        shopCode = shopCode.substring(1, shopCode.length() - 1);
        ShopType shopType = ShopType.of(shopCode);
        if (shopType == null) return;
        // Price Code
        double price;
        try {
            price = Double.parseDouble(priceCode);
        } catch (NumberFormatException nfe) {
            return;
        }
        if (price < 0.0) return;
        // Permission
        if (!player.hasPermission("shop.create")) {
            Msg.warn(player, "You don't have permission to create a shop.");
            event.setCancelled(true);
            return;
        }
        BlockLocation location = BlockLocation.of(event.getBlock());
        Shopper owner = Shopper.of(event.getPlayer());
        String priceFormat = Money.format(price);
        SQLChest chestData = new SQLChest(SQLChest.Type.NAMED_CHEST, shopType, location, owner, price, false);
        plugin.getChestDataStore().store(chestData);
        Msg.info(player, "You created a shop chest %s items for %s.", (shopType == ShopType.BUY ? "selling" : "buying"), priceFormat);
        plugin.getOfferScanner().setDirty(BlockLocation.of(block));
        PluginPlayerEvent.Name.MAKE_SHOP_CHEST.ultimate(plugin, player)
            .detail(Detail.BLOCK, event.getBlock()).call();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        checkBrokenBlock(event.getBlock());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockExplode(BlockExplodeEvent event) {
        boolean shouldSave = false;
        for (Block block: event.blockList()) {
            checkBrokenBlock(block);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityExplode(EntityExplodeEvent event) {
        boolean shouldSave = false;
        for (Block block: event.blockList()) {
            checkBrokenBlock(block);
        }
    }

    boolean checkBrokenBlock(Block block) {
        SQLChest data = plugin.getChestDataStore().remove(block);
        if (data == null) return false;
        ChestShop shop = ChestShop.getByBlock(block);
        if (shop != null) {
            plugin.getOfferScanner().setDirty(shop);
        } else {
            plugin.getOfferScanner().setDirty(BlockLocation.of(block));
        }
        return true;
    }
}
