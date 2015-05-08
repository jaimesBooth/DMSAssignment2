package com.example.taggame;


import java.io.Serializable;
import java.util.UUID;

public interface BattlePlayer extends Runnable, Serializable {
    // uuid for the Bluetooth application
    public static final UUID SERVICE_UUID
            = UUID.fromString("aa7e561f-591f-4767-bf26-e4bff3f089ff");
    // name for the Bluetooth application
    public static final String SERVICE_NAME = "Tag Game";
    public static final int MAX_HP = 20;

    public void forward(String message);

    public void stop();

    public void registerActivity(BattlefieldActivity battlefield);

    public String getPlayerName();
}

