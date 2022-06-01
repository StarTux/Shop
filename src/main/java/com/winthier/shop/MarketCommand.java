package com.winthier.shop;

import com.cavetale.core.command.AbstractCommand;

public final class MarketCommand extends AbstractCommand<ShopPlugin> {
    protected MarketCommand(final ShopPlugin plugin) {
        super(plugin, "market");
    }

    @Override
    protected void onEnable() {
        rootNode.denyTabCompletion()
            .description("Warp to the market")
            .remoteServer(plugin.getMasterServer())
            .remotePlayerCaller(plugin.shopCommand::market);
    }
}
