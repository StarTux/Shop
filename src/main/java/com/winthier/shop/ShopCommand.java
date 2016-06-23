package com.winthier.shop;

import com.winthier.shop.sql.SQLOffer;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShopCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        final Player player = sender instanceof Player ? (Player)sender : null;
        if (player == null) return false;
        if (args.length == 0) return false;
        String firstArg = args[0].toLowerCase();
        if ((firstArg.equals("search") || firstArg.equals("search!")) && args.length >= 2) {
            boolean exact = firstArg.endsWith("!");
            String marketWorld = ShopPlugin.getInstance().getConfig().getString("MarketWorld", "world");
            ShopType shopType = ShopType.BUY;
            List<String> patterns = new ArrayList<>();
            for (int i = 1; i < args.length; ++i) {
                final String arg = args[i];
                if (arg.equalsIgnoreCase("-sell")) {
                    shopType = ShopType.SELL;
                } else if (arg.equalsIgnoreCase("-buy")) {
                    shopType = ShopType.BUY;
                } else if (arg.equalsIgnoreCase("-exact")) {
                    exact = true;
                } else {
                    patterns.add(arg.toLowerCase());
                }
            }
            if (patterns.isEmpty()) return false;
            if (exact) {
                StringBuilder sb = new StringBuilder(patterns.get(0));
                for (int i = 1; i < patterns.size(); ++i) {
                    sb.append(" ").append(patterns.get(0));
                }
                patterns.clear();
                patterns.add(sb.toString());
            }
            List<SQLOffer> offers = new ArrayList<>();
        offerLoop: for (SQLOffer offer: SQLOffer.getAllOffersInWorld(marketWorld)) {
                if (offer.getShopType() != shopType) continue offerLoop;
                String desc = offer.getItemDescription().toLowerCase();
                for (String pattern: patterns) {
                    if (!desc.contains(pattern)) continue offerLoop;
                }
                offers.add(offer);
            }
        }
        return false;
    }
}
