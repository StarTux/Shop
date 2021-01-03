package com.winthier.shop;

import com.winthier.generic_events.GenericEvents;
import com.winthier.shop.util.Msg;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public final class AdminCommand implements CommandExecutor {
    private final ShopPlugin plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        final Player player = sender instanceof Player ? (Player) sender : null;
        if (args.length == 0) return false;
        String firstArg = args[0].toLowerCase();
        if (firstArg.equals("makeplot")) {
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
        } else if (firstArg.equals("plotinfo")) {
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
        } else if (firstArg.equals("deleteplot")) {
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
        } else if (firstArg.equals("showplots")) {
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
        } else if (firstArg.equals("reload")) {
            plugin.reloadConf();
            plugin.reloadMarket();
            plugin.reloadChests();
            sender.sendMessage("Shop configuration reloaded");
        } else if (firstArg.equals("debug")) {
            for (BlockLocation loc: plugin.getOfferScanner().getDirties().keySet()) {
                player.sendMessage("" + loc);
            }
        } else if (firstArg.equals("transfer") && args.length == 2) {
            Market.Plot plot = plugin.getMarket().plotAt(player.getLocation().getBlock());
            if (plot == null) {
                Msg.warn(player, "There is no plot here.");
                return true;
            }
            String name = args[1];
            UUID uuid = GenericEvents.cachedPlayerUuid(name);
            if (uuid == null) {
                Msg.warn(player, "Unknown player: " + name);
                return true;
            }
            Shopper oldOwner = plot.getOwner();
            name = GenericEvents.cachedPlayerName(uuid);
            plot.setOwner(new Shopper(uuid, name));
            plugin.getMarket().save();
            if (oldOwner == null) {
                Msg.info(player, "Plot transferred to " + name);
            } else {
                Msg.info(player, "Plot transferred: " + oldOwner.getName() + " => " + name);
            }
            return true;
        } else if (firstArg.equals("migrate")) {
            sender.sendMessage("Starting migration...");
            int count = plugin.getChestDataStore().migrate();
            sender.sendMessage(count + " chests migrated");
            return true;
        }
        return true;
    }
}
