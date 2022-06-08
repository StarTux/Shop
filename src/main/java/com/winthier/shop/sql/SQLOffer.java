package com.winthier.shop.sql;

import com.cavetale.core.item.ItemKinds;
import com.winthier.shop.BlockLocation;
import com.winthier.shop.ShopPlugin;
import com.winthier.shop.ShopType;
import com.winthier.shop.util.Item;
import com.winthier.sql.SQLRow;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.inventory.ItemStack;

@Getter @Setter @NoArgsConstructor
@Table(name = "offers", indexes = @Index(name = "world", columnList = "world"))
public final class SQLOffer implements SQLRow {
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
    @Column(nullable = false) private Integer x;
    @Column(nullable = false) private Integer y;
    @Column(nullable = false) private Integer z;
    @Column(nullable = false) private String material;
    @Column(nullable = false) private Integer itemAmount;
    @Column(nullable = false, length = 4096) private String itemDisplayName; // Component
    @Column(nullable = false) private String itemDescription;
    @Column(nullable = false) private Double price;

    SQLOffer(final Date time, final BlockLocation location, final SQLChest chestData, final ItemStack item) {
        this.time = time;
        this.shopType = chestData.getShopType();
        if (chestData.isAdminShop()) {
            this.owner = null;
            this.ownerName = "The Bank";
        } else {
            this.owner = chestData.getOwner();
            this.ownerName = chestData.getOwnerName();
        }
        this.world = location.getWorld();
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.material = item.getType().getKey().getKey();
        this.itemAmount = item.getAmount();
        Component itemComponent = ItemKinds.chatDescription(item);
        this.itemDisplayName = GsonComponentSerializer.gson().serialize(itemComponent);
        this.itemDescription = Item.getItemDescription(item);
        this.price = chestData.getPrice();
    }

    public BlockLocation getBlockLocation() {
        return new BlockLocation(getWorld(), getX(), getY(), getZ());
    }

    public static Map<BlockLocation, Offers> getCache() {
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
        if (list.isEmpty()) return null;
        long timeDiff = Math.max(1000 * 60 * 60 * 12, list.size() * 1000);
        Offers oldest = list.get(0);
        for (Offers offers : list) {
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

    public static void store(BlockLocation location, SQLChest chestData, List<ItemStack> items) {
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
        return getPrice() / (double) getItemAmount();
    }

    public Component parseItemDisplayName() {
        return itemDisplayName != null && !itemDisplayName.isEmpty()
            ? GsonComponentSerializer.gson().deserialize(itemDisplayName)
            : Component.empty();
    }
}
