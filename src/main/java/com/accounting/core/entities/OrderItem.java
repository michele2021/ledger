package com.accounting.core.entities;

import lombok.Builder;

@Builder
public record OrderItem(
    String price,
    int quantity,
    String title,
    String sku) {}