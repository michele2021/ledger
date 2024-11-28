package com.accounting.core.db;

import com.accounting.core.entities.MismatchedToken;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface MismatchedTokensDAO {

    @SqlUpdate("INSERT INTO mismatchedToken (txId, tokenTxId, txReference, status, errorMessage) VALUES (:txId, :tokenTxId, :txReference, :status, :errorMessage)")
    void createMismatchedToken(@BindBean MismatchedToken mismatchedToken);
}
