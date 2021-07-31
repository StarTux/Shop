package com.winthier.shop.util;

import lombok.Value;
import org.bukkit.Location;

@Value
public final class Cuboid {
    public static final Cuboid ZERO = new Cuboid(Vec3i.ZERO, Vec3i.ZERO);
    public final Vec3i a;
    public final Vec3i b;

    public boolean contains(int x, int y, int z) {
        return x >= a.x
            && y >= a.y
            && z >= a.z
            && x <= b.x
            && y <= b.y
            && z <= b.z;
    }

    public boolean contains(Location location) {
        return contains(location.getBlockX(),
                        location.getBlockY(),
                        location.getBlockZ());
    }

    @Override
    public String toString() {
        return a + "-" + b;
    }
}
