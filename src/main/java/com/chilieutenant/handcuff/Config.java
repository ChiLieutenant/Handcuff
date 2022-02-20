package com.chilieutenant.handcuff;

import de.leonhard.storage.Yaml;

import java.util.Arrays;

public class Config {

    static Yaml config = new Yaml("config", "plugins/Handcuff");

    public static Yaml getConfig(){
        return config;
    }

    public static void loadDefaultConfig(){
        config.setDefault("handcuff.chance", 0.1);
        config.setDefault("handcuff.time.basetime", 120);
        config.setDefault("handcuff.time.perkill", 120);
        config.setDefault("handcuff.time.leave", 120);
        config.setDefault("handcuff.handcufftime", 3);
        config.setDefault("handcuff.distance", 2);
        config.setDefault("handcuff.item", null);
        config.setDefault("handcuff.location", null);
        config.setDefault("jail.outlocation", null);
        config.setDefault("jail.useablecommands", Arrays.asList(""));
        config.setDefault("messages.prefix", "&5[CityCraft]");
        config.setDefault("messages.cancelduetotime", "&cYou failed to handcuff the player.");
        config.setDefault("messages.cancelduetodistance", "&cYou are too far to handcuff this player!");
        config.setDefault("messages.handcuffsucceeded", "&7You have been handcuffed. You can attempt /jail struggle to break free.");
    }

}
