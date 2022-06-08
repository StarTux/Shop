package com.winthier.shop.sql;

import com.winthier.sql.SQLDatabase;
import org.junit.Test;

public final class SQLTest {
    @Test
    public void main() {
        System.out.println(SQLDatabase.testTableCreation(SQLChest.class));
        System.out.println(SQLDatabase.testTableCreation(SQLLog.class));
        System.out.println(SQLDatabase.testTableCreation(SQLOffer.class));
        System.out.println(SQLDatabase.testTableCreation(SQLPlot.class));
        System.out.println(SQLDatabase.testTableCreation(SQLPlotTrust.class));
    }
}
