package com.accounting.core.dto;

import com.accounting.commons.Constants;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AccountPropertyDTO {
    Constants.AccountPropertyCode name;
    String value;
}
