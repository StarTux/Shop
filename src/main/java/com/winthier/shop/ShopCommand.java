package com.winthier.shop;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandNode;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.command.RemotePlayer;
import com.cavetale.core.connect.NetworkServer;
import com.cavetale.core.event.player.PluginPlayerEvent.Detail;
import com.cavetale.core.event.player.PluginPlayerEvent;
import com.cavetale.core.font.DefaultFont;
import com.cavetale.core.money.Money;
import com.cavetale.mytems.item.coin.Coin;
import com.winthier.playercache.PlayerCache;
import com.winthier.shop.sql.SQLLog;
import com.winthier.shop.sql.SQLOffer;
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
import java.util.function.Consumer;
import lombok.Value;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.JoinConfiguration.separator;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.ClickEvent.suggestCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

public final class ShopCommand extends AbstractCommand<ShopPlugin> {
    private static final String DOT = "\u00D7";
    private final Random random = new Random(System.currentTimeMillis());
    private static final UUID CONSOLE_UUID = new UUID(0L, 0L);
    private final Map<UUID, PlayerContext> contexts = new HashMap<>();
    private static final Comparator<SQLOffer> SQL_OFFER_COMPARATOR = new Comparator<SQLOffer>() {
        @Override public int compare(SQLOffer a, SQLOffer b) {
            return Double.compare(a.pricePerItem(), b.pricePerItem());
        }
    };

    protected ShopCommand(final ShopPlugin plugin) {
        super(plugin, "shop");
    }

    protected void onEnable() {
        final NetworkServer targetServer = plugin.getMasterServer();
        rootNode.addChild("search").arguments("[item]")
            .denyTabCompletion()
            .description("Search for shop chests")
            .remoteServer(targetServer)
            .senderCaller(this::search);
        rootNode.addChild("sell").arguments("[item]")
            .denyTabCompletion()
            .description("Search for sell chests")
            .remoteServer(targetServer)
            .senderCaller(this::sell);
        rootNode.addChild("page").arguments("<page>")
            .completers(CommandArgCompleter.integer(i -> i > 0))
            .description("View page of previous search")
            .remoteServer(targetServer)
            .senderCaller(this::page);
        rootNode.addChild("list").denyTabCompletion()
            .description("See who used your shop chests")
            .remoteServer(targetServer)
            .remotePlayerCaller(this::list);
        rootNode.addChild("port").arguments("[name]")
            .description("Port to a market plot")
            .completers(CommandArgCompleter.NULL)
            .remoteServer(targetServer)
            .remotePlayerCaller(this::port);
        rootNode.addChild("market").denyTabCompletion()
            .description("Teleport to the market")
            .remoteServer(targetServer)
            .remotePlayerCaller(this::market);
        rootNode.addChild("auto").denyTabCompletion()
            .description("Find an unclaimed market plot")
            .permission("shop.market.claim")
            .remoteServer(targetServer)
            .remotePlayerCaller(this::auto);
        rootNode.addChild("claim").denyTabCompletion()
            .description("Claim a market plot")
            .permission("shop.market.claim")
            .playerCaller(this::claim);
        rootNode.addChild("trust").arguments("<player>")
            .completers(PlayerCache.NAME_COMPLETER)
            .description("Trust someone in your plot")
            .permission("shop.market.claim")
            .playerCaller(this::trust);
        rootNode.addChild("untrust").arguments("<player>")
            .completers(PlayerCache.NAME_COMPLETER)
            .description("Untrust someone in your plot")
            .permission("shop.market.claim")
            .playerCaller(this::untrust);
        rootNode.addChild("setspawn").denyTabCompletion()
            .description("Untrust someone in your plot")
            .permission("shop.market.claim")
            .playerCaller(this::setspawn);
    }

    private boolean search(CommandSender sender, String[] args) {
        if (args.length == 0) return false;
        return search(sender, ShopType.BUY, String.join(" ", args));
    }

    private boolean sell(CommandSender sender, String[] args) {
        if (args.length == 0) return false;
        return search(sender, ShopType.SELL, String.join(" ", args));
    }

