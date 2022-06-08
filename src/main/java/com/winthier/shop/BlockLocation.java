package com.winthier.shop;

import lombok.Value;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

@Value
public final class BlockLocation {
    private String world;
    private int x;
    private int y;
    private int z;

    public static BlockLocation of(Block block) {
        return new BlockLocation(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    public static BlockLocation of(Location loc) {
        return of(loc.getBlock());
    }

    public World getBukkitWorld() {
        return Bukkit.getServer().getWorld(world);
    }

    public Block getBlock() {
        World bWorld = Bukkit.getServer().getWorld(world);
        if (bWorld == null) return null;
        return bWorld.getBlockAt(x, y, z);
    }

    @Override
    public String toString() {
        return String.format("BlockLocation(%s,%d,%d,%d)", world, x, y, z);
    }
}
