package com.winthier.shop;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandWarn;
import com.winthier.playercache.PlayerCache;
import com.winthier.shop.sql.SQLChest;
import com.winthier.shop.sql.SQLOffer;
import com.winthier.shop.util.Cuboid;
import com.winthier.shop.util.WorldEdit;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class AdminCommand extends AbstractCommand<ShopPlugin> {
    protected AdminCommand(final ShopPlugin plugin) {
        super(plugin, "shopadmin");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("reload").denyTabCompletion()
            .description("Reload configs")
            .senderCaller(this::reload);
        rootNode.addChild("plotinfo").denyTabCompletion()
            .description("Display current plot info")
            .playerCaller(this::plotInfo);
        rootNode.addChild("plotstats").denyTabCompletion()
            .description("Print plot stats")
            .senderCaller(this::plotStats);
        rootNode.addChild("makeplot").denyTabCompletion()
            .description("Make a plot")
            .playerCaller(this::makePlot);
        rootNode.addChild("deleteplot").denyTabCompletion()
            .description("Delete a plot")
            .playerCaller(this::deletePlot);
        rootNode.addChild("showplots").denyTabCompletion()
            .description("Visualize all plots")
            .playerCaller(this::showPlots);
        rootNode.addChild("transfer").arguments("<player>")
            .description("Transfer plot to another player")
            .completers(CommandArgCompleter.NULL)
            .playerCaller(this::transfer);
        rootNode.addChild("flushoffers").denyTabCompletion()
            .description("Flush all offers (rescan)")
            .senderCaller(this::flushOffers);
        rootNode.addChild("debug").denyTabCompletion()
            .description("Print debug info")
            .senderCaller(this::debug);
        rootNode.addChild("transferall").arguments("<from> <to>")
            .description("Account transfer")
            .completers(PlayerCache.NAME_COMPLETER, PlayerCache.NAME_COMPLETER)
            .senderCaller(this::transferAll);
    }

    protected void reload(CommandSender sender) {
        plugin.reloadConf();
        plugin.getMarket().load();
        plugin.reloadChests();
        sender.sendMessage(text("Shop configuration reloaded", AQUA));
    }

    protected void plotInfo(Player player) {
        Plot plot = plugin.getMarket().plotAt(player.getLocation().getBlock());
        if (plot == null) {
            throw new CommandWarn("There is no plot here");
        }
        int id = plot.getRow().getId();
        if (plot.hasOwner()) {
            player.sendMessage(text("Plot #" + id + " belongs to " + plot.getOwnerName(), AQUA));
        } else {
            player.sendMessage(text("Plot #" + id + " without an owner", AQUA));
        }
    }

    protected void plotStats(CommandSender sender) {
        int owned = 0;
        int unowned = 0;
        int total = 0;
        for (Plot plot : plugin.getMarket().getPlots()) {
            total += 1;
            if (!plot.hasOwner()) {
                unowned += 1;
            } else {
                owned += 1;
            }
        }
        sender.sendMessage(text("Plot statistics", AQUA));
        sender.sendMessage(text("" + total + " Total", AQUA));
        sender.sendMessage(text("" + owned + " Owned", RED));
        sender.sendMessage(text("" + unowned + " Unowned", AQUA));
    }

    protected void makePlot(Player player) {
        Cuboid cuboid = WorldEdit.getSelection(player);
        if (cuboid == null) {
            throw new CommandWarn("Make a selection first");
        }
        Plot plot = plugin.getMarket().makePlot(cuboid);
        if (plugin.getMarket().collides(plot)) {
            throw new CommandWarn("This plot would collide with another one");
        }
        plugin.getMarket().addPlot(plot);
        player.sendMessage(text("Plot created", AQUA));
    }

    protected void deletePlot(Player player) {
        Plot plot = plugin.getMarket().plotAt(player.getLocation().getBlock());
        if (plot == null) {
            throw new CommandWarn("There is no plot here!");
        }
        plugin.getMarket().removePlot(plot);
        if (plot.getOwner() != null) {
            player.sendMessage(text("Plot of " + plot.getOwnerName() + " removed", AQUA));
        } else {
            player.sendMessage(text("Unowned plot removed", AQUA));
        }
    }

    protected void showPlots(Player player) {
        World world = player.getWorld();
        if (!world.getName().equals(plugin.getMarket().getWorld())) return;
        int y = player.getLocation().getBlockY();
        final Material mat = Material.GLOWSTONE;
        int count = 0;
        for (Plot plot : plugin.getMarket().getPlots()) {
            count += 1;
            for (int x = plot.getWest(); x <= plot.getEast(); ++x) {
                player.sendBlockChange(new Location(world, (double) x, (double) y, (double) plot.getNorth()), mat.createBlockData());
                player.sendBlockChange(new Location(world, (double) x, (double) y, (double) plot.getSouth()), mat.createBlockData());
            }
            for (int z = plot.getNorth(); z <= plot.getSouth(); ++z) {
                player.sendBlockChange(new Location(world, (double) plot.getWest(), (double) y, (double) z), mat.createBlockData());
                player.sendBlockChange(new Location(world, (double) plot.getEast(), (double) y, (double) z), mat.createBlockData());
            }
        }
        player.sendMessage(text(count + " plots highlighted", AQUA));
    }

    protected boolean transfer(Player player, String[] args) {
        if (args.length != 1) return false;
        Plot plot = plugin.getMarket().plotAt(player.getLocation().getBlock());
        if (plot == null) {
            throw new CommandWarn("There is no plot here!");
        }
        PlayerCache newOwner = PlayerCache.require(args[0]);
        PlayerCache oldOwner = plot.getOwner() != null
            ? PlayerCache.forUuid(plot.getOwner())
            : null;
        plot.setOwner(newOwner.uuid);
        if (oldOwner == null) {
            player.sendMessage(text("Plot transferred to " + newOwner.name, AQUA));
        } else {
            player.sendMessage(text("Plot transferred: " + oldOwner.name + " => " + newOwner.name, AQUA));
        }
        return true;
    }

    protected void flushOffers(CommandSender sender) {
        List<BlockLocation> bls = new ArrayList<>(SQLOffer.getCache().keySet());
        for (BlockLocation bl : bls) {
            plugin.getOfferScanner().setDirty(bl);
        }
        sender.sendMessage(bls.size() + " offers flushed!");
    }

    protected void debug(CommandSender sender) {
        for (BlockLocation loc : plugin.getOfferScanner().getDirties().keySet()) {
            sender.sendMessage("" + loc);
        }
    }

    private boolean transferAll(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        PlayerCache from = PlayerCache.forArg(args[0]);
        if (from == null) throw new CommandWarn("Player not found: " + args[0]);
        PlayerCache to = PlayerCache.forArg(args[1]);
        if (to == null) throw new CommandWarn("Player not found: " + args[1]);
        if (from.equals(to)) throw new CommandWarn("Players are identical: " + from.getName());
        final int chestCount = plugin.getDb().update(SQLChest.class)
            .set("owner", to.uuid)
            .where(c -> c.eq("owner", from.uuid))
            .sync();
        if (chestCount != 0) {
            plugin.reloadChests();
        }
        int plotCount = 0;
        for (Plot plot : plugin.getMarket().getPlots()) {
            if (plot.isOwner(from.uuid)) {
                plot.setOwner(to.uuid);
                plot.removeTrust(to.uuid);
            }
            if (plot.isTrusted(from.uuid)) {
                plot.removeTrust(from.uuid);
                plot.addTrust(to.uuid);
            }
            plotCount += 1;
        }
        if (chestCount == 0 && plotCount == 0) {
            throw new CommandWarn(from.name + " does not have any shops");
        }
        sender.sendMessage(text("Shops transferred from " + from.name + " to " + to.name + ":"
                                + " chests=" + chestCount
                                + " plots=" + plotCount,
                                AQUA));
        return true;
    }
}
