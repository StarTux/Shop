package com.winthier.shop.sql;

import com.winthier.shop.BlockLocation;
import com.winthier.shop.ShopPlugin;
import com.winthier.shop.ShopType;
import com.winthier.shop.Shopper;
import com.winthier.shop.util.Item;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bukkit.inventory.ItemStack;

@Entity
@Table(name = "logs")
@Getter
@Setter
@NoArgsConstructor
public class SQLLog {
    @Id private Integer id;
    @Column(nullable = false) private Date time;
    @Column(nullable = false) private ShopType shopType;
    private UUID owner;
    @Column(nullable = false) private String ownerName;
    @Column(nullable = false) private UUID customer;
    @Column(nullable = false) private String customerName;
    @Column(nullable = false) private String world;
    @Column(nullable = false) private Integer x;
    @Column(nullable = false) private Integer y;
    @Column(nullable = false) private Integer z;
    @Column(nullable = false) private String material;
    @Column(nullable = false) private Integer itemAmount;
    @Column(nullable = false) private String itemName;
    @Column(nullable = false) private String itemDescription;
    @Column(nullable = false) private Double price;

    SQLLog(final Date time, final SQLChest chestData, final Shopper customer, final ItemStack item) {
        setTime(time);
        setShopType(chestData.getShopType());
        if (chestData.isAdminShop()) {
            setOwner(null);
            setOwnerName("The Bank");
        } else {
            setOwner(chestData.getOwner());
            setOwnerName(chestData.getShopper().getName());
        }
        setCustomer(customer.getUuid());
        setCustomerName(customer.getName());
        BlockLocation location = chestData.getLocation();
        setWorld(location.getWorld());
        setX(location.getX());
        setY(location.getY());
        setZ(location.getZ());
        setMaterial(item.getType().name().toLowerCase());
        setItemAmount(item.getAmount());
        setItemName(Item.getItemName(item));
        setItemDescription(Item.getItemDescription(item));
        setPrice(chestData.getPrice());
    }

    public static void store(SQLChest chestData, Shopper customer, ItemStack item) {
        if (chestData.getPrice() == 0.0) return;
        SQLLog log = new SQLLog(new Date(), chestData, customer, item);
        ShopPlugin.getInstance().getDb().save(log);
    }

    public static void store(SQLChest chestData, Shopper customer, ItemStack item, double price) {
        if (price == 0.0) return;
        SQLLog log = new SQLLog(new Date(), chestData, customer, item);
        log.setPrice(price);
        ShopPlugin.getInstance().getDb().save(log);
    }

    public static void find(UUID uuid, Consumer<List<SQLLog>> callback) {
        ShopPlugin.getInstance().getDb().find(SQLLog.class).where()
            .eq("owner", uuid)
            .orderByDescending("time")
            .findListAsync(callback);
    }
}
