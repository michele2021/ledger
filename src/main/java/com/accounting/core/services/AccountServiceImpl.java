package com.accounting.core.services;

import com.accounting.commons.entities.Account;
import com.accounting.commons.entities.AccountProperty;
import com.accounting.commons.entities.AccountType;
import com.accounting.commons.exceptions.WebValidationException;
import com.accounting.commons.repository.AccountRepoIf;
import com.accounting.commons.repository.AccountTypeRepoIf;
import com.accounting.commons.utils.TextUtils;
import com.accounting.core.db.AccountWriteDAO;
import com.accounting.core.dto.AccountDTO;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class AccountServiceImpl implements AccountService {

    private final AccountWriteDAO accountWriteDAO;
    private final AccountTypeRepoIf accountTypeRepo;
    private final AccountRepoIf accountRepo;
    private final RepositoryHelper repositoryHelper;

    @Inject
    public AccountServiceImpl(RepositoryHelper repositoryHelper) {
        this.repositoryHelper = repositoryHelper;
        this.accountWriteDAO = repositoryHelper.getAccountWriteDAO();
        this.accountRepo = repositoryHelper.getAccountRepo();
        this.accountTypeRepo = repositoryHelper.getAccountTypeRepo();
    }

    @Override
    public long createAccount(@NotNull AccountDTO accountDTO) {
        log.debug("create Account with DTO: " + accountDTO);

        // validations
        if(!TextUtils.isAlphanumericUnderscore(accountDTO.getAccountCode())) {
            throw validationError("accountCode", "The accountCode has to be alphanumeric.");
        }
        if(accountDTO.isTestAccount() && accountDTO.isCreateTestAccount()) {
            throw validationError("createTestAccount", "A corresponding test account can be created only if the current account is not a test account itself already.");
        }
        AccountType accountType = accountTypeRepo.get(accountDTO.getAccountTypeCode().toString());
        Long parentId = null;
        if(!Strings.isNullOrEmpty(accountDTO.getParentAccountCode())) {
            if(Objects.isNull(accountDTO.getParentAccountTypeCode())) {
                throw validationError("parentAccountTypeCode", "Field must have a value");
            }
            Account parentAccount = accountRepo.getByCodeAndType(accountDTO.getParentAccountCode(), accountDTO.getParentAccountTypeCode().toString());
            if (Objects.isNull(parentAccount)) {
                throw validationError("parentAccount", "Parent account not found with code: " + accountDTO.getParentAccountCode() + " and type: " + accountDTO.getParentAccountTypeCode());
            }
            parentId = parentAccount.getAccountId();
        }
        Account account = accountRepo.getByCodeAndType(accountDTO.getAccountCode(), accountDTO.getAccountTypeCode().toString());
        if(account != null) {
            throw new WebValidationException("The specified account already exists!");
        }
        // validations end

        // map from AccountDTO to Account.
        List<AccountProperty> accountProperties = null;
        if(accountDTO.getAccountProperties() != null) {
            accountProperties = accountDTO.getAccountProperties()
                    .stream()
                    .map(e -> AccountProperty.builder().code(e.getName()).value(e.getValue()).build())
                    .collect(Collectors.toList());
        }
        account = Account.builder()
                .accountCode(accountDTO.getAccountCode())
                .accountTypeCode(accountType.getCode())
                .accountTypeId(accountType.getAccountTypeId())
                .parentId(parentId)
                .properties(accountProperties)
                .description(accountDTO.getDescription())
                .test(accountDTO.isTestAccount())
                .createdBy(accountDTO.getUserName())
                .build();

        long accountId = accountWriteDAO.createAccount(account, accountDTO.isCreateTestAccount(), accountDTO.getClientDetails());
        // refresh the account caches
        repositoryHelper.refreshAccountCaches(accountId);

        return accountId;
    }


    private WebValidationException validationError(String fieldName, String validationMessage) {
        StringBuilder sb = new StringBuilder();

        if (StringUtils.isNotEmpty(fieldName)) {
            sb.append("Field message.").append(fieldName).append(" is invalid! ");
        }
        sb.append(validationMessage);

        if (log.isTraceEnabled()) {
            log.trace("Validation error: " + sb);
        }
        return new WebValidationException(sb.toString());
    }

}
