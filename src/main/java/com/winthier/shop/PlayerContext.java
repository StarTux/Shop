package com.winthier.shop;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;

@RequiredArgsConstructor
public final class PlayerContext {
    final UUID player;
    final List<Component> pages = new ArrayList<>();
    final List<BlockLocation> locations = new ArrayList<>();
    ShopType searchType;

    void clear() {
        pages.clear();
        locations.clear();
    }
}
