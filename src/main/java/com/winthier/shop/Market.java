package com.winthier.shop;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

/**
 * Protection controls most global market protections. Even if
 * disabled, plots will still be mostly protected.
 */
@Getter @Setter
public final class Market {
    private String world;
    private int skyLimit;
    private int bottomLimit;
    private final List<Plot> plots = new ArrayList<>();
    private double plotPrice;
    private boolean protect;

    public Plot makePlot() {
        return new Plot(this);
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

    public boolean isAllowedAt(Player player, Block block) {
        if (block == null) return false;
        if (!isMarketWorld(block.getWorld())) return false;
        Plot plot = plotAt(block);
        if (plot == null) return false;
        return plot.isAllowed(player);
    }

    public Plot findPlayerPlot(UUID player) {
        for (Plot plot: plots) {
            if (plot.getOwner() != null && plot.getOwner().getUuid().equals(player)) {
                return plot;
            }
        }
        return null;
    }

    public Plot findPlayerPlotByName(String name) {
        for (Plot plot: plots) {
            if (plot.getOwner() != null && plot.getOwner().getName().equalsIgnoreCase(name)) {
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
        Collections.shuffle(emptyPlots);
        return emptyPlots.get(0);
    }

    public boolean collides(Plot plot) {
        for (Plot other: plots) {
            if (plot != other && plot.collides(other)) return true;
        }
        return false;
    }

    public void load() {
        plots.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(getSaveFile());
        world = yaml.getString("World", "Market");
        skyLimit = yaml.getInt("SkyLimit", 95);
        plotPrice = yaml.getDouble("PlotPrice", 2000.0);
        bottomLimit = yaml.getInt("BottomLimit", 1);
        protect = yaml.getBoolean("Protect", true);
        for (Map<?, ?> m: yaml.getMapList("plots")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) m;
            Plot plot = new Plot(this);
            plot.deserialize(map);
            plots.add(plot);
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("World", world);
        yaml.set("SkyLimit", skyLimit);
        yaml.set("BottomLimit", bottomLimit);
        yaml.set("PlotPrice", plotPrice);
        yaml.set("Protect", protect);
        List<Object> list = new ArrayList<>();
        for (Plot plot: plots) {
            list.add(plot.serialize());
        }
        yaml.set("plots", list);
        try {
            yaml.save(getSaveFile());
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    File getSaveFile() {
        ShopPlugin.getInstance().getDataFolder().mkdirs();
        return new File(ShopPlugin.getInstance().getDataFolder(), "market.yml");
    }

    boolean isWorld(World bworld) {
        return bworld.getName().equals(world);
    }
}
