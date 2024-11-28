package com.accounting.core.entities;

public interface SpecializedTx {

    default boolean isOrder() {
        return false;
    }

    default boolean isCCToken() {
        return false;
    }

    default boolean isCard() {
        return false;
    }
}