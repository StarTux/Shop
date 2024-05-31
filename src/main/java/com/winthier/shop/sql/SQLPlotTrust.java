package com.winthier.shop.sql;

import com.winthier.sql.SQLRow;
import com.winthier.sql.SQLRow.Default;
import com.winthier.sql.SQLRow.Name;
import com.winthier.sql.SQLRow.NotNull;
import com.winthier.sql.SQLRow.UniqueKey;
import java.util.Date;
import java.util.UUID;
import lombok.Data;

@Data @NotNull @Name("plot_trust")
@UniqueKey({"plotId", "player"})
public final class SQLPlotTrust implements SQLRow {
    @Id private Integer id;

    private int plotId;
    private UUID player;

    @Default("NOW()")
    private Date creationTime;

    public SQLPlotTrust() { }

    public SQLPlotTrust(final SQLPlot plot, final UUID player) {
        this.plotId = plot.getId();
        this.player = player;
        this.creationTime = new Date();
    }
}
