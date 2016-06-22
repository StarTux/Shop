package com.winthier.shop.chest;

import com.winthier.shop.BlockLocation;
import com.winthier.shop.ShopType;
import com.winthier.shop.Shopper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

@Getter
@RequiredArgsConstructor
public class ChestData {
    public enum Type { SIGN, INVENTORY_NAME; }
    final Type type;
    final ShopType shopType;
    final BlockLocation location;
    final Shopper owner;
    final double price;
    final boolean adminShop;

    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", type.name());
        map.put("shopType", shopType.name());
        map.put("location", location.serialize());
        map.put("owner", owner.serialize());
        map.put("price", price);
        if (adminShop) map.put("adminShop", adminShop);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static ChestData deserialize(Map<String, Object> map) {
        Type type = Type.valueOf((String)map.get("type"));
        ShopType shopType = ShopType.valueOf((String)map.get("shopType"));
        BlockLocation location = BlockLocation.deserialize((Map<String, Object>)map.get("location"));
        Shopper owner = Shopper.deserialize((Map<String, Object>)map.get("owner"));
        double price = (Double)map.get("price");
        boolean adminShop = map.containsKey("adminShop") ? (Boolean)map.get("adminShop") : false;
        return new ChestData(type, shopType, location, owner, price, adminShop);
    }

    public Player getPlayer() {
        return Bukkit.getServer().getPlayer(getOwner().getUuid());
    }

    public boolean isOwner(Player player) {
        if (isAdminShop()) return false;
        return player.getUniqueId().equals(getOwner().getUuid());
    }
}
