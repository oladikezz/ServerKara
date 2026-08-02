package ru.aurion.kara.model;

import java.util.UUID;

public record Fine(
        int id,
        UUID playerId,
        String playerName,
        double amount,
        long issuedAt,
        long deadline,
        String reason,
        UUID issuerId,
        String issuerName,
        boolean paid,
        boolean overdueNotified
) {
    public Fine withPaid(boolean value) {
        return new Fine(id, playerId, playerName, amount, issuedAt, deadline, reason,
                issuerId, issuerName, value, overdueNotified);
    }

    public Fine withOverdueNotified(boolean value) {
        return new Fine(id, playerId, playerName, amount, issuedAt, deadline, reason,
                issuerId, issuerName, paid, value);
    }
}
