package com.winthier.shop.playercache;

import com.winthier.playercache.PlayerCache;
import java.util.UUID;

public final class PlayerCacheHandler {
    public String nameForUuid(UUID uuid) {
        return PlayerCache.nameForUuid(uuid);
    }

    public UUID uuidForName(String name) {
        return PlayerCache.uuidForName(name);
    }
}
