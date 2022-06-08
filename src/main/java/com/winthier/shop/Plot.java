package com.winthier.shop;

import com.winthier.playercache.PlayerCache;
import com.winthier.shop.sql.SQLPlot;
import com.winthier.shop.sql.SQLPlotTrust;
import com.winthier.shop.util.Cuboid;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

@Getter
public final class Plot {
    private final Market market;
    private final SQLPlot row;
    private final Set<UUID> trustedSet = new HashSet<>();

    /**
     * New plot constructor.
     */
    protected Plot(final Market market) {
        this.market = market;
        this.row = new SQLPlot();
        row.setWorld(market.getWorld());
        row.setCreationTime(new Date());
        row.setClaimTime(new Date());
    }

    protected Plot(final Market market, final Cuboid cuboid) {
        this.market = market;
        this.row = new SQLPlot();
        row.setWorld(market.getWorld());
        row.setWest(cuboid.a.x);
        row.setEast(cuboid.b.x);
        row.setNorth(cuboid.a.z);
        row.setSouth(cuboid.b.z);
        row.setCreationTime(new Date());
        row.setClaimTime(new Date());
    }

    /**
     * Loaded from database constructor.
     */
    protected Plot(final Market market, final SQLPlot row) {
        this.market = market;
        this.row = row;
    }

    public boolean isInside(int x, int y, int z) {
        return row.isInside(x, z)
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

    public boolean isOwner(UUID uuid) {
        return row.isOwner(uuid);
    }

    public boolean isOwner(Player player) {
        return row.isOwner(player.getUniqueId());
    }

    public boolean isTrusted(UUID uuid) {
        return trustedSet.contains(uuid);
    }

    public void addTrust(UUID uuid) {
        if (trustedSet.contains(uuid)) return;
        trustedSet.add(uuid);
        market.getPlugin().getDb().insertAsync(new SQLPlotTrust(row, uuid), null);
    }

    public void removeTrust(UUID uuid) {
        if (!trustedSet.remove(uuid)) return;
        market.getPlugin().getDb().find(SQLPlotTrust.class)
            .eq("plotId", row.getId())
            .eq("player", uuid)
            .deleteAsync(null);
    }

    public void removeAllTrust() {
        if (trustedSet.isEmpty()) return;
        market.getPlugin().getDb().find(SQLPlotTrust.class)
            .eq("plotId", row.getId())
            .in("player", Set.copyOf(trustedSet))
            .deleteAsync(null);
        trustedSet.clear();
    }

    public boolean isAllowed(UUID uuid) {
        return row.isOwner(uuid) || isTrusted(uuid);
    }

    public boolean isAllowed(Player player) {
        return isAllowed(player.getUniqueId());
    }

    public boolean collides(Plot other) {
        return row.collidesWith(other.row);
    }

    public String getOwnerName() {
        return hasOwner()
            ? PlayerCache.nameForUuid(getOwner())
            : "The Bank";
    }

    public UUID getOwner() {
        return row.getOwner();
    }

    public boolean hasOwner() {
        return row.getOwner() != null;
    }

    public void setOwner(UUID owner) {
        row.setOwner(owner);
        row.setClaimTime(new Date());
        market.getPlugin().getDb().updateAsync(row, Set.of("owner", "claimTime"), null);
    }

    public int getWest() {
        return row.getWest();
    }

    public int getNorth() {
        return row.getNorth();
    }

    public int getEast() {
        return row.getEast();
    }

    public int getSouth() {
        return row.getSouth();
    }

    public Location getSpawnLocation() {
        if (!row.isHasSpawn()) return null;
        return new Location(Bukkit.getWorld(market.getWorld()),
                            row.getSpawnX(), row.getSpawnY(), row.getSpawnZ(),
                            row.getSpawnYaw(), row.getSpawnPitch());
    }

    public void setSpawnLocation(Location location) {
        if (location == null) {
            row.setHasSpawn(false);
            market.getPlugin().getDb().updateAsync(row, Set.of("hasSpawn"), null);
            return;
        }
        row.setSpawnX(location.getX());
        row.setSpawnY(location.getY());
        row.setSpawnZ(location.getZ());
        row.setSpawnYaw(location.getYaw());
        row.setSpawnPitch(location.getPitch());
        row.setHasSpawn(true);
        market.getPlugin().getDb().updateAsync(row, Set.of("spawnX", "spawnY", "spawnZ",
                                                           "spawnYaw", "spawnPitch", "hasSpawn"), null);
    }
}