    private boolean search(CommandSender sender, final ShopType shopType, final String term) {
        final boolean exact = term.startsWith("\"") && term.endsWith("\"");
        List<String> patterns = exact
            ? List.of(term.substring(1, term.length() - 1))
            : List.of(term.split(" "));
        plugin.getDb().find(SQLOffer.class)
            .eq("world", plugin.getMarket().getWorld())
            .eq("shopType", shopType.ordinal())
            .findListAsync(list -> CommandNode.wrap(sender, () -> searchCallback(sender, shopType, patterns, list)));
        return true;
    }

    private void searchCallback(CommandSender sender, ShopType shopType, List<String> patterns, List<SQLOffer> rows) {
        List<SQLOffer> offers = new ArrayList<>();
        String marketWorld = plugin.getMarket().getWorld();
        OFFERS: for (SQLOffer offer : rows) {
            if (!offer.getWorld().equals(marketWorld)) continue;
            if (offer.getShopType() != shopType) continue;
            String desc = offer.getItemDescription().toLowerCase();
            for (String pattern : patterns) {
                if (!desc.contains(pattern)) continue OFFERS;
            }
            offers.add(offer);
        }
        if (offers.isEmpty()) {
            throw new CommandWarn("Nothing found");
        }
        offers.sort(SQL_OFFER_COMPARATOR);
        if (shopType == ShopType.SELL) Collections.reverse(offers);
        getPlayerContext(sender).clear();
        int offerIndex = 0;
        Set<DoneItem> doneItems = new HashSet<>();
        List<Component> lines = new ArrayList<>();
        for (SQLOffer offer : offers) {
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
            TextComponent.Builder cb = text()
                .hoverEvent(showText(text("Port to " + offer.getOwnerName() + "'s shop chest", GRAY)))
                .clickEvent(runCommand("/shop port " + offerIndex));
            cb.append(text("[Port]", BLUE));
            cb.append(space());
            cb.append(Coin.format(offer.getPrice()));
            cb.append(space());
            cb.append(offer.parseItemDisplayName());
            cb.append(space());
            cb.append(text("by", DARK_GRAY));
            cb.append(space());
            cb.append(text(offer.getOwnerName(), WHITE));
            lines.add(cb.build());
            getPlayerContext(sender).locations.add(offer.getBlockLocation());
            getPlayerContext(sender).searchType = shopType;
            offerIndex += 1;
        }
        getPlayerContext(sender).pages.addAll(pagesOf(lines));
        sender.sendMessage(text("Shop Search", BLUE, BOLD));
        showPage(sender, 0);
        if (sender instanceof Player player) {
            PluginPlayerEvent.Name.SHOP_SEARCH.call(plugin, player);
        } else if (sender instanceof RemotePlayer player) {
            PluginPlayerEvent.Name.SHOP_SEARCH.broadcast(plugin, player.getUniqueId());
        }
    }

    private boolean page(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        int page = CommandArgCompleter.requireInt(args[0], i -> i > 0);
        showPage(sender, page - 1);
        return true;
    }

    private void showPage(CommandSender sender, int index) {
        int pageCount = getPlayerContext(sender).pages.size();
        if (index < 0 || index >= pageCount) return;
        PlayerContext context = getPlayerContext(sender);
        if (context == null) return;
        Component page = context.pages.get(index);
        if (context.searchType == ShopType.SELL) {
            sender.sendMessage(text("Sell Page " + (index + 1) + "/" + pageCount, GREEN));
        } else {
            sender.sendMessage(text("Shop Page " + (index + 1) + "/" + pageCount, GREEN));
        }
        sender.sendMessage(page);
        if (index + 1 < pageCount) {
            sender.sendMessage(text("[More]", BLUE)
                               .hoverEvent(showText(text("Next Page", GRAY)))
                               .clickEvent(runCommand("/shop page " + (index + 2))));
        }
    }

