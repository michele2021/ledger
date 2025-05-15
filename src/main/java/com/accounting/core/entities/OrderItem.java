package com.accounting.core.entities;

public record OrderItem(
    String price,
    int quantity,
    String id,
    String name,
    String sku) {}