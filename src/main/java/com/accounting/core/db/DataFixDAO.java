package com.accounting.core.db;

import com.accounting.core.entities.views.JournalLineView;
import com.accounting.core.entities.views.JournalTxView;
import com.accounting.core.jobs.entities.CardBackFill;
import com.accounting.core.jobs.entities.PaymentView;
import com.accounting.core.jobs.entities.TxJournalLineBatch;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

public interface DataFixDAO {

    // N.B. table 'txFix' needs to be created manually when/if necessary, we do not want it to be part of the released product.

    @SqlQuery("select t.txId\n" +
              "      ,jl.journalLineId\n" +
              "      ,jl.batchId\n" +
              "      ,b.registerTypeId\n" +
              "      ,a.accountCode\n" +
              "      ,accountTypeCode\n" +
              "  from datafix.txFix\n" +
              "  join tx t using (txId)\n" +
              "  join config.txType txt using (txTypeId)\n" +
              "  join journalTx jtx using (txId)\n" +
              "  join journal j using (journalId)\n" +
              "  join journalLine jl using (journalId)\n" +
              "  join batch b using (batchId)\n" +
              "  join config.account a on (a.accountId = b.accountId)\n" +
              "  join config.accountType at using (accountTypeId)\n" +
              " where txt.txTypeCode in ('Token', 'Payment')" +
              "   and t.accountId = :originalAccountTxId")
    @RegisterBeanMapper(TxJournalLineBatch.class)
    List<TxJournalLineBatch> getJournalLineBatchToUpdate(long originalAccountTxId);

    @SqlUpdate("update journalLine jl \n" +
               "   set batchId = :batchId\n" +
               " where journalLineId = :journalLineId")
    void updateJournalLineBatch(long journalLineId, long batchId);

    @SqlUpdate("update tx\n" +
               "   set accountId = :newAccountIdTx\n" +
               "      ,description = 'HAMMER: update accountId '||:originalAccountTxId||' to '||:newAccountIdTx||'. Timestamp: '||now()::timestamp" +
               "  from datafix.txFix\n" +
               " where tx.txId = txFix.txId" +
               "   and tx.accountId = :originalAccountTxId")
    int updateTx(long originalAccountTxId, long newAccountIdTx);

    @SqlUpdate("truncate table datafix.txFix")
    void truncateTxFix();

    @SqlUpdate("CREATE TEMP TABLE IF NOT EXISTS t_journals(journalId BIGINT)")
    void createJournalsTempTable();

    @SqlUpdate("INSERT INTO t_journals(journalId)\n"+
               "SELECT j.journalId\n" +
               "  FROM journalTx \n" +
               "  JOIN journal j USING (journalId)\n" +
               " WHERE txId = (SELECT txId FROM tx WHERE txReference = :txReference)\n" +
               "   AND j.bookingDate < now() - :hoursInterval * INTERVAL '1 hour'")
    int selectJournalsToDelete(String txReference, int hoursInterval);

    @SqlUpdate("DELETE FROM journalLine USING t_journals\n" +
               " WHERE journalLine.journalId = t_journals.journalId")
    int deleteJournalLines();

    @SqlUpdate("DELETE FROM journalTx USING t_journals\n" +
               " WHERE journalTx.journalId = t_journals.journalId")
    int deleteJournalTx();

    @SqlUpdate("DELETE FROM journal USING t_journals\n" +
               " WHERE journal.journalId = t_journals.journalId")
    int deleteJournal();

    default int[] deleteJournals() {
        int [] ret = new int[3];
        ret[0] = deleteJournalLines();
        ret[1] = deleteJournalTx();
        ret[2] = deleteJournal();
        return ret;
    }

    @SqlUpdate("truncate table t_journals")
    void truncateTJournals();

    // "Activated" journal fees data fix:
    // 1) remove 'FeeReceivable' journal line
    // 2) make 'Fee' amounts be Debit rather than credit
    // 3) add fees amount to OpenBalance journal line
    @SqlQuery(" SELECT j.journalId\n" +
               "  FROM journal j\n" +
               "  JOIN config.journalType jt USING (journalTypeId)\n" +
               "  JOIN journalLine jl USING (journalId)\n" +
               "  JOIN batch b USING (batchId)\n" +
               "  JOIN config.registerType rt USING (registerTypeId)\n" +
               "  JOIN journalTx jtx USING (journalId)\n" +
               "  JOIN tx t USING (txId)\n" +
               "  JOIN config.account a ON (a.accountId = t.accountId)\n" +
               " WHERE a.accountCode = :accountCode\n" +
               "   AND rt.registerTypeCode = 'OpenBalance'\n" +
               "   AND jt.journalTypeCode = 'Activated'\n" +
               "   AND j.bookingDate >= TO_DATE(:startDate, 'YYYY-MM-DD')\n" +
               "   AND abs(jl.quantity)  IN (5000, 10000,15000)\n" +
               " ORDER BY j.bookingDate")
    List<Long> selectActivatedJournalsToFix(String accountCode, String startDate);

    @SqlQuery("  SELECT jl.journalLineId\n" +
              "        ,rt.registerTypeCode as registerType\n" +
              "        ,jl.quantity\n" +
              "    FROM journalLine jl \n" +
              "    JOIN batch b USING (batchId)\n" +
              "    JOIN config.registerType rt USING (registerTypeId)\n" +
              "   WHERE jl.journalId = :journalId\n" +
              "ORDER BY rt.registerTypeCode")
    @RegisterBeanMapper(JournalLineView .class)
    List<JournalLineView> selectJournalLinesByJournalId(long journalId);

    @SqlUpdate("UPDATE journalLine \n" +
               "   SET quantity = -quantity\n" +
               " WHERE journalLineId = :journalLineId")
    int switchJournalLineDebitCredit(long journalLineId);

