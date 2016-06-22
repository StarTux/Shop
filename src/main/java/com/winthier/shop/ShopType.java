package com.winthier.shop;

public enum ShopType {
    /** A shop players can buy from */
    BUY,
    /** A shop players can sell to */
    SELL;

    public static ShopType of(String name) {
        if ("shop".equalsIgnoreCase(name)) return ShopType.BUY;
        if ("buy".equalsIgnoreCase(name)) return ShopType.BUY;
        if ("sell".equalsIgnoreCase(name)) return ShopType.SELL;
        return null;
    }

    public String gerund() {
        switch (this) {
        case BUY: return "buying";
        case SELL: return "selling";
        default: return "shopping"; // Should never happen!
        }
    }
}
