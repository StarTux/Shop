package com.winthier.shop;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.connect.NetworkServer;

public final class MarketCommand extends AbstractCommand<ShopPlugin> {
    protected MarketCommand(final ShopPlugin plugin) {
        super(plugin, "market");
    }

    @Override
    protected void onEnable() {
        rootNode.denyTabCompletion()
            .description("Warp to the market")
            .remoteServer(NetworkServer.current().getManager())
            .remotePlayerCaller(plugin.shopCommand::market);
    }
}
