package com.winthier.shop.sql;

import com.avaje.ebean.validation.Length;
import com.avaje.ebean.validation.NotEmpty;
import com.avaje.ebean.validation.NotNull;
import com.winthier.shop.BlockLocation;
import com.winthier.shop.ShopPlugin;
import com.winthier.shop.ShopType;
import com.winthier.shop.chest.ChestData;
import com.winthier.shop.util.Item;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PersistenceException;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

@Entity
@Table(name = "offers")
@Getter
@Setter
@NoArgsConstructor
public class SQLOffer {
    // Cache
    static class Offers {
        final List<SQLOffer> list = new ArrayList<>();
        boolean invalid = false;
    }
    static Map<BlockLocation, Offers> cache = null;
    // Payload
    @Id Integer id;
    @NotNull Date time;
    @NotNull ShopType shopType;
    UUID owner;
    @NotNull String ownerName;
    @NotNull String world;
    @NotNull Integer x, y, z;
    @NotNull Integer itemType, itemDamage, itemAmount;
    @NotNull String itemName;
    @NotNull String itemDescription;
    @NotNull Double price;

    SQLOffer(Date time, BlockLocation location, ChestData chestData, ItemStack item){
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
        setItemType(item.getType().getId());
        setItemDamage((int)item.getDurability());
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
            for (SQLOffer offer: ShopPlugin.getInstance().getDatabase().find(SQLOffer.class).findList()) {
                BlockLocation location = offer.getBlockLocation();
                Offers offers = cache.get(location);
                if (offers == null) {
                    offers = new Offers();
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
        final Offers result = getCache().remove(location);
        if (result == null) return false;
        result.invalid = true;
        Iterator<SQLOffer> iter = result.list.iterator();
        new BukkitRunnable() {
            @Override public void run() {
                if (!iter.hasNext()) {
                    cancel();
                } else {
                    try {
                        ShopPlugin.getInstance().getDatabase().delete(iter.next());
                    } catch (PersistenceException pe) {
                        pe.printStackTrace();
                    }
                }
            }
        }.runTaskTimer(ShopPlugin.getInstance(), 1, 1);
        return true;
    }

    public static void store(BlockLocation location, ChestData chestData, List<ItemStack> items) {
        Date time = new Date();
        List<SQLOffer> list = new ArrayList<>();
        List<ItemStack> doneItems = new ArrayList<>();
        for (ItemStack item: items) {
            for (ItemStack doneItem: doneItems) if (doneItem.isSimilar(item)) continue;
            doneItems.add(item);
            list.add(new SQLOffer(time, location, chestData, item));
        }
        deleteAt(location);
        final Offers offers = new Offers();
        offers.list.addAll(list);
        getCache().put(location, offers);
        final Iterator<SQLOffer> iter = list.iterator();
        new BukkitRunnable() {
            @Override public void run() {
                if (offers.invalid || !iter.hasNext()) {
                    cancel();
                } else {
                    try {
                        ShopPlugin.getInstance().getDatabase().save(iter.next());
                    } catch (PersistenceException pe) {
                        pe.printStackTrace();
                        cancel();
                    }
                }
            }
        }.runTaskTimer(ShopPlugin.getInstance(), 1, 1);
    }

    public double pricePerItem() {
        return getPrice() / (double)getItemAmount();
    }
}

