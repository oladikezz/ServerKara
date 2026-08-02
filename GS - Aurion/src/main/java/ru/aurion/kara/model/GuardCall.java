package ru.aurion.kara.model;

import java.util.UUID;

public record GuardCall(
        int id,
        UUID playerId,
        String playerName,
        String message,
        String world,
        double x,
        double y,
        double z,
        long createdAt,
        boolean resolved
) {
    public GuardCall withResolved(boolean value) {
        return new GuardCall(id, playerId, playerName, message, world, x, y, z, createdAt, value);
    }
}