    private boolean port(RemotePlayer player, String[] args) {
        if (args.length > 1) return false;
        if (args.length == 0) {
            Plot plot = plugin.getMarket().findPlayerPlot(player.getUniqueId());
            if (plot == null) {
                throw new CommandWarn("You do not have a market plot");
            }
            boolean res = portToPlot(player, plot, player2 -> {
                    if (player2 == null) return;
                    player.sendMessage(text("Ported to your market plot", GREEN));
                });
            if (!res) {
                throw new CommandWarn("Could not port to your plot");
            }
            return true;
        }
        try {
            int portIndex = Integer.parseInt(args[0]);
            boolean res = portToShop(player, portIndex, player2 -> {
                    if (player2 == null) return;
                    player2.sendMessage(text("Ported to shop", GREEN));
                    PluginPlayerEvent.Name.SHOP_SEARCH_PORT.call(plugin, player2);
                });
            if (!res) {
                throw new CommandWarn("Could not port to shop");
            }
            return true;
        } catch (NumberFormatException nfe) { }
        PlayerCache target = PlayerCache.require(args[0]);
        Plot plot = plugin.getMarket().findPlayerPlot(target.uuid);
        if (plot == null) {
            throw new CommandWarn("Market plot not found: " + target.name);
        }
        boolean res = portToPlot(player, plot, player2 -> {
                if (player2 == null) return;
                player2.sendMessage(text("Ported to " + plot.getOwnerName() + "'s market plot", GREEN));
            });
        if (!res) {
            throw new CommandWarn("Could not port to " + plot.getOwnerName() + "'s market plot");
        }
        return true;
    }

    protected void market(RemotePlayer player) {
        World world = Bukkit.getServer().getWorld(plugin.getMarket().getWorld());
        if (world == null) {
            throw new CommandWarn("Market not found");
        }
        player.bring(plugin, world.getSpawnLocation(), player2 -> {
                if (player2 == null) return;
                player2.sendMessage(text("Teleported to the market", GREEN));
                PluginPlayerEvent.Name.USE_WARP.make(plugin, player2)
                    .detail(Detail.NAME, "market")
                    .callEvent();
            });
    }

    private void list(RemotePlayer player) {
        SQLLog.find(player.getUniqueId(), logs -> printShopList(player, logs));
    }

    private void printShopList(RemotePlayer player, List<SQLLog> logs) {
        List<Component> lines = new ArrayList<>();
        logs.removeIf(log -> log.getPrice() == 0.0);
        for (SQLLog log : logs) {
            TextComponent.Builder cb = text();
            cb.append(space());
            cb.append(text(log.getCustomerName(), WHITE));
            cb.append(space());
            if (log.getShopType() == ShopType.SELL) {
                cb.append(text("sold", DARK_GRAY));
            } else {
                cb.append(text("bought", DARK_GRAY));
            }
            cb.append(space());
            cb.append(text("" + log.getItemAmount(), WHITE));
            cb.append(text(DOT, DARK_GRAY));
            cb.append(text(log.getItemDescription(), WHITE));
            cb.append(space());
            cb.append(text("for", DARK_GRAY));
            cb.append(space());
            cb.append(Coin.format(log.getPrice()));
            lines.add(cb.build());
        }
        if (lines.isEmpty()) {
            throw new CommandWarn("Nothing found");
        }
        getPlayerContext(player).clear();
        getPlayerContext(player).pages.addAll(pagesOf(lines));
        player.sendMessage(text("Shop List", BLUE, BOLD));
        showPage(player, 0);
    }

    private void auto(RemotePlayer player) {
        if (!player.hasPermission("shop.market.override")
            && plugin.getMarket().findPlayerPlot(player.getUniqueId()) != null) {
            throw new CommandWarn("You already have a plot");
        }
        Plot plot = plugin.getMarket().randomEmptyPlot();
        if (plot == null) {
            throw new CommandWarn("All plots are occupied. Please make a ticket");
        }
        boolean res = portToPlot(player, plot, player2 -> {
                player2.sendMessage(join(noSeparators(),
                                         text("Teleported to an empty plot. Claim it via "),
                                         (text("/shop claim", GREEN)
                                          .hoverEvent(showText(text("/shop claim", GREEN)))
                                          .clickEvent(runCommand("/shop claim")))));
            });
        if (!res) {
            throw new CommandWarn("Could not warp you to an empty plot");
        }
    }

