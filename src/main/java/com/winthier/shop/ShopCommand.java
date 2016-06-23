package com.winthier.shop;

import com.winthier.shop.sql.SQLOffer;
import com.winthier.shop.util.Msg;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class ShopCommand implements CommandExecutor {
    final Map<UUID, PlayerContext> contexts = new HashMap<>();
    final Comparator<SQLOffer> sqlOfferComparator = new Comparator<SQLOffer>() {
        @Override public int compare(SQLOffer a, SQLOffer b) {
            return Double.compare(a.pricePerItem(), b.pricePerItem());
        }
    };
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        final Player player = sender instanceof Player ? (Player)sender : null;
        if (player == null) return false;
        if (args.length == 0) return false;
        String firstArg = args[0].toLowerCase();
        if ((firstArg.equals("search")||
             firstArg.equals("search!") ||
             firstArg.equals("s") ||
             firstArg.equals("s!"))) {
            if (!shopSearch(player, args)) {
                Msg.info(player, "/Shop Search &7Usage");
                Msg.send(player, "/Shop Search &7&o... &8-&r Search for items");
                Msg.send(player, "/Shop Search! &7&o... &8-&r Search for exact word combo");
                Msg.send(player, "/Shop Search -Sell &7&o... &8-&r Search for selling shops");
            }
        } else if (firstArg.equals("page")) {
            if (!shopPage(player, args)) {
                Msg.info(player, "/Shop Page &7Usage");
                Msg.send(player, "/Shop Page &7&o<Number> &8-&r View page of previous search");
            }
        } else if (firstArg.equals("port")) {
            shopPort(player, args);
        }
        return true;
    }

    boolean shopSearch(Player player, String[] args) {
        if (args.length < 2) return false;
        boolean exact = args[0].endsWith("!");
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
                sb.append(" ").append(patterns.get(i));
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
        if (offers.isEmpty()) {
            Msg.warn(player, "Nothing found.");
            return true;
        }
        Collections.sort(offers, sqlOfferComparator);
        getPlayerContext(player).clear();
        int offerIndex = 0;
        List<List<Object>> lines = new ArrayList<>();
        for (SQLOffer offer: offers) {
            List<Object> json = new ArrayList<>();
            json.add(" ");
            json.add(Msg.button(ChatColor.BLUE, "&r[&9Port&r]", "Port to this shop chest", "/shop port " + offerIndex));
            json.add(" ");
            json.add(Msg.format("%s", offer.getOwnerName()));
            if (offer.getShopType() == ShopType.BUY) {
                json.add(Msg.format(" &8sells&r "));
            } else {
                json.add(Msg.format(" &8buys&r "));
            }
            json.add("" + offer.getItemAmount());
            json.add(Msg.format("&8x&r"));
            json.add(offer.getItemDescription());
            lines.add(json);
            getPlayerContext(player).locations.add(offer.getBlockLocation());
            offerIndex += 1;
        }
        getPlayerContext(player).pages.addAll(Page.pagesOf(lines));
        showPage(player, 0);
        return true;
    }

    boolean shopPage(Player player, String[] args) {
        if (args.length != 2) return false;
        try {
            int index = Integer.parseInt(args[1]) - 1;
            showPage(player, index);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    boolean shopPort(Player player, String[] args) {
        if (args.length != 2) return false;
        try {
            int portIndex = Integer.parseInt(args[1]);
            boolean res = portToShop(player, portIndex);
            if (res) {
                Msg.info(player, "Ported to shop.");
            } else {
                Msg.warn(player, "Could not port to shop.");
            }
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    void showPage(Player player, int index) {
        int pageCount = getPlayerContext(player).pages.size();
        if (index < 0 || index >= pageCount) return;
        Page page = getPlayerContext(player).pages.get(index);
        Msg.info(player, "Page %d/%d", index+1, pageCount);
        for (List<Object> json: page.lines) {
            Msg.raw(player, json);
        }
        if (index+1 < pageCount) {
            Msg.raw(player,
                Msg.button(ChatColor.BLUE, "&r[&9More&r]", "Next page", "/shop page " + (index+1))
                );
        }
    }

    static boolean blocks(Block block) {
        return block.getType().isSolid();
    }

    boolean portToShop(Player player, int index) {
        if (index < 0 || index >= getPlayerContext(player).locations.size()) return false;
        Location loc = savePortLocation(getPlayerContext(player).locations.get(index));
        if (loc == null) return false;
        player.teleport(loc);
        return true;
    }

    Location savePortLocation(BlockLocation location) {
        List<Block> nbors = new ArrayList<>();
    faceLoop: for (BlockFace face: Arrays.<BlockFace>asList(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST)) {
            Block block = location.getBlock().getRelative(face);
            int count = 0;
            while (block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST) {
                block = block.getRelative(face);
                count += 1;
                if (count > 5) continue faceLoop;
            }
            nbors.add(block);
        }
        if (nbors.isEmpty()) return null;
        List<Block> results = new ArrayList<>();
    nborLoop: for (Block nbor: nbors) {
            if (blocks(nbor)) {
                int count = 0;
                while (blocks(nbor.getRelative(BlockFace.UP, 1)) ||
                       blocks(nbor.getRelative(BlockFace.UP, 2))) {
                    nbor = nbor.getRelative(BlockFace.UP);
                    count += 1;
                    if (count > 5) continue nborLoop;
                }
                results.add(nbor.getRelative(BlockFace.UP));
            } else {
                int count = 0;
                while (!blocks(nbor)) {
                    nbor = nbor.getRelative(BlockFace.DOWN);
                    count += 1;
                    if (count > 5) continue nborLoop;
                }
                if (!blocks(nbor.getRelative(BlockFace.UP, 1)) &&
                    !blocks(nbor.getRelative(BlockFace.UP, 2))) {
                    results.add(nbor.getRelative(BlockFace.UP));
                }
            }
        }
        if (results.isEmpty()) return null;
        Block resultBlock = results.get(0);
        for (Block block: results) {
            if (block.getY() < resultBlock.getY()) resultBlock = block;
        }
        Vector to = location.getBlock().getLocation().add(0.5, 0.5, 0.5).toVector();
        Vector from = resultBlock.getLocation().add(0.5, 1.5, 0.5).toVector();
        Location result = resultBlock.getLocation().add(0.5, 0.0, 0.5);
        result = result.setDirection(to.subtract(from));
        return result;
    }

    PlayerContext getPlayerContext(Player player) {
        PlayerContext result = contexts.get(player.getUniqueId());
        if (result == null) {
            result = new PlayerContext(player.getUniqueId());
            contexts.put(player.getUniqueId(), result);
        }
        return result;
    }

    static class Page {
        final List<List<Object>> lines = new ArrayList<>();
        static List<Page> pagesOf(List<List<Object>> lines) {
            List<Page> result = new ArrayList<>();
            int i = 0;
            Page page = new Page();
            for (List<Object> line: lines) {
                page.lines.add(line);
                i += 1;
                if (i == 5) {
                    result.add(page);
                    page = new Page();
                }
            }
            if (!page.lines.isEmpty()) result.add(page);
            return result;
        }
    }

    @RequiredArgsConstructor
    class PlayerContext {
        final UUID player;
        final List<Page> pages = new ArrayList<>();;
        final List<BlockLocation> locations = new ArrayList<>();
        void clear() {
            pages.clear();
            locations.clear();
        }
    }
}
