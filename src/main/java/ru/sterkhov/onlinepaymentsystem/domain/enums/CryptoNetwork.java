package ru.sterkhov.onlinepaymentsystem.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Поддерживаемые криптовалютные сети.
 */
@Getter
@RequiredArgsConstructor
public enum CryptoNetwork {

    /** Сеть Bitcoin. */
    BTC("BTC", "Bitcoin", 8),

    /** Сеть Ethereum. */
    ETH("ETH", "Ethereum", 18),

    /** Сеть Solana. */
    SOL("SOL", "Solana", 9),

    /** Сеть TON (Toncoin). */
    TON("TON", "Toncoin", 9),

    /** USDT в сети TON. */
    USDT_TON("USDT", "USDT (TON)", 6),

    /** USDT в сети TRON (TRC-20). */
    USDT_TRC20("USDT", "USDT (TRC-20)", 6),

    /** Tether Gold (XAUT). */
    XAUT("XAUT", "Tether Gold", 6),
    ;

    /** Код валюты. */
    private final String currency;

    /** Отображаемое имя сети. */
    private final String displayName;

    /** Количество знаков после запятой. */
    private final int decimals;
}
