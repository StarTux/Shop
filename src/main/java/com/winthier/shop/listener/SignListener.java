package com.winthier.shop.listener;

import com.cavetale.core.event.player.PluginPlayerEvent;
import com.cavetale.core.event.player.PluginPlayerEvent.Detail;
import com.cavetale.core.playercache.PlayerCache;
import com.cavetale.mytems.item.coin.Coin;
import com.winthier.shop.BlockLocation;
import com.winthier.shop.ShopPlugin;
import com.winthier.shop.ShopType;
import com.winthier.shop.chest.ChestShop;
import com.winthier.shop.sql.SQLChest;
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
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.format.NamedTextColor.*;

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
            player.sendMessage(text("You do not have permission to create a shop", RED));
            event.setCancelled(true);
            return;
        }
        switch (event.getBlock().getRelative(0, -1, 0).getType()) {
        case CHEST:
        case TRAPPED_CHEST:
            break;
        default:
            player.sendMessage(text("There is no chest under this sign", RED));
            event.setCancelled(true);
            return;
        }
        double price = 10.0;
        try {
            String priceLine = PlainTextComponentSerializer.plainText().serialize(event.line(1));
            price = Double.parseDouble(priceLine);
            if (price < 0.0 || Double.isNaN(price) || Double.isInfinite(price)) {
                price = 10.0;
            }
        } catch (NumberFormatException nfe) {
        }
        String nameLine = PlainTextComponentSerializer.plainText().serialize(event.line(3));
        final UUID owner;
        if ("admin".equalsIgnoreCase(nameLine)) {
            if (!player.hasPermission("shop.create.admin")) {
                player.sendMessage(text("You do not have permission", RED));
                event.setCancelled(true);
                return;
            }
            owner = null;
        } else if (nameLine != null && !nameLine.isEmpty() && !nameLine.equalsIgnoreCase(player.getName())) {
            if (!player.hasPermission("shop.create.other")) {
                player.sendMessage(text("You do not have permission", RED));
                event.setCancelled(true);
                return;
            }
            owner = PlayerCache.uuidForName(nameLine);
            if (owner == null) {
                event.setCancelled(true);
                player.sendMessage(text("Not found: " + nameLine, RED));
                return;
            }
        } else {
            owner = player.getUniqueId();
        }
        BlockLocation location = BlockLocation.of(event.getBlock());
        final SQLChest chestData = new SQLChest(SQLChest.Type.SIGN, shopType, location, owner, price, owner == null);
        plugin.getChestDataStore().store(chestData);
        player.sendMessage(join(noSeparators(),
                                text("You created a shop "
                                     + (shopType == ShopType.BUY ? "selling" : "buying")
                                     + " items for "),
                                Coin.format(price)));
        Bukkit.getScheduler().runTask(plugin, chestData::updateInWorld);
        plugin.getOfferScanner().setDirty(BlockLocation.of(event.getBlock().getRelative(0, -1, 0)));
        PluginPlayerEvent.Name.MAKE_SHOP_CHEST.make(plugin, player)
            .detail(Detail.BLOCK, event.getBlock())
            .callEvent();
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
        if (price < 0.0 || Double.isNaN(price) || Double.isInfinite(price)) {
            return;
        }
        // Permission
        if (!player.hasPermission("shop.create")) {
            player.sendMessage(text("You do not have permission to create a shop", RED));
            event.setCancelled(true);
            return;
        }
        BlockLocation location = BlockLocation.of(event.getBlock());
        UUID owner = event.getPlayer().getUniqueId();
        SQLChest chestData = new SQLChest(SQLChest.Type.NAMED_CHEST, shopType, location, owner, price, false);
        plugin.getChestDataStore().store(chestData);
        player.sendMessage(join(noSeparators(),
                                text("You created a shop "
                                     + (shopType == ShopType.BUY ? "selling" : "buying")
                                     + " items for "),
                                Coin.format(price)));
        plugin.getOfferScanner().setDirty(BlockLocation.of(block));
        PluginPlayerEvent.Name.MAKE_SHOP_CHEST.make(plugin, player)
            .detail(Detail.BLOCK, event.getBlock())
            .callEvent();
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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockDropItem(BlockDropItemEvent event) {
        checkBrokenBlock(event.getBlock());
    }

    private boolean checkBrokenBlock(Block block) {
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
