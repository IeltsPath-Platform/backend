package com.group01.access.domain.aggregate;

import com.group01.access.domain.exception.InsufficientPointsException;

import java.time.Instant;
import java.util.UUID;

public class PointWallet {

    private final UUID userId;
    private long balance;
    private long totalCredited;
    private long totalDebited;
    private long rowVersion;
    private Instant updatedAt;

    public PointWallet(UUID userId, long balance, long totalCredited, long totalDebited, long rowVersion, Instant updatedAt) {
        if (balance < 0) {
            throw new IllegalArgumentException("Wallet balance cannot be negative");
        }
        this.userId = userId;
        this.balance = balance;
        this.totalCredited = totalCredited;
        this.totalDebited = totalDebited;
        this.rowVersion = rowVersion;
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static PointWallet create(UUID userId) {
        return new PointWallet(userId, 0L, 0L, 0L, 0L, Instant.now());
    }

    public UUID getUserId() {
        return userId;
    }

    public long getBalance() {
        return balance;
    }

    public long getTotalCredited() {
        return totalCredited;
    }

    public long getTotalDebited() {
        return totalDebited;
    }

    public long getRowVersion() {
        return rowVersion;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean canDebit(long amount) {
        return amount > 0 && this.balance >= amount;
    }

    public void credit(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Credit amount must be greater than 0");
        }
        this.balance += amount;
        this.totalCredited += amount;
        this.updatedAt = Instant.now();
    }

    public void debit(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Debit amount must be greater than 0");
        }
        if (this.balance < amount) {
            throw new InsufficientPointsException(userId, balance, amount);
        }
        this.balance -= amount;
        this.totalDebited += amount;
        this.updatedAt = Instant.now();
    }
}
