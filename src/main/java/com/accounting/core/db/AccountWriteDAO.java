package com.accounting.core.db;

import com.accounting.commons.Constants;
import com.accounting.commons.entities.Account;
import com.accounting.commons.entities.AccountBase;
import com.accounting.commons.entities.AccountProperty;
import com.accounting.commons.entities.TokenOriginal;
import com.accounting.commons.utils.AccountingUtils;
import com.accounting.core.dto.ClientDetailsDTO;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;

import java.util.List;

public interface AccountWriteDAO {

    @SqlQuery("SELECT a.accountId, a.accountCode, a.accountTypeId" +
            "  FROM config.tokenHolder th" +
            "  JOIN config.account a USING (accountId)" +
            " WHERE th.email = :email")
    @RegisterBeanMapper(Account.class)
    Account getTokenHolderAccount(String email);

    @SqlBatch("INSERT INTO config.accountProperty(accountId, code, value) VALUES (:accountId, :code, :value)")
    void insertAccountProperties(@BindBean List<AccountProperty> properties, long accountId);

    @SqlUpdate("INSERT INTO config.account(accountCode, parentId, accountTypeId, status, description, test, createdBy) " +
            "VALUES (:accountCode, :parentId, :accountTypeId, 'ACTIVE', :description, :test, :createdBy)")
    @RegisterBeanMapper(AccountBase.class)
    @GetGeneratedKeys
    AccountBase insertAccount(String accountCode, int accountTypeId, Long parentId, String description, Boolean test, String createdBy);

    @SqlUpdate("INSERT INTO config.client(accountId, streetAddress, email, telephoneNumber, cityName, country, contactRef, postalCode) " +
            "VALUES (:accountId, :streetAddress, :email, :telephoneNumber, :cityName, :country, :contactRef, :postalCode)")
    void insertClientDetails(long accountId, @BindBean ClientDetailsDTO clientDetailsDTO);


    @Transaction
    default long createAccount(Account account, boolean createTestAccount, ClientDetailsDTO clientDetailsDTO) {
        long newAccountId = createAccountInternal(account);
        createClientDetailsIfNecessary(newAccountId, clientDetailsDTO);
        createTestAccountIfNecessary(account, createTestAccount);
        return newAccountId;
    }

    default long createAccountInternal(Account account) {
        AccountBase newAccount = insertAccount(account.getAccountCode(), account.getAccountTypeId(),
                account.getParentId(), account.getDescription(), account.isTest(), account.getCreatedBy());
        if (!AccountingUtils.isEmptyOrNull(account.getProperties())) {
            insertAccountProperties(account.getProperties(), newAccount.getAccountId());
        }
        return newAccount.getAccountId();
    }

    default void createTestAccountIfNecessary(Account account, boolean createTestAccount) {
        // for Client and Distributor account types we can create a corresponding _TEST account, if required.
        boolean addTest = createTestAccount && Constants.isClient(account.getAccountTypeCode());
        if (!addTest) {
            return;
        }
        // N.B. on test accounts we do not set the parentId
        Account testAccount = Account.builder()
                .accountCode(account.getAccountCode().concat("_TEST"))
                .accountTypeCode(account.getAccountTypeCode())
                .accountTypeId(account.getAccountTypeId())
                .properties(account.getProperties())
                .description(account.getDescription())
                .test(true)
                .createdBy(account.getCreatedBy())
                .build();
        createAccountInternal(testAccount);
    }

    default void createClientDetailsIfNecessary(long accountId, ClientDetailsDTO clientDetailsDTO) {
        if(clientDetailsDTO == null) {
            return;
        }
        insertClientDetails(accountId, clientDetailsDTO);
    }

    @SqlUpdate("INSERT INTO config.tokenHolder(accountId, firstName, lastName, email) " +
               "VALUES (:accountId, :recipientName, null, :recipientEmail)")
    void createTokenHolder(long accountId, @BindBean TokenOriginal token);

    @Transaction
    @RegisterBeanMapper(Account.class)
    default AccountBase createTokenHolderAccount(String accountCode, Long parentId, int accountTypeId, TokenOriginal token) {
        AccountBase account = insertAccount(accountCode, accountTypeId, parentId, null, false, null);
        createTokenHolder(account.getAccountId(), token);
        return account;
    }
}
