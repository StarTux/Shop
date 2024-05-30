package com.winthier.shop.util;

import com.cavetale.core.item.ItemKinds;
import com.cavetale.mytems.Mytems;
import com.destroystokyo.paper.profile.PlayerProfile;
import java.util.Map;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;
import static net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText;

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

    public static String getEnchantmentName(Enchantment enchantment) {
        return niceEnumName(enchantment.getKey().getKey());
    }

    public static String getItemDescription(ItemStack item) {
        Mytems mytems = Mytems.forItem(item);
        if (mytems != null) {
            return mytems.name(item);
        }
        StringBuilder desc = new StringBuilder(ItemKinds.name(item));
        if (!item.hasItemMeta()) return desc.toString();
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasItemFlag(ItemFlag.HIDE_ENCHANTS)) {
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
        }
        if (meta instanceof PotionMeta && !meta.hasItemFlag(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)) {
            PotionMeta potions = (PotionMeta) meta;
            try {
                PotionType potionType = potions.getBasePotionType();
                if (potionType != null && potionType != PotionType.AWKWARD) {
                    desc.append(", ");
                    desc.append(niceEnumName(potionType.name()));
                    if (potionType.name().startsWith("LONG_")) desc.append(" Ext");
                    if (potionType.name().startsWith("STRONG_")) desc.append(" II");
                }
            } catch (IllegalArgumentException iae) {
                if (!warnedAboutPotionData) {
                    warnedAboutPotionData = true;
                    iae.printStackTrace();
                    System.err.println("Omitting further stack traces at MarketCrawler");
                }
            }
            if (potions.hasCustomEffects()) {
                for (PotionEffect effect : potions.getCustomEffects()) {
                    desc.append(", ");
                    desc.append(niceEnumName(effect.getType().getKey().getKey()));
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
            PlayerProfile playerProfile = skull.getPlayerProfile();
            if (playerProfile != null) {
                name = playerProfile.getName();
            }
            if (name == null && skull.hasOwner()) {
                name = skull.getOwningPlayer().getName();
            }
            if (name != null) {
                desc.append(" <");
                // This nightmare is the replacement for ChatColor.stripColor()
                // See https://docs.advntr.dev/migration/bungeecord-chat-api.html
                desc.append(plainText().serialize(legacySection().deserialize(name)));
                desc.append(">");
            }
        }
        return desc.toString();
    }
}
