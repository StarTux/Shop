package com.winthier.shop;

import com.cavetale.core.struct.Cuboid;
import com.winthier.shop.sql.SQLPlot;
import com.winthier.shop.sql.SQLPlotTrust;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

/**
 * Protection controls most global market protections. Even if
 * disabled, plots will still be mostly protected.
 */
@RequiredArgsConstructor
public final class Market {
    @Getter private final ShopPlugin plugin;
    @Getter private String world;
    @Getter private int skyLimit;
    @Getter private int bottomLimit;
    @Getter private final List<Plot> plots = new ArrayList<>();
    @Getter private double plotPrice;
    @Getter private boolean protect;

    /**
     * Create a new plot but do not add it to the list of plots, nor
     * save it to the database.
     */
    public Plot makePlot(Cuboid cuboid) {
        return new Plot(this, cuboid);
    }

    public Plot plotAt(Block block) {
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();
        for (Plot plot: plots) {
            if (plot.isInside(x, y, z)) return plot;
        }
        return null;
    }

    public boolean isMarketWorld(World bukkitWorld) {
        return Objects.equals((String) bukkitWorld.getName(), (String) this.world);
    }

    public void addPlot(Plot plot) {
        if (plot.getRow().getId() != null) {
            throw new IllegalStateException("Attempting to add plot with id: " + plot.getRow());
        }
        plugin.getDb().insert(plot.getRow());
        plots.add(plot);
        // One time migration only:
        for (UUID uuid : plot.getTrustedSet()) {
            plugin.getDb().insert(new SQLPlotTrust(plot.getRow(), uuid));
        }
    }

    public void removePlot(Plot plot) {
        if (plot.getRow().getId() == null || !plots.remove(plot)) {
            throw new IllegalStateException("Attempting to remove unknown plot: " + plot.getRow());
        }
        int plotId = plot.getRow().getId();
        plugin.getDb().delete(plot.getRow());
        plugin.getDb().find(SQLPlotTrust.class).eq("plotId", plotId).delete();
    }

    public boolean isAllowedAt(Player player, Block block) {
        if (block == null) return false;
        if (!isMarketWorld(block.getWorld())) return false;
        Plot plot = plotAt(block);
        if (plot == null) return false;
        return plot.isAllowed(player);
    }

    public Plot findPlayerPlot(UUID uuid) {
        for (Plot plot : plots) {
            if (plot.isOwner(uuid)) {
                return plot;
            }
        }
        return null;
    }

    public Plot randomEmptyPlot() {
        List<Plot> emptyPlots = new ArrayList<>();
        for (Plot plot: plots) {
            if (plot.getOwner() == null) emptyPlots.add(plot);
        }
        if (emptyPlots.isEmpty()) return null;
        return emptyPlots.get(ThreadLocalRandom.current().nextInt(emptyPlots.size()));
    }

    public boolean collides(Plot plot) {
        for (Plot other: plots) {
            if (plot != other && plot.collides(other)) return true;
        }
        return false;
    }

    public void load() {
        File saveFile = new File(plugin.getDataFolder(), "market.yml");
        if (!saveFile.exists()) {
            plugin.getDataFolder().mkdirs();
            plugin.saveResource("market.yml", false);
        }
        plots.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(saveFile);
        world = yaml.getString("World", "market");
        skyLimit = yaml.getInt("SkyLimit", 95);
        plotPrice = yaml.getDouble("PlotPrice", 10000.0);
        bottomLimit = yaml.getInt("BottomLimit", 1);
        protect = yaml.getBoolean("Protect", true);
        Map<Integer, Plot> plotIdMap = new TreeMap<>();
        for (SQLPlot row : plugin.getDb().find(SQLPlot.class).eq("world", world).findList()) {
            Plot plot = new Plot(this, row);
            plots.add(plot);
            plotIdMap.put(plot.getRow().getId(), plot);
        }
        if (!plotIdMap.isEmpty()) {
            for (SQLPlotTrust row : plugin.getDb().find(SQLPlotTrust.class).findList()) {
                Plot plot = plotIdMap.get(row.getPlotId());
                if (plot == null) continue;
                plot.getTrustedSet().add(row.getPlayer());
            }
        }
    }

    public List<String> getPlotOwnerNames() {
        List<String> result = new ArrayList<>();
        for (Plot plot : plots) {
            if (plot.hasOwner()) {
                result.add(plot.getOwnerName());
            }
        }
        return result;
    }
}
