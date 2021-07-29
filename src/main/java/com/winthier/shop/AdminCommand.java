package com.winthier.shop;

import com.winthier.playercache.PlayerCache;
import com.winthier.shop.sql.SQLOffer;
import com.winthier.shop.util.Msg;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public final class AdminCommand implements TabExecutor {
    private final ShopPlugin plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        final Player player = sender instanceof Player ? (Player) sender : null;
        if (args.length == 0) return false;
        String firstArg = args[0].toLowerCase();
        switch (firstArg) {
        case "makeplot": {
            if (player == null) {
                sender.sendMessage("Player expected");
                return true;
            }
            int ax;
            int ay;
            int az;
            int bx;
            int by;
            int bz;
            try {
                ax = player.getMetadata("SelectionAX").get(0).asInt();
                ay = player.getMetadata("SelectionAY").get(0).asInt();
                az = player.getMetadata("SelectionAZ").get(0).asInt();
                bx = player.getMetadata("SelectionBX").get(0).asInt();
                by = player.getMetadata("SelectionBY").get(0).asInt();
                bz = player.getMetadata("SelectionBZ").get(0).asInt();
            } catch (Exception e) {
                player.sendMessage("Make a selection first.");
                return true;
            }
            Market.Plot plot = plugin.getMarket().makePlot();
            plot.setWest(Math.min(ax, bx));
            plot.setEast(Math.max(ax, bx));
            plot.setNorth(Math.min(az, bz));
            plot.setSouth(Math.max(az, bz));
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
        case "plotinfo": {
            Market.Plot plot = plugin.getMarket().plotAt(player.getLocation().getBlock());
            if (plot == null) {
                Msg.info(player, "There is no plot here.");
                return true;
            }
            if (plot.getOwner() != null) {
                Msg.info(player, "Plot belongs to %s.", plot.getOwner().getName());
            } else {
                Msg.info(player, "Plot without an owner.");
            }
            return true;
        }
        case "deleteplot": {
            Market.Plot plot = plugin.getMarket().plotAt(player.getLocation().getBlock());
            if (plot == null) {
                Msg.warn(player, "There is no plot here!");
                return true;
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
        case "showplots": {
            if (player == null) return false;
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
        case "reload": {
            plugin.reloadConf();
            plugin.reloadMarket();
            plugin.reloadChests();
            sender.sendMessage("Shop configuration reloaded");
            return true;
        }
        case "debug": {
            for (BlockLocation loc: plugin.getOfferScanner().getDirties().keySet()) {
                player.sendMessage("" + loc);
            }
            return true;
        }
        case "transfer": {
            if (args.length != 2) return false;
            Market.Plot plot = plugin.getMarket().plotAt(player.getLocation().getBlock());
            if (plot == null) {
                Msg.warn(player, "There is no plot here.");
                return true;
            }
            String name = args[1];
            UUID uuid = PlayerCache.uuidForName(name);
            if (uuid == null) {
                Msg.warn(player, "Unknown player: " + name);
                return true;
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
        case "flushoffers": {
            List<BlockLocation> bls = new ArrayList<>(SQLOffer.getCache().keySet());
            for (BlockLocation bl : bls) {
                plugin.getOfferScanner().setDirty(bl);
            }
            sender.sendMessage(bls.size() + " offers flushed!");
            return true;
        }
        default: return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) return null;
        String arg = args[args.length - 1];
        if (args.length == 1) {
            return Stream.of("makeplot", "plotinfo", "deleteplot",
            "showplots", "reload", "debug", "transfer", "migrate",
            "flushoffers")
                .filter(s -> s.contains(arg))
                .collect(Collectors.toList());
        }
        return null;
    }
}
