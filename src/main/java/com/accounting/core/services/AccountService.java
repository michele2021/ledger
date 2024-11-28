package com.accounting.core.services;

import com.accounting.core.dto.AccountDTO;

public interface AccountService {

    long createAccount(AccountDTO accountDTO);
}
