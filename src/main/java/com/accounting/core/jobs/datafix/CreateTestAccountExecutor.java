package com.accounting.core.jobs.datafix;

import com.accounting.commons.entities.Account;
import com.accounting.commons.repository.AccountRepoIf;
import com.accounting.core.db.AccountWriteDAO;
import com.accounting.core.exceptions.JobProcessingException;
import com.accounting.core.services.RepositoryHelper;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreateTestAccountExecutor {

    private final AccountWriteDAO accountWriteDAO;
    private final AccountRepoIf accountRepo;

    @Inject
    public CreateTestAccountExecutor(RepositoryHelper repositoryHelper) {
        this.accountWriteDAO = repositoryHelper.getAccountWriteDAO();
        this.accountRepo = repositoryHelper.getAccountRepo();
    }

    public void execute(String accountCode, String accountTypeCode) throws JobProcessingException {
        Account account = accountRepo.getByCodeAndType(accountCode, accountTypeCode);
        if (account == null) {
            throw new JobProcessingException("Account not found for account code '" + accountCode + "' and type '" + accountTypeCode + "'");
        }
        // check if the test account already exist.
        String testAccountCode = account.getAccountCode().concat("_TEST");
        Account a = accountRepo.getByCodeAndType(testAccountCode, accountTypeCode);
        if (a != null) {
            throw new JobProcessingException("Test Account with code '" + testAccountCode + "' already existent!");
        }
        Account testAccount = Account.builder()
                .accountCode(testAccountCode)
                .parentId(null)
                .test(true)
                .accountTypeId(account.getAccountTypeId())
                .accountTypeCode(account.getAccountTypeCode())
                .description(account.getDescription() + " TEST")
                .propertiesMap(account.getPropertiesMap())
                .build();
        accountWriteDAO.createAccount(testAccount, false, null);
        log.info("Test account created. Code: {}, Type: {}.", testAccountCode, testAccount.getAccountTypeCode());
    }
}