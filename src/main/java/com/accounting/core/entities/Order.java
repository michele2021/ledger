package com.accounting.core.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order implements SpecializedTx {

    private long orderId;
    private Long externalId;
    private String orderType;
    private String orderRef;
    private String sku;
    private String title;
    private String cardProgram;

    @Override
    public boolean isOrder() {
        return true;
    }
}
