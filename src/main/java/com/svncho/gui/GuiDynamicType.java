package com.svncho.gui;

public enum GuiDynamicType {
    NONE,
    INFO,
    STATS;

    public static GuiDynamicType from(String raw) {
        if (raw == null || raw.isBlank()) return NONE;
        try {
            return GuiDynamicType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
