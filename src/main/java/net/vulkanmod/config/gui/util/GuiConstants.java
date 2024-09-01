package net.vulkanmod.config.gui.util;

import net.vulkanmod.vulkan.util.ColorUtil;

public class GuiConstants {
    public static final int COLOR_WHITE = ColorUtil.ARGB.pack(1f, 1f, 1f, 1f);
    public static final int COLOR_BLACK = ColorUtil.ARGB.pack(0f, 0f, 0f, 1f);
    public static final int COLOR_GRAY = ColorUtil.ARGB.pack(0.6f, 0.6f, 0.6f, 1f);
    public static final int COLOR_DARK_GRAY = ColorUtil.ARGB.pack(0.4f, 0.4f, 0.4f, 1f);
    public static final int COLOR_RED = ColorUtil.ARGB.pack(0.59f, 0.18f, 0.17f, 1f);
    public static final int COLOR_DARK_RED = ColorUtil.ARGB.pack(0.15f, 0.05f, 0.04f, 1f);

    public static final int WIDGET_HEIGHT = 20;
    public static final int WIDGET_MARGIN = 5;

    private GuiConstants() {
    }
}
