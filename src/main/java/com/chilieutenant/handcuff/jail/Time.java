package com.chilieutenant.handcuff.jail;

public class Time {

    public static String getDateBySeconds(int seconds) {
        int hours = ((seconds % 604800) % 86400) / 3600;
        int minutes = (((seconds % 604800) % 86400) % 3600) / 60;
        seconds = (((seconds % 604800) % 86400) % 3600) % 60;
        return + hours + "h " + minutes + "m " + seconds + "s";
    }

}