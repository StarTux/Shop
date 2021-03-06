package com.winthier.shop.util;

import com.winthier.generic_events.GenericEvents;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;

public final class Item {
    private Item() { }

    private static boolean warnedAboutPotionData = false;

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
        String result = GenericEvents.getItemName(item);
        if (result != null) return result;
        return Msg.camelCase(item.getType().name());
    }

    public static String getEnchantmentName(Enchantment enchantment) {
        return niceEnumName(enchantment.getKey().getKey());
    }

    public static String getItemDescription(ItemStack item) {
        String result = GenericEvents.getItemName(item);
        if (result != null) return result;
        StringBuilder desc = new StringBuilder(getItemName(item));
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            Map<Enchantment, Integer> enchants;
            enchants = meta.getEnchants();
            if (enchants.isEmpty() && meta instanceof EnchantmentStorageMeta) {
                enchants = ((EnchantmentStorageMeta) meta).getStoredEnchants();
            }
            if (enchants != null && !enchants.isEmpty()) {
                for (Enchantment enchant : enchants.keySet()) {
                    desc.append(", ");
                    desc.append(getEnchantmentName(enchant));
                    desc.append(" ");
                    desc.append(enchants.get(enchant));
                }
            }
            if (meta instanceof PotionMeta) {
                PotionMeta potions = (PotionMeta) meta;
                try {
                    PotionData data = potions.getBasePotionData();
                    if (data != null && data.getType() != PotionType.UNCRAFTABLE) {
                        desc.append(", ");
                        desc.append(niceEnumName(data.getType().name()));
                        if (data.isExtended()) desc.append(" Ext");
                        if (data.isUpgraded()) desc.append(" II");
                    }
                } catch (IllegalArgumentException iae) {
                    if (!warnedAboutPotionData) {
                        warnedAboutPotionData = true;
                        iae.printStackTrace();
                        System.err.println("Omitting further stack traces at MarketCrawler");
                    }
                }
                if (potions.hasCustomEffects()) {
                    for (PotionEffect effect: potions.getCustomEffects()) {
                        desc.append(", ");
                        desc.append(niceEnumName(effect.getType().getName()));
                        int amp = effect.getAmplifier();
                        if (amp > 0) {
                            desc.append(" ").append((amp + 1));
                        }
                    }
                }
            }
            if (meta instanceof SkullMeta) {
                SkullMeta skull = (SkullMeta) meta;
                String name = null;
                if (skull.hasOwner()) {
                    name = skull.getOwningPlayer().getName();
                    if (name == null) {
                        name = skull.getOwner();
                    }
                }
                if (name == null) {
                    name = meta.getDisplayName();
                }
                if (name != null) {
                    desc.append(" <");
                    desc.append(ChatColor.stripColor(name));
                    desc.append(">");
                }
            }
        }
        return desc.toString();
    }
}
