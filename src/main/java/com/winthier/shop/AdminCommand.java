package com.winthier.shop;

import com.winthier.shop.util.Msg;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class AdminCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        final Player player = sender instanceof Player ? (Player)sender : null;
        if (args.length == 0) return false;
        String firstArg = args[0].toLowerCase();
        if (firstArg.equals("makeplot")) {
            if (player == null) {
                sender.sendMessage("Player expected");
                return true;
            }
            int ax, ay, az, bx, by, bz;
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
            Market.Plot plot = ShopPlugin.getInstance().getMarket().makePlot();
            plot.setWest(Math.min(ax, bx));
            plot.setEast(Math.max(ax, bx));
            plot.setNorth(Math.min(az, bz));
            plot.setSouth(Math.max(az, bz));
            plot.setOwner(null);
            if (ShopPlugin.getInstance().getMarket().collides(plot)) {
                Msg.warn(player, "This plot would collide with another one.");
            } else {
                ShopPlugin.getInstance().getMarket().getPlots().add(plot);
                ShopPlugin.getInstance().getMarket().save();
                Msg.info(player, "Plot created");
            }
        } else if (firstArg.equals("plotinfo")) {
            Market.Plot plot = ShopPlugin.getInstance().getMarket().plotAt(player.getLocation().getBlock());
            if (plot == null) {
                Msg.info(player, "There is no plot here.");
                return true;
            }
            if (plot.getOwner() != null) {
                Msg.info(player, "Plot belongs to %s.", plot.getOwner().getName());
            } else {
                Msg.info(player, "Plot without an owner.");
            }
        } else if (firstArg.equals("deleteplot")) {
            Market.Plot plot = ShopPlugin.getInstance().getMarket().plotAt(player.getLocation().getBlock());
            if (plot == null) {
                Msg.warn(player, "There is no plot here!");
                return true;
            }
            ShopPlugin.getInstance().getMarket().getPlots().remove(plot);
            ShopPlugin.getInstance().getMarket().save();
            if (plot.getOwner() != null) {
                Msg.info(player, "Plot of %s removed.", plot.getOwner().getName());
            } else {
                Msg.info(player, "Unowned plot removed.");
            }
        } else if (firstArg.equals("showplots")) {
            if (player == null) return false;
            World world = player.getWorld();
            if (!world.getName().equals(ShopPlugin.getInstance().getMarket().getWorld())) return false;
            int y = player.getLocation().getBlockY();
            final Material mat = Material.GLOWSTONE;
            int count = 0;
            for (Market.Plot plot: ShopPlugin.getInstance().getMarket().getPlots()) {
                count += 1;
                for (int x = plot.getWest(); x <= plot.getEast(); ++x) {
                    player.sendBlockChange(new Location(world, (double)x, (double)y, (double)plot.getNorth()), mat.createBlockData());
                    player.sendBlockChange(new Location(world, (double)x, (double)y, (double)plot.getSouth()), mat.createBlockData());
                }
                for (int z = plot.getNorth(); z <= plot.getSouth(); ++z) {
                    player.sendBlockChange(new Location(world, (double)plot.getWest(), (double)y, (double)z), mat.createBlockData());
                    player.sendBlockChange(new Location(world, (double)plot.getEast(), (double)y, (double)z), mat.createBlockData());
                }
            }
            Msg.info(player, "%d plots highlighted", count);
        } else if (firstArg.equals("reload")) {
            ShopPlugin.getInstance().flush();
            ShopPlugin.getInstance().reloadConfig();
            sender.sendMessage("Shop configuration reloaded");
        } else if (firstArg.equals("debug")) {
            for (BlockLocation loc: ShopPlugin.getInstance().getOfferScanner().getDirties().keySet()) {
                player.sendMessage("" + loc);
            }
        }
        return true;
    }
}
