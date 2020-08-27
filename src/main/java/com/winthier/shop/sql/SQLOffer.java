package com.winthier.shop.sql;

import com.winthier.shop.BlockLocation;
import com.winthier.shop.ShopPlugin;
import com.winthier.shop.ShopType;
import com.winthier.shop.chest.ChestData;
import com.winthier.shop.util.Item;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.inventory.ItemStack;

@Entity
@Table(name = "offers")
@Getter
@Setter
@NoArgsConstructor
public final class SQLOffer {
    // Cache
    @RequiredArgsConstructor
    static class Offers {
        private final BlockLocation location;
        private final List<SQLOffer> list = new ArrayList<>();
        private long time = System.currentTimeMillis();
    }
    private static Map<BlockLocation, Offers> cache = null;
    // Payload
    @Id private Integer id;
    @Column(nullable = false) private Date time;
    @Column(nullable = false) private ShopType shopType;
    private UUID owner;
    @Column(nullable = false) private String ownerName;
    @Column(nullable = false) private String world;
    @Column(nullable = false) private Integer x, y, z;
    @Column(nullable = false) private String material;
    @Column(nullable = false) private Integer itemAmount;
    @Column(nullable = false) private String itemName;
    @Column(nullable = false) private String itemDescription;
    @Column(nullable = false) private Double price;

    SQLOffer(Date time, BlockLocation location, ChestData chestData, ItemStack item) {
        setTime(time);
        setShopType(chestData.getShopType());
        if (chestData.isAdminShop()) {
            setOwner(null);
            setOwnerName("The Bank");
        } else {
            setOwner(chestData.getOwner().getUuid());
            setOwnerName(chestData.getOwner().getName());
        }
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

    public BlockLocation getBlockLocation() {
        return new BlockLocation(getWorld(), getX(), getY(), getZ());
    }

    static Map<BlockLocation, Offers> getCache() {
        if (cache == null) {
            cache = new HashMap<>();
            for (SQLOffer offer: ShopPlugin.getInstance().getDb().find(SQLOffer.class).findList()) {
                BlockLocation location = offer.getBlockLocation();
                Offers offers = cache.get(location);
                if (offers == null) {
                    offers = new Offers(location);
                    offers.time = offer.getTime().getTime();
                    cache.put(location, offers);
                }
                offers.list.add(offer);
            }
        }
        return cache;
    }

    public static List<SQLOffer> getAllOffers() {
        List<SQLOffer> result = new ArrayList<>();
        for (Offers offers: getCache().values()) {
            result.addAll(offers.list);
        }
        return result;
    }

    public static BlockLocation findExpiredLocation() {
        long now = System.currentTimeMillis();
        List<Offers> list = new ArrayList<>(getCache().values());
        long timeDiff = Math.max(1000 * 60 * 60 * 12, list.size() * 1000);
        if (list.isEmpty()) return null;
        Offers oldest = list.get(0);
        for (Offers offers: list) {
            if (offers.time < oldest.time) oldest = offers;
        }
        if (oldest.time + timeDiff < now) {
            return oldest.location;
        }
        return null;
    }

    public static List<SQLOffer> getAllOffersInWorld(String world) {
        List<SQLOffer> result = new ArrayList<>();
        for (Map.Entry<BlockLocation, Offers> entry: getCache().entrySet()) {
            if (entry.getKey().getWorld().equals(world)) {
                result.addAll(entry.getValue().list);
            }
        }
        return result;
    }

    public static boolean deleteAt(BlockLocation location) {
        Offers result = getCache().remove(location);
        if (result == null) return false;
        ShopPlugin.getInstance().getDb().deleteAsync(result.list, null);
        return true;
    }

    public static void store(BlockLocation location, ChestData chestData, List<ItemStack> items) {
        Date time = new Date();
        List<SQLOffer> list = new ArrayList<>();
        List<ItemStack> doneItems = new ArrayList<>();
    OUTER: for (ItemStack item: items) {
            for (ItemStack doneItem: doneItems) {
                if (doneItem.isSimilar(item)) continue OUTER;
            }
            doneItems.add(item);
            list.add(new SQLOffer(time, location, chestData, item));
        }
        deleteAt(location);
        final Offers offers = new Offers(location);
        offers.list.addAll(list);
        ShopPlugin.getInstance().getDb().save(list);
        getCache().put(location, offers);
    }

    public double pricePerItem() {
        return getPrice() / (double)getItemAmount();
    }
}
