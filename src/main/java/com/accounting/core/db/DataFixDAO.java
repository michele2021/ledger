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

    @SqlQuery("""
            select t.txId
                  ,jl.journalLineId
                  ,jl.batchId
                  ,b.registerTypeId
                  ,a.accountCode
                  ,accountTypeCode
              from datafix.txFix
              join tx t using (txId)
              join config.txType txt using (txTypeId)
              join journalTx jtx using (txId)
              join journal j using (journalId)
              join journalLine jl using (journalId)
              join batch b using (batchId)
              join config.account a on (a.accountId = b.accountId)
              join config.accountType at using (accountTypeId)
             where txt.txTypeCode in ('Token', 'Payment')\
               and t.accountId = :originalAccountTxId""")
    @RegisterBeanMapper(TxJournalLineBatch.class)
    List<TxJournalLineBatch> getJournalLineBatchToUpdate(long originalAccountTxId);

    @SqlUpdate("""
            update journalLine jl\s
               set batchId = :batchId
             where journalLineId = :journalLineId""")
    void updateJournalLineBatch(long journalLineId, long batchId);

    @SqlUpdate("""
            update tx
               set accountId = :newAccountIdTx
                  ,description = 'HAMMER: update accountId '||:originalAccountTxId||' to '||:newAccountIdTx||'. Timestamp: '||now()::timestamp\
              from datafix.txFix
             where tx.txId = txFix.txId\
               and tx.accountId = :originalAccountTxId""")
    int updateTx(long originalAccountTxId, long newAccountIdTx);

    @SqlUpdate("truncate table datafix.txFix")
    void truncateTxFix();

    @SqlUpdate("CREATE TEMP TABLE IF NOT EXISTS t_journals(journalId BIGINT)")
    void createJournalsTempTable();

    @SqlUpdate("""
            INSERT INTO t_journals(journalId)
            SELECT j.journalId
              FROM journalTx\s
              JOIN journal j USING (journalId)
             WHERE txId = (SELECT txId FROM tx WHERE txReference = :txReference)
               AND j.bookingDate < now() - :hoursInterval * INTERVAL '1 hour'""")
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
    @SqlQuery("""
             SELECT j.journalId
              FROM journal j
              JOIN config.journalType jt USING (journalTypeId)
              JOIN journalLine jl USING (journalId)
              JOIN batch b USING (batchId)
              JOIN config.registerType rt USING (registerTypeId)
              JOIN journalTx jtx USING (journalId)
              JOIN tx t USING (txId)
              JOIN config.account a ON (a.accountId = t.accountId)
             WHERE a.accountCode = :accountCode
               AND rt.registerTypeCode = 'OpenBalance'
               AND jt.journalTypeCode = 'Activated'
               AND j.bookingDate >= TO_DATE(:startDate, 'YYYY-MM-DD')
               AND abs(jl.quantity)  IN (5000, 10000,15000)
             ORDER BY j.bookingDate\
            """)
    List<Long> selectActivatedJournalsToFix(String accountCode, String startDate);

    @SqlQuery("""
              SELECT jl.journalLineId
                    ,rt.registerTypeCode as registerType
                    ,jl.quantity
                FROM journalLine jl\s
                JOIN batch b USING (batchId)
                JOIN config.registerType rt USING (registerTypeId)
               WHERE jl.journalId = :journalId
            ORDER BY rt.registerTypeCode""")
    @RegisterBeanMapper(JournalLineView .class)
    List<JournalLineView> selectJournalLinesByJournalId(long journalId);

    @SqlUpdate("""
            UPDATE journalLine\s
               SET quantity = -quantity
             WHERE journalLineId = :journalLineId""")
    int switchJournalLineDebitCredit(long journalLineId);

    @SqlUpdate("""
            UPDATE journalLine\s
               SET quantity = :quantity
             WHERE journalLineId = :journalLineId""")
    int updateJournalLineQuantity(long journalLineId, long quantity);

    @SqlUpdate("DELETE FROM journalLine \n" +
               " WHERE journalLineId = :journalLineId")
    int deleteJournalLine(long journalLineId);

    // "Activated" journal, missing lines data fix:
    // for accounts with no fees configured, some lines of the "Activated" journal where mistakenly not created.
    @SqlQuery("""
            SELECT j.journalId
                    ,t.currencyId
                    ,t.accountId
                    ,t.quantity
                FROM journal j
                JOIN config.journalType USING (journalTypeId)
                JOIN journalTx jtx USING (journalId)
                JOIN tx t USING (txId)
                JOIN config.account a USING (accountId)
               WHERE accountCode = :accountCode
                 AND journalTypeCode = 'Activated'
                 AND j.bookingDate >= to_date(:startDate, 'YYYY-MM-DD')
                 AND 2 = (SELECT count(1)
                            FROM journalLine jl
                           WHERE jl.journalId = j.journalId)""")
    @RegisterBeanMapper(JournalTxView.class)
    List<JournalTxView> selectActivatedJournalsWithMissingJournalLinesToFix(String accountCode, String startDate);

    @SqlUpdate("INSERT INTO journalLine(journalId, batchId, currencyId, quantity)\n" +
               " VALUES (:journalId, :batchId, :currencyId, :quantity)")
    int insertJournalLine(long journalId, long batchId, int currencyId, long quantity);

    @SqlQuery("""
            SELECT a.accountCode
                    ,t.txId
                    ,t.quantity as quantityAsLong
                    ,currencyCode
                    ,t.creationDate as creationDateAsOffset
                    ,(SELECT accountCode
                        FROM journalTx jtx\s
                        JOIN journal USING (journalId)
                        JOIN journalLine USING (journalId)
                        JOIN batch b USING (batchId)
                        JOIN config.registerType USING (registerTypeId)
                        JOIN config.account a USING (accountId)
                       WHERE jtx.txId = pr.txId\s
                         AND registerTypeCode = 'BankBalance') bankAccountCode
                FROM datafix.df_payments_to_reverse pr
                JOIN tx t ON (t.txId = pr.txId)
                JOIN config.account a USING (accountId)
                JOIN config.txType txt USING (txTypeId)
                JOIN config.currency c USING (currencyId)\
               WHERE t.txId = COALESCE(:txId, t.txId)""")
    @RegisterBeanMapper(PaymentView.class)
    List<PaymentView> getPaymentsJournalToReverse(Long txId);

    @SqlQuery("""
            SELECT i1.accountCode
                    ,i1.amount
                    ,i1.discount
                    ,i1.fee
                    ,i1.vat
                    ,i1.currencyCode
                    ,i1.bookingDate::timestamp as creationDateAsOffset
                    ,(CASE WHEN i1.currencyCode in ('EUR', 'USD') THEN 'EURClients' ELSE 'GBPClients1' END) bankAccountCode
                FROM\s
                   (SELECT a.accountCode
                          ,amount
                          ,discount
                          ,fee
                          ,vat
                          ,(SELECT value
                              FROM config.accountproperty ap\s
                             WHERE ap.accountId = a.accountId
                               AND ap.code = 'AccountCurrency') currencyCode
                          ,bookingDate
                      FROM datafix.df_payments_new pn
                      JOIN config.account a on (a.accountCode = pn.accountCode)
                      JOIN config.accountType at using (accountTypeId)
                     WHERE pn.id = COALESCE(:newPaymentId, pn.id)
                       AND at.accountTypeCode in ('Client', 'Company')) i1""")
    @RegisterBeanMapper(PaymentView.class)
    List<PaymentView> getNewPaymentsToSubmit(Long newPaymentId);

    //
    // Create manually the table containing the list of card to backfill
    //    CREATE TABLE datafix.cards_back_fill (
    //    prepaidcardexternalid varchar NOT NULL,
    //    done bool NULL);
    @SqlQuery("""
            SELECT t.quantity
                    , t.currencyid
                    ,t.txId\s
                    ,prepaidcardexternalid\s
                    ,a.accountCode
                    ,at.accountTypeCode
                FROM datafix.cards_back_fill cb
                JOIN token tk USING (prepaidcardexternalid)
                JOIN tx t USING (txId)
                JOIN config.account a USING(accountId)
                JOIN config.accountType at USING (accountTypeId)
                WHERE done = false\s""")
    @RegisterBeanMapper(CardBackFill.class)
    List<CardBackFill> getCardsToBackFill();
    @SqlUpdate("UPDATE datafix.cards_back_fill SET done = true WHERE prepaidcardexternalid = :cardExternalId ")
    void markCardAsBackFilled(String cardExternalId);

}