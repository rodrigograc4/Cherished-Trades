package com.rodrigograc4.cherishedtrades.util;

public class EnchantmentHelper {

    public static String convertToIdFormat(String displayName) {
        String name = displayName.toLowerCase().trim();
        int level = 1;

        if (name.endsWith(" 5")) { level = 5; name = name.substring(0, name.length() - 2); }
        else if (name.endsWith(" 4")) { level = 4; name = name.substring(0, name.length() - 2); }
        else if (name.endsWith(" 3")) { level = 3; name = name.substring(0, name.length() - 2); }
        else if (name.endsWith(" 2")) { level = 2; name = name.substring(0, name.length() - 2); }
        else if (name.endsWith(" 1")) { level = 1; name = name.substring(0, name.length() - 2); }

        name = name.trim().replace(" ", "_");
        
        if (name.equals("curse_of_vanishing")) name = "vanishing_curse";
        if (name.equals("curse_of_binding")) name = "binding_curse";

        return "minecraft:" + name + " " + level;
    }
}