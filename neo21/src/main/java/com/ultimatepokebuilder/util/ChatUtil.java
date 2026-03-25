package com.ultimatepokebuilder.util;

import net.minecraft.network.chat.Component;

public class ChatUtil {

    public static Component color(String text) {
        if (text == null) return Component.empty();
        return Component.literal(text.replace("&", "§"));
    }
}