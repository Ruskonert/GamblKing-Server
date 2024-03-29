package com.ruskonert.GamblKing.engine.event.connect;

import com.ruskonert.GamblKing.entity.Player;
import com.ruskonert.GamblKing.event.Event;

public class PlayerLoginAttemptEvent extends Event
{
    private Player player;
    public Player getPlayer() { return this.player; }

    public PlayerLoginAttemptEvent(Player player)
    {
        this.player = player;
    }
}
