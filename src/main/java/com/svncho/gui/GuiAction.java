package com.svncho.gui;

public enum GuiAction {
    CONFIRM,
    INFO,
    CLOSE_RETURN,
    NONE;

    public static GuiAction from(String raw) {
        if (raw == null || raw.isBlank()) return NONE;
        try {
            return GuiAction.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
