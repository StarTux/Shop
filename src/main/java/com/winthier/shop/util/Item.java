package com.winthier.shop.util;

import java.util.Map;
import net.milkbowl.vault.item.ItemInfo;
import net.milkbowl.vault.item.Items;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;

public class Item {
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
        ItemInfo info = Items.itemByStack(item);
        if (info == null) {
            return niceEnumName(item.getType().name());
        }
        return info.getName();
    }

    public static String getEnchantmentName(Enchantment enchantment) {
        switch (enchantment.getId()) {
        case 0: return "Protection";
        case 1: return "Fire Protection";
        case 2: return "Feather Falling";
        case 3: return "Blast Protection";
        case 4: return "Projectile Protection";
        case 5: return "Respiration";
        case 6: return "Aqua Affinity";
        case 7: return "Thorns";
        case 16: return "Sharpness";
        case 17: return "Smite";
        case 18: return "Bane of Arthropods";
        case 19: return "Knockback";
        case 20: return "Fire Aspect";
        case 21: return "Looting";
        case 48: return "Power";
        case 49: return "Punch";
        case 50: return "Flame";
        case 51: return "Infinity";
        case 32: return "Efficiency";
        case 33: return "Silk Touch";
        case 34: return "Unbreaking";
        case 35: return "Fortune";
        case 61: return "Luck of the Sea";
        case 62: return "Lure";
        default: return niceEnumName(enchantment.getName());
        }
    }

    public static String getItemDescription(ItemStack item) {
        StringBuilder desc = new StringBuilder(getItemName(item));
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            Map<Enchantment, Integer> enchants;
            enchants = meta.getEnchants();
            if (enchants.isEmpty() && meta instanceof EnchantmentStorageMeta) {
                enchants = ((EnchantmentStorageMeta)meta).getStoredEnchants();
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
                PotionMeta potions = (PotionMeta)meta;
                try {
                    PotionData data = potions.getBasePotionData();
                    if (data != null) {
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
                            desc.append(" ").append((amp+1));
                        }
                    }
                }
            }
            if (meta instanceof SkullMeta) {
                SkullMeta skull = (SkullMeta)meta;
                if (skull.hasOwner()) {
                    desc.append(" <");
                    desc.append(skull.getOwner());
                    desc.append(">");
                }
            }
        }
        return desc.toString();
    }
}
