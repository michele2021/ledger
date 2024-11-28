package com.accounting.core.dto;

import com.accounting.commons.Constants;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Value
@Builder(toBuilder = true)
public class AccountDTO {

    @NotEmpty
    @Schema(description = "Account code (alphanumeric)", required = true)
    String accountCode;

    @NotNull
    @Schema(description = "Account type code", required = true)
    Constants.AccountType accountTypeCode;

    @Schema(description = "Parent account code")
    String parentAccountCode;

    @Schema(description = "Parent account type code")
    Constants.AccountType parentAccountTypeCode;

    @Schema(description = "Account description", required = true)
    String description;

    @Schema(description = "Account properties")
    List<AccountPropertyDTO> accountProperties;

    @Valid
    @Schema(description = "Client Account details")
    ClientDetailsDTO clientDetails;

    @Schema(description = "Is this account a test account")
    boolean testAccount;

    @Schema(description = "Create also a corresponding _TEST account (only if this account is not a test account)")
    boolean createTestAccount;

    @Schema(description = "User that is creating the account")
    String userName;
}
