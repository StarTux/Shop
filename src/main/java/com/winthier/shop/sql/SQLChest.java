package com.winthier.shop.sql;

import com.cavetale.core.command.RemotePlayer;
import com.cavetale.core.connect.Connect;
import com.cavetale.mytems.item.coin.Coin;
import com.winthier.shop.BlockLocation;
import com.winthier.shop.ShopPlugin;
import com.winthier.shop.ShopType;
import com.winthier.shop.Shopper;
import com.winthier.sql.SQLRow;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import static com.cavetale.core.font.Unicode.tiny;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@Getter @Setter @Table(name = "chests")
public final class SQLChest implements SQLRow {
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

    public RemotePlayer getRemotePlayer() {
        return owner != null
            ? Connect.get().getRemotePlayer(owner)
            : null;
    }

    public Player getPlayer() {
        return owner != null
            ? Bukkit.getServer().getPlayer(owner)
            : null;
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
        Block block = getLocation().getBlock();
        if (block == null) return null;
        BlockState blockState = block.getState();
        if (!(blockState instanceof Sign)) return null;
        return (Sign) blockState;
    }

    public boolean updateInWorld() {
        if (type != Type.SIGN) return false;
        Sign sign = getSign();
        if (sign == null) {
            if (ShopPlugin.getInstance().isDebugMode()) {
                ShopPlugin.getInstance().getLogger().warning("SQLChest#updateInWorld: Sign not found: " + location);
            }
            return false;
        }
        if (getShopType() == ShopType.BUY) {
            String firstLine = PlainTextComponentSerializer.plainText().serialize(sign.line(0));
            if (firstLine.toLowerCase().contains("buy")) {
                sign.line(0, text("[Buy]", BLUE));
            } else {
                sign.line(0, text("[Shop]", BLUE));
            }
        } else if (getShopType() == ShopType.SELL) {
            sign.line(0, text("[Sell]", BLUE));
        }
        if (soldOut) {
            sign.line(1, text(tiny("sold out"), DARK_RED));
        } else {
            sign.line(1, Coin.format(price));
        }
        if (adminShop) {
            sign.line(2, empty());
            sign.line(3, text(tiny("the bank"), AQUA));
        } else {
            sign.line(2, empty());
            sign.line(3, text(getShopper().getName(), DARK_GRAY));
        }
        sign.update();
        return true;
    }
}
