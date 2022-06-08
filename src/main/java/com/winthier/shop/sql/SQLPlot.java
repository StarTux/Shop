package com.winthier.shop.sql;

import com.winthier.sql.SQLRow.Default;
import com.winthier.sql.SQLRow.Keyed;
import com.winthier.sql.SQLRow.Name;
import com.winthier.sql.SQLRow.NotNull;
import com.winthier.sql.SQLRow.Nullable;
import com.winthier.sql.SQLRow.VarChar;
import com.winthier.sql.SQLRow;
import java.util.Date;
import java.util.UUID;
import lombok.Data;

@Data @NotNull @Name("plots")
public final class SQLPlot implements SQLRow {
    @Id private Integer id;

    @VarChar(40) @Keyed
    private String world;
    private int west;
    private int east;
    private int north;
    private int south;

    @Nullable
    private UUID owner;

    @Default("0") private boolean hasSpawn;
    @Default("0") private double spawnX;
    @Default("0") private double spawnY;
    @Default("0") private double spawnZ;
    @Default("0") private float spawnPitch;
    @Default("0") private float spawnYaw;

    @Default("NOW()")
    private Date creationTime;
    @Default("NOW()")
    private Date claimTime;

    public SQLPlot() { }

    public boolean isOwner(UUID uuid) {
        return uuid.equals(owner);
    }

    public boolean isInside(int x, int z) {
        return x >= west
            && x <= east
            && z >= north
            && z <= south;
    }

    public boolean collidesWith(SQLPlot other) {
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
}
