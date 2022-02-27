package com.winthier.shop;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandWarn;
import com.winthier.playercache.PlayerCache;
import com.winthier.shop.sql.SQLOffer;
import com.winthier.shop.util.Cuboid;
import com.winthier.shop.util.Msg;
import com.winthier.shop.util.WorldEdit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
    }

    protected boolean reload(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        plugin.reloadConf();
        plugin.reloadMarket();
        plugin.reloadChests();
        sender.sendMessage(text("Shop configuration reloaded", AQUA));
        return true;
    }

    protected boolean plotInfo(Player player, String[] args) {
        if (args.length != 0) return false;
        Market.Plot plot = plugin.getMarket().plotAt(player.getLocation().getBlock());
        if (plot == null) {
            throw new CommandWarn("There is no plot here.");
        }
        if (plot.getOwner() != null) {
            Msg.info(player, "Plot belongs to %s.", plot.getOwner().getName());
        } else {
            Msg.info(player, "Plot without an owner.");
        }
        return true;
    }

    protected boolean plotStats(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        int owned = 0;
        int unowned = 0;
        int total = 0;
        for (Market.Plot plot : plugin.getMarket().getPlots()) {
            total += 1;
            if (plot.getOwner() == null) {
                unowned += 1;
            } else {
                owned += 1;
            }
        }
        sender.sendMessage(text("Plot statistics", AQUA));
        sender.sendMessage(text("" + total + " Total", AQUA));
        sender.sendMessage(text("" + owned + " Owned", RED));
        sender.sendMessage(text("" + unowned + " Unowned", GREEN));
        return true;
    }

    protected boolean makePlot(Player player, String[] args) {
        if (args.length != 0) return false;
        Cuboid cuboid = WorldEdit.getSelection(player);
        if (cuboid == null) {
            throw new CommandWarn("Make a selection first");
        }
        Market.Plot plot = plugin.getMarket().makePlot();
        plot.setWest(cuboid.a.x);
        plot.setEast(cuboid.b.x);
        plot.setNorth(cuboid.a.z);
        plot.setSouth(cuboid.b.z);
        plot.setOwner(null);
        if (plugin.getMarket().collides(plot)) {
            Msg.warn(player, "This plot would collide with another one.");
        } else {
            plugin.getMarket().getPlots().add(plot);
            plugin.getMarket().save();
            Msg.info(player, "Plot created");
        }
        return true;
    }

    protected boolean deletePlot(Player player, String[] args) {
        if (args.length != 0) return false;
        Market.Plot plot = plugin.getMarket().plotAt(player.getLocation().getBlock());
        if (plot == null) {
            throw new CommandWarn("There is no plot here!");
        }
        plugin.getMarket().getPlots().remove(plot);
        plugin.getMarket().save();
        if (plot.getOwner() != null) {
            Msg.info(player, "Plot of %s removed.", plot.getOwner().getName());
        } else {
            Msg.info(player, "Unowned plot removed.");
        }
        return true;
    }

    protected boolean showPlots(Player player, String[] args) {
        if (args.length != 0) return false;
        World world = player.getWorld();
        if (!world.getName().equals(plugin.getMarket().getWorld())) return false;
        int y = player.getLocation().getBlockY();
        final Material mat = Material.GLOWSTONE;
        int count = 0;
        for (Market.Plot plot: plugin.getMarket().getPlots()) {
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
        Msg.info(player, "%d plots highlighted", count);
        return true;
    }

    protected boolean transfer(Player player, String[] args) {
        if (args.length != 1) return false;
        Market.Plot plot = plugin.getMarket().plotAt(player.getLocation().getBlock());
        if (plot == null) {
            throw new CommandWarn("There is no plot here!");
        }
        String name = args[0];
        UUID uuid = PlayerCache.uuidForName(name);
        if (uuid == null) {
            throw new CommandWarn("Unknown player: " + name);
        }
        Shopper oldOwner = plot.getOwner();
        name = PlayerCache.nameForUuid(uuid);
        plot.setOwner(new Shopper(uuid, name));
        plugin.getMarket().save();
        if (oldOwner == null) {
            Msg.info(player, "Plot transferred to " + name);
        } else {
            Msg.info(player, "Plot transferred: " + oldOwner.getName() + " => " + name);
        }
        return true;
    }

    protected boolean flushOffers(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        List<BlockLocation> bls = new ArrayList<>(SQLOffer.getCache().keySet());
        for (BlockLocation bl : bls) {
            plugin.getOfferScanner().setDirty(bl);
        }
        sender.sendMessage(bls.size() + " offers flushed!");
        return true;
    }

    protected boolean debug(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        for (BlockLocation loc : plugin.getOfferScanner().getDirties().keySet()) {
            sender.sendMessage("" + loc);
        }
        return true;
    }
}
