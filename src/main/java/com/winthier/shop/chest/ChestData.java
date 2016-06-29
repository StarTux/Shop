package com.winthier.shop.chest;

import com.winthier.shop.BlockLocation;
import com.winthier.shop.ShopType;
import com.winthier.shop.Shopper;
import com.winthier.shop.util.Msg;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

@Getter
@RequiredArgsConstructor
public class ChestData {
    public enum Type { SIGN, NAMED_CHEST; }
    final Type type;
    final ShopType shopType;
    final BlockLocation location;
    final Shopper owner;
    final double price;
    final boolean adminShop;

    @Setter transient boolean soldOut = false;

    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", type.name());
        map.put("shopType", shopType.name());
        map.put("location", location.serialize());
        if (owner != null) map.put("owner", owner.serialize());
        map.put("price", price);
        if (adminShop) map.put("adminShop", adminShop);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static ChestData deserialize(Map<String, Object> map) {
        Type type = Type.valueOf((String)map.get("type"));
        ShopType shopType = ShopType.valueOf((String)map.get("shopType"));
        BlockLocation location = BlockLocation.deserialize((Map<String, Object>)map.get("location"));
        Shopper owner;
        if (map.containsKey("owner")) {
            owner = Shopper.deserialize((Map<String, Object>)map.get("owner"));
        } else {
            owner = null;
        }
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


    // Real world

    public Sign getSign() {
        Block block = location.getBlock();
        BlockState blockState = block.getState();
        if (!(blockState instanceof Sign)) return null;
        return (Sign)blockState;
    }

    public boolean updateInWorld() {
        if (type == Type.SIGN) {
            Sign sign = getSign();
            if (sign == null) return false;
            if (getShopType() == ShopType.BUY) {
                if (sign.getLine(0).toLowerCase().contains("buy")) {
                    sign.setLine(0, Msg.format("&r[&9&lBuy&r]"));
                } else {
                    sign.setLine(0, Msg.format("&r[&9&lShop&r]"));
                }
            } else if (getShopType() == ShopType.SELL) {
                sign.setLine(0, Msg.format("&r[&9&lSell&r]"));
            }
            if (soldOut) {
                sign.setLine(1, Msg.format("&4SOLD OUT"));
            } else {
                sign.setLine(1, Msg.format("&1%.02f", price));
            }
            if (adminShop) {
                sign.setLine(2, "");
                sign.setLine(3, Msg.format("&9The Bank"));
            } else {
                String ownerName = owner.getName();
                if (ownerName.length() <= 13) {
                    sign.setLine(2, "");
                    sign.setLine(3, Msg.format("&8%s", ownerName));
                } else {
                    int b = (ownerName.length() - 1) / 2 + 1;
                    sign.setLine(2, Msg.format("&8%s", ownerName.substring(0, b)));
                    sign.setLine(3, Msg.format("&8%s", ownerName.substring(b)));
                }
            }
            sign.update();
            return true;
        }
        return false;
    }
}