    private boolean claim(Player player, String[] args) {
        if (args.length > 1) return false;
        Block block = player.getLocation().getBlock();
        if (plugin.getMarket().findPlayerPlot(player.getUniqueId()) != null) {
            throw new CommandWarn("You already have a plot");
        }
        Plot plot = plugin.getMarket().plotAt(block);
        if (plot == null) {
            throw new CommandWarn("There is no plot here");
        }
        if (plot.getOwner() != null) {
            throw new CommandWarn("This plot is already claimed by " + plot.getOwnerName());
        }
        double price = plugin.getMarket().getPlotPrice();
        if (args.length == 1 && args[0].equals("confirm")) {
            // Clicked confirm.
            if (!Money.get().take(player.getUniqueId(), price, plugin, "Claim market plot")) {
                throw new CommandWarn("You cannot afford the " + Money.get().format(price));
            } else {
                plot.setOwner(player.getUniqueId());
                player.sendMessage(join(noSeparators(),
                                        text("You paid "),
                                        Coin.format(price),
                                        text(" to claim this plot. Get to it via "),
                                        (text("/shop port", GREEN)
                                         .hoverEvent(showText(text("/shop port", GREEN)))
                                         .clickEvent(runCommand("/shop port")))));
                plugin.getLogger().info(player.getName() + " claimed plot at " + plot.getNorth() + "," + plot.getWest());
            }
            return true;
        } else if (args.length == 0) {
            player.sendMessage(join(noSeparators(),
                                    text("Claiming this plot costs "),
                                    Coin.format(price),
                                    space(),
                                    (DefaultFont.ACCEPT_BUTTON.component
                                     .hoverEvent(showText(text("Confirm this purchase", GREEN)))
                                     .clickEvent(runCommand("/shop claim confirm")))));
            return true;
        } else {
            return false;
        }
    }

    private boolean trust(Player player, String[] args) {
        return trustEdit(player, true, args);
    }

    private boolean untrust(Player player, String[] args) {
        return trustEdit(player, false, args);
    }

