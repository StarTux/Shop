package com.winthier.shop;

import com.winthier.generic_events.GenericEventsPlugin;
import com.winthier.generic_events.PlayerCanBuildEvent;
import com.winthier.generic_events.PlayerCanDamageEntityEvent;
import com.winthier.generic_events.PlayerCanGriefEvent;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
public final class GenericEventsHandler implements Listener {
    private final ShopPlugin plugin;

    public String getItemName(ItemStack item) {
        return GenericEventsPlugin.getInstance().getItemName(item);
    }

    @EventHandler
    public void onPlayerCanBuild(PlayerCanBuildEvent event) {
        plugin.getMarketListener().onMarketEvent(event.getPlayer(), event.getBlock(), event);
    }

    @EventHandler
    public void onPlayerCanGrief(PlayerCanGriefEvent event) {
        plugin.getMarketListener().onMarketEvent(event.getPlayer(), event.getBlock(), event);
    }

    @EventHandler
    public void onPlayerCanDamageEntity(PlayerCanDamageEntityEvent event) {
        plugin.getMarketListener().onMarketEvent(event.getPlayer(), event.getEntity().getLocation(), event);
    }
}
