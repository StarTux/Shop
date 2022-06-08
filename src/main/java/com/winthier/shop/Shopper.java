package com.winthier.shop;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Value;
import org.bukkit.entity.Player;

@Value
public final class Shopper {
    private final UUID uuid;
    private final String name;

    public static Shopper of(Player player) {
        return new Shopper(player.getUniqueId(), player.getName());
    }

    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("uuid", uuid.toString());
        map.put("name", name);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static Shopper deserialize(Map<String, Object> map) {
        UUID uuid = UUID.fromString((String) map.get("uuid"));
        String name = (String) map.get("name");
        return new Shopper(uuid, name);
    }
}
