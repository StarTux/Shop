package com.winthier.shop;

import com.winthier.shop.util.Msg;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AdminCommand implements CommandExecutor {
    @RequiredArgsConstructor
    class MakePlotContext {
        final UUID uuid;
        BlockLocation a = null;
        void bump(Player player, Block block) {
            if (block == null) {
                Msg.info(player, "Now touch a corner block in the market world");
            } else if (a == null) {
                a = BlockLocation.of(block);
                Msg.info(player, "Now touch another corner block in the market world");
            } else {
                BlockLocation b = BlockLocation.of(block);
                Market.Plot plot = ShopPlugin.getInstance().getMarket().makePlot();
                plot.setWest(Math.min(a.getX(), b.getX()));
                plot.setEast(Math.max(a.getX(), b.getX()));
                plot.setNorth(Math.min(a.getZ(), b.getZ()));
                plot.setSouth(Math.max(a.getZ(), b.getZ()));
                plot.setOwner(null);
                if (ShopPlugin.getInstance().getMarket().collides(plot)) {
                    Msg.warn(player, "This plot would collide with another one.");
                } else {
                    ShopPlugin.getInstance().getMarket().getPlots().add(plot);
                    ShopPlugin.getInstance().getMarket().save();
                    Msg.info(player, "Plot created");
                }
                makePlotContexts.remove(uuid);
            }
        }
    }
    final Map<UUID, MakePlotContext> makePlotContexts = new HashMap<>();
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        final Player player = sender instanceof Player ? (Player)sender : null;
        if (args.length == 0) return false;
        String firstArg = args[0].toLowerCase();
        if (firstArg.equals("makeplot")) {
            if (player == null) return false;
            MakePlotContext mpc = new MakePlotContext(player.getUniqueId());
            makePlotContexts.put(player.getUniqueId(), mpc);
            mpc.bump(player, null);
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
                    player.sendBlockChange(new Location(world, (double)x, (double)y, (double)plot.getNorth()), mat, (byte)0);
                    player.sendBlockChange(new Location(world, (double)x, (double)y, (double)plot.getSouth()), mat, (byte)0);
                }
                for (int z = plot.getNorth(); z <= plot.getSouth(); ++z) {
                    player.sendBlockChange(new Location(world, (double)plot.getWest(), (double)y, (double)z), mat, (byte)0);
                    player.sendBlockChange(new Location(world, (double)plot.getEast(), (double)y, (double)z), mat, (byte)0);
                }
            }
            Msg.info(player, "%d plots highlighted", count);
        } else if (firstArg.equals("reload")) {
            ShopPlugin.getInstance().market = null;
            ShopPlugin.getInstance().chestDataStore = null;
            ShopPlugin.getInstance().reloadConfig();
            sender.sendMessage("Shop configuration reloaded");
        } else if (firstArg.equals("debug")) {
            for (BlockLocation loc: ShopPlugin.getInstance().getOfferScanner().dirties.keySet()) {
                player.sendMessage(""+loc);
            }
        }
        return true;
    }

    public boolean bump(Player player, Block block) {
        MakePlotContext mpc = makePlotContexts.get(player.getUniqueId());
        if (mpc != null) {
            mpc.bump(player, block);
            return true;
        }
        return false;
    }
}
