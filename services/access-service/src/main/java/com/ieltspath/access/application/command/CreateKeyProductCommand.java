package com.ieltspath.access.application.command;

import com.ieltspath.access.domain.vo.KeyType;

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
