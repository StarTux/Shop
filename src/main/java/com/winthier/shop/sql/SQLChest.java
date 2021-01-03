package com.winthier.shop.sql;

import com.winthier.shop.BlockLocation;
import com.winthier.shop.ShopPlugin;
import com.winthier.shop.ShopType;
import com.winthier.shop.Shopper;
import com.winthier.shop.util.Msg;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

@Getter @Setter @Table(name = "chests")
public final class SQLChest {
    @Id private Integer id;
    @Column(nullable = false) private Type type;
    @Column(nullable = false) private ShopType shopType;
    @Column(nullable = false) private String world;
    @Column(nullable = false) private int x;
    @Column(nullable = false) private int y;
    @Column(nullable = false) private int z;
    @Column(nullable = true) private UUID owner;
    @Column(nullable = false) private double price;
    @Column(nullable = false) private boolean adminShop;
    private transient boolean soldOut = false;
    private transient BlockLocation location;
    private transient Shopper shopper;

    public enum Type {
        SIGN,
        NAMED_CHEST;
    }

    public SQLChest() { }

    public SQLChest(final Type type, final ShopType shopType, final BlockLocation location,
                    final Shopper shopper, final double price, final boolean adminShop) {
        this.type = type;
        this.shopType = shopType;
        this.location = location;
        this.world = location.getWorld();
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.shopper = shopper;
        if (shopper != null) {
            this.owner = shopper.getUuid();
        }
        this.price = price;
        this.adminShop = adminShop;
    }

    public Shopper getShopper() {
        if (shopper == null) {
            shopper = ShopPlugin.getInstance().findShopper(owner);
        }
        return shopper;
    }

    public BlockLocation getLocation() {
        if (location == null) {
            location = new BlockLocation(world, x, y, z);
        }
        return location;
    }

    public Player getPlayer() {
        return Bukkit.getServer().getPlayer(owner);
    }

    public boolean isOwner(Player player) {
        if (isAdminShop()) return false;
        return player.getUniqueId().equals(owner);
    }

    public String getOwnerName() {
        if (adminShop) return "The Bank";
        if (owner == null) return "N/A";
        return getShopper().getName();
    }

    // Real world

    public Sign getSign() {
        Block block = location.getBlock();
        BlockState blockState = block.getState();
        if (!(blockState instanceof Sign)) return null;
        return (Sign) blockState;
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
                String ownerName = getShopper().getName();
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