    @SqlUpdate("UPDATE journalLine \n" +
            "   SET quantity = :quantity\n" +
            " WHERE journalLineId = :journalLineId")
    int updateJournalLineQuantity(long journalLineId, long quantity);

    @SqlUpdate("DELETE FROM journalLine \n" +
               " WHERE journalLineId = :journalLineId")
    int deleteJournalLine(long journalLineId);

    // "Activated" journal, missing lines data fix:
    // for accounts with no fees configured, some lines of the "Activated" journal where mistakenly not created.
    @SqlQuery("SELECT j.journalId\n" +
            "        ,t.currencyId\n" +
            "        ,t.accountId\n" +
            "        ,t.quantity\n" +
            "    FROM journal j\n" +
            "    JOIN config.journalType USING (journalTypeId)\n" +
            "    JOIN journalTx jtx USING (journalId)\n" +
            "    JOIN tx t USING (txId)\n" +
            "    JOIN config.account a USING (accountId)\n" +
            "   WHERE accountCode = :accountCode\n" +
            "     AND journalTypeCode = 'Activated'\n" +
            "     AND j.bookingDate >= to_date(:startDate, 'YYYY-MM-DD')\n" +
            "     AND 2 = (SELECT count(1)\n" +
            "                FROM journalLine jl\n" +
            "               WHERE jl.journalId = j.journalId)")
    @RegisterBeanMapper(JournalTxView.class)
    List<JournalTxView> selectActivatedJournalsWithMissingJournalLinesToFix(String accountCode, String startDate);

    @SqlUpdate("INSERT INTO journalLine(journalId, batchId, currencyId, quantity)\n" +
               " VALUES (:journalId, :batchId, :currencyId, :quantity)")
    int insertJournalLine(long journalId, long batchId, int currencyId, long quantity);

    @SqlQuery("SELECT a.accountCode\n" +
            "        ,t.txId\n" +
            "        ,t.quantity as quantityAsLong\n" +
            "        ,currencyCode\n" +
            "        ,t.creationDate as creationDateAsOffset\n" +
            "        ,(SELECT accountCode\n" +
            "            FROM journalTx jtx \n" +
            "            JOIN journal USING (journalId)\n" +
            "            JOIN journalLine USING (journalId)\n" +
            "            JOIN batch b USING (batchId)\n" +
            "            JOIN config.registerType USING (registerTypeId)\n" +
            "            JOIN config.account a USING (accountId)\n" +
            "           WHERE jtx.txId = pr.txId \n" +
            "             AND registerTypeCode = 'BankBalance') bankAccountCode\n" +
            "    FROM datafix.df_payments_to_reverse pr\n" +
            "    JOIN tx t ON (t.txId = pr.txId)\n" +
            "    JOIN config.account a USING (accountId)\n" +
            "    JOIN config.txType txt USING (txTypeId)\n" +
            "    JOIN config.currency c USING (currencyId)" +
            "   WHERE t.txId = COALESCE(:txId, t.txId)")
    @RegisterBeanMapper(PaymentView.class)
    List<PaymentView> getPaymentsJournalToReverse(Long txId);

    @SqlQuery("SELECT i1.accountCode\n" +
            "        ,i1.amount\n" +
            "        ,i1.discount\n" +
            "        ,i1.fee\n" +
            "        ,i1.vat\n" +
            "        ,i1.currencyCode\n" +
            "        ,i1.bookingDate::timestamp as creationDateAsOffset\n" +
            "        ,(CASE WHEN i1.currencyCode in ('EUR', 'USD') THEN 'EURClients' ELSE 'GBPClients1' END) bankAccountCode\n" +
            "    FROM \n" +
            "       (SELECT a.accountCode\n" +
            "              ,amount\n" +
            "              ,discount\n" +
            "              ,fee\n" +
            "              ,vat\n" +
            "              ,(SELECT value\n" +
            "                  FROM config.accountproperty ap \n" +
            "                 WHERE ap.accountId = a.accountId\n" +
            "                   AND ap.code = 'AccountCurrency') currencyCode\n" +
            "              ,bookingDate\n" +
            "          FROM datafix.df_payments_new pn\n" +
            "          JOIN config.account a on (a.accountCode = pn.accountCode)\n" +
            "          JOIN config.accountType at using (accountTypeId)\n" +
            "         WHERE pn.id = COALESCE(:newPaymentId, pn.id)\n" +
            "           AND at.accountTypeCode in ('Client', 'Distributor')) i1")
    @RegisterBeanMapper(PaymentView.class)
    List<PaymentView> getNewPaymentsToSubmit(Long newPaymentId);

    //
    // Create manually the table containing the list of card to backfill
    //    CREATE TABLE datafix.cards_back_fill (
    //    prepaidcardexternalid varchar NOT NULL,
    //    done bool NULL);
    @SqlQuery("SELECT t.quantity\n" +
            "        , t.currencyid\n" +
            "        ,t.txId \n" +
            "        ,prepaidcardexternalid \n" +
            "        ,a.accountCode\n" +
            "        ,at.accountTypeCode\n" +
            "    FROM datafix.cards_back_fill cb\n" +
            "    JOIN token tk USING (prepaidcardexternalid)\n" +
            "    JOIN tx t USING (txId)\n" +
            "    JOIN config.account a USING(accountId)\n" +
            "    JOIN config.accountType at USING (accountTypeId)\n" +
            "    WHERE done = false ")
    @RegisterBeanMapper(CardBackFill.class)
    List<CardBackFill> getCardsToBackFill();
    @SqlUpdate("UPDATE datafix.cards_back_fill SET done = true WHERE prepaidcardexternalid = :cardExternalId ")
    void markCardAsBackFilled(String cardExternalId);

}