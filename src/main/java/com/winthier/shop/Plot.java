package com.winthier.shop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

@Getter @Setter @RequiredArgsConstructor
public final class Plot {
    private final Market market;
    private int west;
    private int east;
    private int north;
    private int south;
    private Shopper owner = null;
    private final List<Shopper> trusted = new ArrayList<>();
    private Location spawnLocation = null;

    public boolean isInside(int x, int y, int z) {
        return
            x >= west
            && x <= east
            && z >= north
            && z <= south
            && y >= market.getBottomLimit()
            && y <= market.getSkyLimit();
    }

    public boolean isWorld(World theWorld) {
        return market.getWorld().equals(theWorld.getName());
    }

    public boolean isInside(Block block) {
        return isWorld(block.getWorld()) && isInside(block.getX(), block.getY(), block.getZ());
    }

    public boolean isInside(Location l) {
        return isWorld(l.getWorld()) && isInside(l.getBlockX(), l.getBlockY(), l.getBlockZ());
    }

    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("west", west);
        map.put("east", east);
        map.put("south", south);
        map.put("north", north);
        if (owner != null) map.put("owner", owner.serialize());
        List<Map<String, Object>> trustedList = new ArrayList<>();
        for (Shopper shopper: this.trusted) trustedList.add(shopper.serialize());
        map.put("trusted", trustedList);
        if (spawnLocation != null) {
            Map<String, Object> section = new HashMap<>();
            section.put("x", spawnLocation.getX());
            section.put("y", spawnLocation.getY());
            section.put("z", spawnLocation.getZ());
            section.put("pitch", spawnLocation.getPitch());
            section.put("yaw", spawnLocation.getYaw());
            map.put("spawnLocation", section);
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    public void deserialize(Map<String, Object> map) {
        this.west = (Integer) map.get("west");
        this.east = (Integer) map.get("east");
        this.south = (Integer) map.get("south");
        this.north = (Integer) map.get("north");
        if (map.containsKey("owner")) {
            this.owner = Shopper.deserialize((Map<String, Object>) map.get("owner"));
        }
        List<Object> trustedList = (List<Object>) map.get("trusted");
        for (Object o : trustedList) {
            this.trusted.add(Shopper.deserialize((Map<String, Object>) o));
        }
        if (map.containsKey("spawnLocation")) {
            try {
                Map<String, Object> section = (Map<String, Object>) map.get("spawnLocation");
                double x = ((Number) section.get("x")).doubleValue();
                double y = ((Number) section.get("y")).doubleValue();
                double z = ((Number) section.get("z")).doubleValue();
                float yaw = ((Number) section.get("yaw")).floatValue();
                float pitch = ((Number) section.get("pitch")).floatValue();
                this.spawnLocation = new Location(Bukkit.getWorld(market.getWorld()), x, y, z, yaw, pitch);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
            (west >= other.west && west <= other.east)
            || (east >= other.west && east <= other.east)
            || (west < other.west && east > other.east);
        if (!hor) return false;
        boolean ver =
            (north >= other.north && north <= other.south)
            || (south >= other.north && south <= other.south)
            || (north < other.north && south > other.south);
        if (!ver) return false;
        return true;
    }

    public String getOwnerName() {
        return owner != null
            ? owner.getName()
            : "The Bank";
    }
}
