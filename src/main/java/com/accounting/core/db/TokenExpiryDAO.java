package com.accounting.core.db;

import com.accounting.core.entities.views.TokenExpiryView;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import java.util.List;

public interface TokenExpiryDAO {

    @SqlQuery("SELECT t.txId\n" +
            "        ,t.txReference\n" +
            "        ,t.accountId\n" +
            "        ,a.accountCode\n" +
            "        ,tk.expiryDate\n" +
            "    FROM token tk\n" +
            "    JOIN tx t USING (txId)\n" +
            "    JOIN config.account a USING (accountId)\n" +
            "   WHERE COALESCE(:txId, t.txId) = t.txId\n" +
            "     AND (   (:recovery = false AND tk.expiryDate = DATE_TRUNC('day', now()))\n" +
            "          OR (:recovery = true  AND tk.expiryDate <= DATE_TRUNC('day', now())))\n" +
            "     AND 'Activated' = (SELECT journalTypeCode\n" +
            "                          FROM journalTx jtx \n" +
            "                          JOIN journal j using (journalId)\n" +
            "                          JOIN config.journalType jt using (journalTypeId)\n" +
            "                         WHERE jtx.txId = t.txId\n" +
            "                      ORDER BY j.bookingDate desc\n" +
            "                         LIMIT 1)" +
            "ORDER BY t.accountId")
    @RegisterBeanMapper(TokenExpiryView.class)
    List<TokenExpiryView> getExpiredTokens(boolean recovery, Long txId);
}
