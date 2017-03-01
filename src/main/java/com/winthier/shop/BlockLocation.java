package com.winthier.shop;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Value;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

@Value
public final class BlockLocation {
    private String world;
    private int x, y, z;

    public static BlockLocation of(Block block) {
        return new BlockLocation(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    public static BlockLocation of(Location loc) {
        return of(loc.getBlock());
    }

    public Block getBlock() {
        World bWorld = Bukkit.getServer().getWorld(this.world);
        if (bWorld == null) return null;
        return bWorld.getBlockAt(x, y, z);
    }

    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("world", world);
        map.put("coordinates", Arrays.<Integer>asList(x, y, z));
        return map;
    }

    @SuppressWarnings("unchecked")
    public static BlockLocation deserialize(Map<String, Object> map) {
        String world = (String)map.get("world");
        List<Integer> coords = (List<Integer>)map.get("coordinates");
        return new BlockLocation(world, coords.get(0), coords.get(1), coords.get(2));
    }

    @Override
    public String toString() {
        return String.format("BlockLocation(%s,%d,%d,%d)", world, x, y, z);
    }
}
