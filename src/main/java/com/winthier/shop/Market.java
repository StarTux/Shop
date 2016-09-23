package com.winthier.shop;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

@Getter @Setter
public class Market {
    String world;
    int skyLimit, bottomLimit;
    final List<Plot> plots = new ArrayList<>();
    double plotPrice;

    @Getter @Setter
    public class Plot {
        int west, east, north, south;
        Shopper owner = null;
        final List<Shopper> trusted = new ArrayList<>();

        public boolean isInside(int x, int y, int z) {
            return
                x >= west &&
                x <= east &&
                z >= north &&
                z <= south &&
                y >= bottomLimit &&
                y <= skyLimit;
        }

        public Map<String, Object> serialize() {
            Map<String, Object> map = new HashMap<>();
            map.put("west", west);
            map.put("east", east);
            map.put("south", south);
            map.put("north", north);
            if (owner != null) map.put("owner", owner.serialize());
            List<Map<String, Object>> trusted = new ArrayList<>();
            for (Shopper shopper: this.trusted) trusted.add(shopper.serialize());
            map.put("trusted", trusted);
            return map;
        }

        public boolean isAllowed(Player player) {
            if (owner != null && owner.getUuid().equals(player.getUniqueId())) return true;
            for (Shopper shopper: trusted) {
                if (shopper.getUuid().equals(player.getUniqueId())) return true;
            }
            return false;
        }

        public boolean collides(Plot other) {
            boolean hor =
                (west >= other.west && west <= other.east) ||
                (east >= other.west && east <= other.east) ||
                (west < other.west && east > other.east);
            if (!hor) return false;
            boolean ver =
                (north >= other.north && north <= other.south) ||
                (south >= other.north && south <= other.south) ||
                (north < other.north && south > other.south);
            if (!ver) return false;
            return true;
        }
    }

    public Plot makePlot() {
        return new Plot();
    }

    @SuppressWarnings("unchecked")
    public Plot deserializePlot(Map<String, Object> map) {
        Plot plot = new Plot();
        plot.west = (Integer)map.get("west");
        plot.east = (Integer)map.get("east");
        plot.south = (Integer)map.get("south");
        plot.north = (Integer)map.get("north");
        if (map.containsKey("owner")) {
            plot.owner = Shopper.deserialize((Map<String, Object>)map.get("owner"));
        }
        List<Object> trusted = (List<Object>)map.get("trusted");
        for (Object o: trusted) plot.trusted.add(Shopper.deserialize((Map<String, Object>)o));
        return plot;
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

    public Plot findPlayerPlot(UUID player) {
        for (Plot plot: plots) {
            if (plot.owner != null && plot.owner.getUuid().equals(player)) {
                return plot;
            }
        }
        return null;
    }

    public Plot findPlayerPlotByName(String name) {
        for (Plot plot: plots) {
            if (plot.owner != null && plot.owner.getName().equalsIgnoreCase(name)) {
                return plot;
            }
        }
        return null;
    }

    public Plot randomEmptyPlot() {
        List<Plot> emptyPlots = new ArrayList<>();
        for (Plot plot: plots) {
            if (plot.owner == null) emptyPlots.add(plot);
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
        for (Map<?, ?> m: yaml.getMapList("plots")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>)m;
            plots.add(deserializePlot(map));
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("World", world);
        yaml.set("SkyLimit", skyLimit);
        yaml.set("BottomLimit", bottomLimit);
        yaml.set("PlotPrice", plotPrice);
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
}
