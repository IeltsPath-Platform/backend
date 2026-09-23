package com.group01.access.application.command;

import com.group01.access.domain.vo.KeyType;

import java.util.UUID;

public record CreateKeyProductCommand(
        String code,
        String name,
        KeyType keyType,
        Integer pointsAmount,
        UUID planId,
        Integer premiumDays,
        Integer humanGradingCredits
        ) {

}
