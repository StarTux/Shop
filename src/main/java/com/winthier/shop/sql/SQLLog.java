package com.winthier.shop.sql;

import com.avaje.ebean.validation.NotNull;
import com.winthier.shop.BlockLocation;
import com.winthier.shop.ShopPlugin;
import com.winthier.shop.ShopType;
import com.winthier.shop.Shopper;
import com.winthier.shop.chest.ChestData;
import com.winthier.shop.util.Item;
import java.util.Date;
import java.util.List;
import java.util.UUID;
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
    @NotNull private Date time;
    @NotNull private ShopType shopType;
    private UUID owner;
    @NotNull private String ownerName;
    @NotNull private UUID customer;
    @NotNull private String customerName;
    @NotNull private String world;
    @NotNull private Integer x, y, z;
    @NotNull private Integer itemType, itemDamage, itemAmount;
    @NotNull private String itemName;
    @NotNull private String itemDescription;
    @NotNull private Double price;

    SQLLog(Date time, ChestData chestData, Shopper customer, ItemStack item) {
        setTime(time);
        setShopType(chestData.getShopType());
        if (chestData.isAdminShop()) {
            setOwner(null);
            setOwnerName("The Bank");
        } else {
            setOwner(chestData.getOwner().getUuid());
            setOwnerName(chestData.getOwner().getName());
        }
        setCustomer(customer.getUuid());
        setCustomerName(customer.getName());
        BlockLocation location = chestData.getLocation();
        setWorld(location.getWorld());
        setX(location.getX());
        setY(location.getY());
        setZ(location.getZ());
        setItemType(item.getType().getId());
        setItemDamage((int)item.getDurability());
        setItemAmount(item.getAmount());
        setItemName(Item.getItemName(item));
        setItemDescription(Item.getItemDescription(item));
        setPrice(chestData.getPrice());
    }

    public static void store(ChestData chestData, Shopper customer, ItemStack item) {
        SQLLog log = new SQLLog(new Date(), chestData, customer, item);
        ShopPlugin.getInstance().getDatabase().save(log);
    }

    public static void store(ChestData chestData, Shopper customer, ItemStack item, double price) {
        SQLLog log = new SQLLog(new Date(), chestData, customer, item);
        log.setPrice(price);
        ShopPlugin.getInstance().getDatabase().save(log);
    }

    public static List<SQLLog> find(UUID uuid) {
        return ShopPlugin.getInstance().getDatabase().find(SQLLog.class).where()
            .eq("owner", uuid)
            .orderBy("time desc")
            .findList();
    }
}
