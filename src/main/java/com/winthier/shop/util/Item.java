package com.winthier.shop.util;

import net.milkbowl.vault.item.ItemInfo;
import net.milkbowl.vault.item.Items;
import org.bukkit.inventory.ItemStack;

public class Item {
    public static String capitalName(String in) {
        return "" + Character.toUpperCase(in.charAt(0)) + in.substring(1, in.length()).toLowerCase();
    }

    public static String niceEnumName(String name) {
        String[] parts = name.split("_");
        StringBuilder sb = new StringBuilder(capitalName(parts[0]));
        for (int i = 1; i < parts.length; ++i) {
            sb.append(" ").append(capitalName(parts[i]));
        }
        return sb.toString();
    }

    public static String getItemName(ItemStack item) {
        ItemInfo info = Items.itemByStack(item);
        if (info == null) {
            return niceEnumName(item.getType().name());
        }
        return info.getName();
    }
}
