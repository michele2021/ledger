package com.accounting.core.exceptions;

import com.accounting.commons.exceptions.AccountingException;

public class AccountingTemplateException extends AccountingException {
    public AccountingTemplateException(String message) {
        super(message);
    }

    public AccountingTemplateException(String message, Throwable cause) {
        super(message, cause);
    }
}
