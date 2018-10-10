package com.winthier.shop;

import com.winthier.generic_events.GenericEvents;
import com.winthier.shop.sql.SQLLog;
import com.winthier.shop.sql.SQLOffer;
import com.winthier.shop.util.Msg;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class ShopCommand implements CommandExecutor {
    final Map<UUID, PlayerContext> contexts = new HashMap<>();
    final Comparator<SQLOffer> sqlOfferComparator = new Comparator<SQLOffer>() {
        @Override public int compare(SQLOffer a, SQLOffer b) {
            return Double.compare(a.pricePerItem(), b.pricePerItem());
        }
    };
    @Value static class DoneItem { private final UUID uuid; private final String name; }
    final Random random = new Random(System.currentTimeMillis());

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        final Player player = sender instanceof Player ? (Player)sender : null;
        if (player == null) return false;
        if (args.length == 0) {
            usage(player);
            return true;
        }
        String firstArg = args[0].toLowerCase();
        if ((firstArg.equals("search")
             || firstArg.equals("search!")
             || firstArg.equals("s")
             || firstArg.equals("s!"))) {
            if (!shopSearch(player, args)) {
                Msg.info(player, "/Shop Search &7Usage");
                Msg.send(player, "/Shop Search &7&o... &8-&r Search for items");
                Msg.send(player, "/Shop Search! &7&o... &8-&r Search for exact word combo");
                Msg.send(player, "/Shop Search -Sell &7&o... &8-&r Search for selling shops");
                Msg.send(player, "/Shop Search -Owner &7&o<Name> &8-&r Search by shop owner");
            }
        } else if (firstArg.equals("page")) {
            if (!shopPage(player, args)) {
                Msg.info(player, "/Shop Page &7Usage");
                Msg.send(player, "/Shop Page &7&o<Number> &8-&r View page of previous search");
            }
        } else if (firstArg.equals("port")) {
            shopPort(player, args);
        } else if (firstArg.equals("list")) {
            shopList(player, args);
        } else if (firstArg.equals("claim")) {
            if (!player.hasPermission("shop.market.claim")) {
                Msg.warn(player, "You don't have permission.");
                return true;
            }
            shopClaim(player, args);
        } else if (firstArg.equals("auto")) {
            if (!player.hasPermission("shop.market.claim")) {
                Msg.warn(player, "You don't have permission.");
                return true;
            }
            shopAuto(player, args);
        } else if (firstArg.equals("trust")) {
            if (!player.hasPermission("shop.market.claim")) {
                Msg.warn(player, "You don't have permission.");
                return true;
            }
            shopTrust(player, true, args);
        } else if (firstArg.equals("untrust")) {
            if (!player.hasPermission("shop.market.claim")) {
                Msg.warn(player, "You don't have permission.");
                return true;
            }
            shopTrust(player, false, args);
        } else if (firstArg.equals("setspawn")) {
            if (!player.hasPermission("shop.market.claim")) {
                Msg.warn(player, "You don't have permission.");
                return true;
            }
            Market.Plot plot = ShopPlugin.getInstance().getMarket().findPlayerPlot(player.getUniqueId());
            if (plot == null) {
                Msg.warn(player, "You don't have a market plot.");
                return true;
            }
            Location loc = player.getLocation();
            if (!plot.isInside(loc)) {
                Msg.warn(player, "The spawn location must be inside your plot.");
                return true;
            }
            plot.setSpawnLocation(loc);
            ShopPlugin.getInstance().getMarket().save();
            Msg.info(player, "Spawn location of your plot was set.");
        } else if (firstArg.equals("market")) {
            World world = Bukkit.getServer().getWorld(ShopPlugin.getInstance().getMarket().getWorld());
            if (world == null) return true;
            player.teleport(world.getSpawnLocation());
            Msg.info(player, "Teleported to the market.");
        } else {
            usage(player);
        }
        return true;
    }

    boolean shopSearch(Player player, String[] args) {
        if (args.length < 2) return false;
        boolean exact = args[0].endsWith("!");
        boolean searchOwner = false;
        String marketWorld = ShopPlugin.getInstance().getMarket().getWorld();
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
            } else if (arg.equalsIgnoreCase("-owner")) {
                searchOwner = true;
            } else {
                patterns.add(arg.toLowerCase());
            }
        }
        if (patterns.isEmpty()) return false;
        if (searchOwner && patterns.size() != 1) {
            Msg.warn(player, "Exactly one player name expected.");
            return true;
        }
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
            if (searchOwner) {
                String ownerName = offer.getOwnerName();
                if (ownerName != null && ownerName.equalsIgnoreCase(patterns.get(0))) {
                    offers.add(offer);
                }
            } else {
                String desc = offer.getItemDescription().toLowerCase();
                for (String pattern: patterns) {
                    if (exact) {
                        if (!desc.equals(pattern)) continue offerLoop;
                    } else {
                        if (!desc.contains(pattern)) continue offerLoop;
                    }
                }
                offers.add(offer);
            }
        }
        if (offers.isEmpty()) {
            Msg.warn(player, "Nothing found.");
            return true;
        }
        Collections.sort(offers, sqlOfferComparator);
        if (shopType == ShopType.SELL) Collections.reverse(offers);
        getPlayerContext(player).clear();
        int offerIndex = 0;
        List<List<Object>> lines = new ArrayList<>();
        Set<DoneItem> doneItems = new HashSet<>();
        for (SQLOffer offer: offers) {
            // Only one mention per player
            UUID owner = offer.getOwner();
            if (owner != null) {
                DoneItem doneItem = new DoneItem(owner, offer.getItemDescription());
                if (doneItems.contains(doneItem)) {
                    continue;
                } else {
                    doneItems.add(doneItem);
                }
            }
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
            json.add(Msg.format(" &8for&r "));
            json.add(Msg.button(ChatColor.BLUE, GenericEvents.formatMoney(offer.getPrice()), null, null));
            lines.add(json);
            getPlayerContext(player).locations.add(offer.getBlockLocation());
            offerIndex += 1;
        }
        getPlayerContext(player).pages.addAll(Page.pagesOf(lines));
        Msg.send(player, "&9&lShop Search");
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
        if (args.length == 1) {
            Market.Plot plot = ShopPlugin.getInstance().getMarket().findPlayerPlot(player.getUniqueId());
            if (plot == null) {
                Msg.warn(player, "You don't have a market plot.");
                return true;
            }
            portToPlot(player, plot);
            Msg.info(player, "Ported to your market plot.", plot.getOwner().getName());
        } else if (args.length == 2) {
            try {
                int portIndex = Integer.parseInt(args[1]);
                boolean res = portToShop(player, portIndex);
                if (res) {
                    Msg.info(player, "Ported to shop.");
                } else {
                    Msg.warn(player, "Could not port to shop.");
                }
            } catch (NumberFormatException nfe) {
                String nameArg = args[1];
                final Shopper owner = ShopPlugin.getInstance().findShopper(nameArg);
                if (owner == null) {
                    Msg.warn(player, "Market plot not found: %s.", nameArg);
                    return true;
                }
                Market.Plot plot = ShopPlugin.getInstance().getMarket().findPlayerPlot(owner.getUuid());
                if (plot == null) {
                    Msg.warn(player, "Market plot not found: %s.", owner.getName());
                    return true;
                }
                portToPlot(player, plot);
                Msg.info(player, "Ported to %s's market plot.", owner.getName());
            }
        } else {
            return false;
        }
        return true;
    }

    boolean shopList(Player player, String[] args) {
        List<List<Object>> lines = new ArrayList<>();
        for (SQLLog log: SQLLog.find(player.getUniqueId())) {
            List<Object> json = new ArrayList<>();
            json.add(" ");
            json.add(log.getCustomerName());
            if (log.getShopType() == ShopType.SELL) {
                json.add(Msg.format(" &8sold&r "));
            } else {
                json.add(Msg.format(" &8bought&r "));
            }
            json.add("" + log.getItemAmount());
            json.add(Msg.format("&8x"));
            json.add(log.getItemDescription());
            json.add(Msg.format(" &8for&r "));
            json.add(Msg.format("&9%s&r.", GenericEvents.formatMoney(log.getPrice())));
            lines.add(json);
        }
        if (lines.isEmpty()) {
            Msg.warn(player, "Nothing found.");
            return true;
        }
        getPlayerContext(player).clear();
        getPlayerContext(player).pages.addAll(Page.pagesOf(lines));
        Msg.send(player, "&9&lShop List");
        showPage(player, 0);
        return true;
    }

    boolean shopClaim(Player player, String[] args) {
        Block block = player.getLocation().getBlock();
        if (ShopPlugin.getInstance().getMarket().findPlayerPlot(player.getUniqueId()) != null) {
            Msg.warn(player, "You already have a plot.");
            return true;
        }
        Market.Plot plot = ShopPlugin.getInstance().getMarket().plotAt(block);
        if (plot == null) {
            Msg.warn(player, "There is no plot here.");
            return true;
        }
        if (plot.getOwner() != null) {
            Msg.warn(player, "This plot is already claimed by %s.", plot.getOwner().getName());
            return true;
        }
        Shopper shopper = Shopper.of(player);
        double price = ShopPlugin.getInstance().getMarket().getPlotPrice();
        String priceFormat = GenericEvents.formatMoney(price);
        if (args.length == 2 && args[1].equals("confirm")) {
            // Clicked confirm.
            if (!GenericEvents.takePlayerMoney(shopper.getUuid(), price, ShopPlugin.getInstance(), "Claim market plot")) {
                Msg.warn(player, "You can't afford the %s.", priceFormat);
            } else {
                plot.setOwner(Shopper.of(player));
                ShopPlugin.getInstance().getMarket().save();
                Msg.info(player, "You paid %s to claim this plot. Get to it via &a/Shop Port&r.", priceFormat);
                ShopPlugin.getInstance().getLogger().info(player.getName() + " claimed plot at " + plot.getNorth() + "," + plot.getWest());
            }
        } else {
            Msg.raw(player,
                    Msg.format("Claiming this plot costs &a%s&r: ", priceFormat),
                    Msg.button(ChatColor.GREEN, "&r[&aConfirm&r]", "&aConfirm this purchage", "/shop claim confirm"));
        }
        return true;
    }

    boolean shopAuto(Player player, String[] args) {
        if (ShopPlugin.getInstance().getMarket().findPlayerPlot(player.getUniqueId()) != null) {
            Msg.warn(player, "You already have a plot.");
            return true;
        }
        Market.Plot plot = ShopPlugin.getInstance().getMarket().randomEmptyPlot();
        if (plot == null) {
            Msg.warn(player, "All plots are occupied. Please make a ticket.");
            return true;
        }
        portToPlot(player, plot);
        Msg.raw(player,
                Msg.button(ChatColor.WHITE, "Teleported to an empty plot. Claim it via ", null, null),
                Msg.button(ChatColor.GREEN, "/shop claim", "/shop claim", "/shop claim "),
                Msg.button(ChatColor.WHITE, ".", null, null));
        return true;
    }

    boolean shopTrust(Player player, boolean trust, String[] args) {
        if (args.length > 2) return false;
        Market.Plot plot = ShopPlugin.getInstance().getMarket().findPlayerPlot(player.getUniqueId());
        if (plot == null) {
            Msg.warn(player, "You don't own a plot.");
            return true;
        }
        if (args.length == 1) {
            // Just list all trusted players.
            List<Object> json = new ArrayList<>();
            json.add(Msg.button(ChatColor.WHITE, "Trusted (" + plot.getTrusted().size() + "):", null, "/shop trust "));
            for (Shopper shopper: plot.getTrusted()) {
                json.add(" ");
                json.add(Msg.button(ChatColor.GREEN, shopper.getName(), null, "/shop untrust " + shopper.getName() + " "));
            }
            Msg.raw(player, json);
        } else if (args.length == 2) {
            String targetName = args[1];
            if (trust) {
                Shopper alreadyTrusted = null;
                for (Shopper shopper: plot.getTrusted()) {
                    if (shopper.getName().equalsIgnoreCase(targetName)) {
                        alreadyTrusted = shopper;
                        break;
                    }
                }
                if (alreadyTrusted != null) {
                    Msg.warn(player, "Player already trusted: %s.", alreadyTrusted.getName());
                } else {
                    Player target = Bukkit.getServer().getPlayer(targetName);
                    if (target == null) {
                        Msg.warn(player, "Player not online: %s.", targetName);
                    } else {
                        Shopper shopper = Shopper.of(target);
                        plot.getTrusted().add(shopper);
                        ShopPlugin.getInstance().getMarket().save();
                        Msg.info(player, "Trusted player in your plot: %s.", shopper.getName());
                    }
                }
            } else {
                if (targetName.equals("*")) {
                    plot.getTrusted().clear();
                    ShopPlugin.getInstance().getMarket().save();
                    Msg.info(player, "Trusted players cleared.");
                } else {
                    Shopper target = null;
                    for (Shopper shopper: plot.getTrusted()) {
                        if (shopper.getName().equalsIgnoreCase(targetName)) {
                            target = shopper;
                            break;
                        }
                    }
                    if (target == null) {
                        Msg.warn(player, "Player not trusted: %s.", targetName);
                    } else {
                        plot.getTrusted().remove(target);
                        ShopPlugin.getInstance().getMarket().save();
                        Msg.info(player, "Player untrusted: %s.", target.getName());
                    }
                }
            }
        }
        return true;
    }

    void showPage(Player player, int index) {
        int pageCount = getPlayerContext(player).pages.size();
        if (index < 0 || index >= pageCount) return;
        Page page = getPlayerContext(player).pages.get(index);
        Msg.info(player, "Page %d/%d", index + 1, pageCount);
        for (List<Object> json: page.lines) {
            Msg.raw(player, json);
        }
        if (index + 1 < pageCount) {
            Msg.raw(player,
                Msg.button(ChatColor.BLUE, "&r[&9More&r]", "Next page", "/shop page " + (index + 2))
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

    void portToPlot(Player player, Market.Plot plot) {
        Location loc;
        if (plot.getSpawnLocation() == null) {
            int x, z;
            if (random.nextBoolean()) {
                x = plot.getWest() + random.nextInt(plot.getEast() - plot.getWest() + 1);
                if (random.nextBoolean()) {
                    z = plot.getNorth() - 1;
                } else {
                    z = plot.getSouth() + 1;
                }
            } else {
                z = plot.getNorth() + random.nextInt(plot.getSouth() - plot.getNorth() + 1);
                if (random.nextBoolean()) {
                    x = plot.getWest() - 1;
                } else {
                    x = plot.getEast() + 1;
                }
            }
            World world = Bukkit.getServer().getWorld(ShopPlugin.getInstance().getMarket().getWorld());
            if (world == null) return;
            int y = world.getHighestBlockYAt(x, z);
            Block block = world.getBlockAt(x, y, z);
            loc = block.getLocation().add(0.5, 0.0, 0.5);
            block = world.getBlockAt((plot.getWest() + plot.getEast()) / 2, y, (plot.getNorth() + plot.getSouth()) / 2);
            Vector vec = block.getLocation().add(0.5, 0.0, 0.5).toVector();
            loc.setDirection(vec.subtract(loc.toVector()));
        } else {
            loc = plot.getSpawnLocation();
        }
        player.teleport(loc);
    }

    Location savePortLocation(BlockLocation location) {
        List<Block> nbors = new ArrayList<>();
        BlockData blockData = location.getBlock().getBlockData();
        BlockFace firstFace = BlockFace.NORTH;
        if (blockData instanceof org.bukkit.block.data.Directional) {
            firstFace = ((org.bukkit.block.data.Directional)blockData).getFacing();
        }
    faceLoop: for (BlockFace face: Arrays.<BlockFace>asList(firstFace, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST)) {
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
                while (blocks(nbor.getRelative(BlockFace.UP, 1))
                       || blocks(nbor.getRelative(BlockFace.UP, 2))) {
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
                if (!blocks(nbor.getRelative(BlockFace.UP, 1))
                    && !blocks(nbor.getRelative(BlockFace.UP, 2))) {
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

    void usage(Player player) {
        if (player == null) return;
        Msg.info(player, "/Shop &7Usage");
        Msg.raw(player, " ", Msg.button("&a/Shop Search ...", "Search for items", "/shop search "), Msg.format(" &8-&r Search for items"));
        Msg.raw(player, " ", Msg.button("&a/Shop List", "See who used your shop chests", "/shop list"), Msg.format(" &8-&r See who used your shop chests"));
        Msg.raw(player, " ", Msg.button("&a/Shop Port &7&o[Name]", "Port to a market plot", "/shop port "), Msg.format(" &8-&r Port to a market plot"));
        Msg.raw(player, " ", Msg.button("&a/Shop Market", "Teleport to the market", "/shop market"), Msg.format(" &8-&r Teleport to the market"));
        if (player.hasPermission("shop.market.claim")) {
            Msg.raw(player, " ", Msg.button("&a/Shop Auto", "Find an unclaimed market plot", "/shop auto"), Msg.format(" &8-&r Find an unclaimed market plot"));
            Msg.raw(player, " ", Msg.button("&a/Shop Claim", "Claim a market plot", "/shop claim"), Msg.format(" &8-&r Claim a market plot"));
            Msg.raw(player, " ", Msg.button("&a/Shop Trust", "Trust someone in your plot", "/shop trust "), Msg.format(" &8-&r Trust someone in your plot"));
            Msg.raw(player, " ", Msg.button("&a/Shop Untrust", "Untrust someone in your plot", "/shop untrust "), Msg.format(" &8-&r Untrust someone in your plot"));
            Msg.raw(player, " ", Msg.button("&a/Shop SetSpawn", "Set the spawn location of your plot", "/shop setspawn "), Msg.format(" &8-&r Set the spawn location of your plot"));
        }
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
                    i = 0;
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