    private boolean trustEdit(Player player, boolean trust, String[] args) {
        if (args.length > 1) return false;
        Plot plot = plugin.getMarket().findPlayerPlot(player.getUniqueId());
        if (plot == null) {
            throw new CommandWarn("You do not own a plot");
        }
        if (args.length == 0) {
            if (plot.getTrustedSet().isEmpty()) {
                throw new CommandWarn("Nobody is trusted in your plot");
            }
            List<PlayerCache> caches = new ArrayList<>(plot.getTrustedSet().size());
            for (UUID uuid : plot.getTrustedSet()) {
                caches.add(PlayerCache.forUuid(uuid));
            }
            caches.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.name, b.name));
            List<Component> components = new ArrayList<>();
            components.add(text("Trusted (" + caches.size() + ")", GREEN));
            for (PlayerCache cache : caches) {
                String cmd = "/shop untrust " + cache.name;
                components.add(text(cache.name)
                               .hoverEvent(showText(text(cmd, GREEN)))
                               .clickEvent(suggestCommand(cmd)));
            }
            player.sendMessage(join(separator(space()), components));
            return true;
        }
        if (trust) {
            PlayerCache target = PlayerCache.requireForName(args[0]);
            if (plot.isTrusted(target.uuid)) {
                throw new CommandWarn("Player already trusted: " + target.name);
            } else {
                plot.addTrust(target.uuid);
                player.sendMessage(text("Trusted player in your plot: " + target.name, GREEN));
            }
        } else {
            if (args[0].equals("*")) {
                plot.removeAllTrust();
                player.sendMessage(text("Trusted players cleared", GREEN));
                return true;
            }
            PlayerCache target = PlayerCache.requireForName(args[0]);
            if (!plot.isTrusted(target.uuid)) {
                throw new CommandWarn("Player not trusted: " + target.name);
            }
            plot.removeTrust(target.uuid);
            player.sendMessage(text("Player untrusted: " + target.name, GREEN));
        }
        return true;
    }

    private void setspawn(Player player) {
        Plot plot = plugin.getMarket().findPlayerPlot(player.getUniqueId());
        if (plot == null) {
            throw new CommandWarn("You do not have a market plot");
        }
        Location loc = player.getLocation();
        if (!plot.isInside(loc)) {
            throw new CommandWarn("The spawn location must be inside your plot");
        }
        plot.setSpawnLocation(loc);
        player.sendMessage(text("Spawn location of your plot was set", GREEN));
    }

    private boolean portToShop(RemotePlayer player, int index, Consumer<Player> callback) {
        if (index < 0 || index >= getPlayerContext(player).locations.size()) return false;
        Location loc = savePortLocation(getPlayerContext(player).locations.get(index));
        if (loc == null) return false;
        player.bring(plugin, loc, callback);
        return true;
    }

    private boolean portToPlot(RemotePlayer player, Plot plot, Consumer<Player> callback) {
        Location loc;
        if (plot.getSpawnLocation() == null) {
            int x;
            int z;
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
            World world = Bukkit.getServer().getWorld(plugin.getMarket().getWorld());
            if (world == null) return false;
            int y = world.getHighestBlockYAt(x, z);
            Block block = world.getBlockAt(x, y, z);
            while (!block.isEmpty() && block.getY() < world.getMaxHeight()) block = block.getRelative(0, 1, 0);
            loc = block.getLocation().add(0.5, 0.0, 0.5);
            block = world.getBlockAt((plot.getWest() + plot.getEast()) / 2, y, (plot.getNorth() + plot.getSouth()) / 2);
            Vector vec = block.getLocation().add(0.5, 0.0, 0.5).toVector();
            loc.setDirection(vec.subtract(loc.toVector()));
        } else {
            loc = plot.getSpawnLocation();
        }
        player.bring(plugin, loc, callback);
        return true;
    }

    private Location savePortLocation(BlockLocation location) {
        List<Block> nbors = new ArrayList<>();
        BlockData blockData = location.getBlock().getBlockData();
        BlockFace firstFace = BlockFace.NORTH;
        if (blockData instanceof org.bukkit.block.data.Directional) {
            firstFace = ((org.bukkit.block.data.Directional) blockData).getFacing();
        }
        FACES: for (BlockFace face: Arrays.<BlockFace>asList(firstFace, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST)) {
            Block block = location.getBlock().getRelative(face);
            int count = 0;
            while (block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST) {
                block = block.getRelative(face);
                count += 1;
                if (count > 5) continue FACES;
            }
            nbors.add(block);
        }
        if (nbors.isEmpty()) return null;
        List<Block> results = new ArrayList<>();
        NBORS: for (Block nbor: nbors) {
            if (nbor.isSolid()) {
                int count = 0;
                while (!nbor.getRelative(BlockFace.UP, 1).isEmpty() || !nbor.getRelative(BlockFace.UP, 2).isEmpty()) {
                    nbor = nbor.getRelative(BlockFace.UP);
                    count += 1;
                    if (count > 5) continue NBORS;
                }
                results.add(nbor.getRelative(BlockFace.UP));
            } else {
                int count = 0;
                while (!nbor.isSolid()) {
                    nbor = nbor.getRelative(BlockFace.DOWN);
                    count += 1;
                    if (count > 5) continue NBORS;
                }
                if (nbor.getRelative(BlockFace.UP, 1).isEmpty() && nbor.getRelative(BlockFace.UP, 2).isEmpty()) {
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

    private PlayerContext getPlayerContext(CommandSender sender) {
        final UUID uuid;
        if (sender instanceof ConsoleCommandSender) {
            uuid = CONSOLE_UUID;
        } else if (sender instanceof Player player) {
            uuid = player.getUniqueId();
        } else if (sender instanceof RemotePlayer player) {
            uuid = player.getUniqueId();
        } else {
            throw new IllegalArgumentException(sender.getClass().getName());
        }
        PlayerContext result = contexts.get(uuid);
        if (result == null) {
            result = new PlayerContext(uuid);
            contexts.put(uuid, result);
        }
        return result;
    }

    private static List<Component> pagesOf(List<Component> lines) {
        List<Component> result = new ArrayList<>();
        int i = 0;
        List<Component> page = new ArrayList<>();
        for (Component line : lines) {
            page.add(line);
            i += 1;
            if (i >= 9) {
                result.add(join(separator(newline()), page));
                page.clear();
                i = 0;
            }
        }
        if (!page.isEmpty()) {
            result.add(join(separator(newline()), page));
        }
        return result;
    }
}

@Value
class DoneItem {
    private final UUID uuid;
    private final String name;
}
