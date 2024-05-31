package com.winthier.shop.sql;

import com.cavetale.core.item.ItemKinds;
import com.winthier.shop.BlockLocation;
import com.winthier.shop.ShopPlugin;
import com.winthier.shop.ShopType;
import com.winthier.shop.util.Item;
import com.winthier.sql.SQLRow;
import com.winthier.sql.SQLRow.Name;
import com.winthier.sql.SQLRow.NotNull;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.Data;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@Data
@NotNull
@Name("logs")
public class SQLLog implements SQLRow {
    @Id private Integer id;
    private Date time;
    private ShopType shopType;
    @Keyed @Nullable private UUID owner;
    private String ownerName;
    private UUID customer;
    private String customerName;
    private String world;
    private Integer x;
    private Integer y;
    private Integer z;
    private String material;
    private Integer itemAmount;
    private String itemName;
    private String itemDescription;
    private Double price;

    public SQLLog() { }

    public SQLLog(final Date time, final SQLChest chestData, final Player customer, final ItemStack item, final double price, final int amount) {
        this.time = time;
        this.shopType = chestData.getShopType();
        if (chestData.isAdminShop()) {
            this.owner = null;
            this.ownerName = "The Bank";
        } else {
            this.owner = chestData.getOwner();
            this.ownerName = chestData.getOwnerName();
        }
        this.customer = customer.getUniqueId();
        this.customerName = customer.getName();
        BlockLocation location = chestData.getLocation();
        this.world = location.getWorld();
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.material = item.getType().name().toLowerCase();
        this.itemAmount = amount;
        this.itemName = ItemKinds.name(item);
        this.itemDescription = Item.getItemDescription(item);
        this.price = price;
    }

    public static void store(SQLChest chestData, Player customer, ItemStack item, double price, int amount) {
        if (price == 0.0) return;
        SQLLog log = new SQLLog(new Date(), chestData, customer, item, price, amount);
        ShopPlugin.getInstance().getDb().save(log);
    }

    public static void find(UUID uuid, Consumer<List<SQLLog>> callback) {
        ShopPlugin.getInstance().getDb().find(SQLLog.class).where()
            .eq("owner", uuid)
            .orderByDescending("time")
            .findListAsync(callback);
    }
}
