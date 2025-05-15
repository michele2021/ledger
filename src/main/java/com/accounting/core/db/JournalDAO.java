package com.accounting.core.db;

import com.accounting.commons.Constants;
import com.accounting.commons.entities.Amount;
import com.accounting.commons.utils.AccountingUtils;
import com.accounting.core.db.reducers.JournalDTOReducer;
import com.accounting.core.entities.*;
import com.accounting.core.entities.composite.TxJournal;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.*;

import java.time.OffsetDateTime;
import java.util.List;

@RegisterBeanMapper(value = JournalDTO.class, prefix = "j")
@RegisterBeanMapper(value = JournalLineDTO.class, prefix = "jl")
public interface JournalDAO {

    @SqlUpdate("insert into journal (journalTypeId, bookingDate) values (\n" +
            " :journalTypeId, now())")
    @GetGeneratedKeys("journalid")
    long createJournalNow(int journalTypeId);

    @SqlUpdate("insert into journal (journalTypeId, bookingDate) values (\n" +
            " :journalTypeId, coalesce(:bookingDate, now()))")
    @GetGeneratedKeys("journalid")
    long createJournalAt(int journalTypeId, OffsetDateTime bookingDate);

    @SqlUpdate("insert into journal (journalTypeId, bookingDate) values (\n" +
            " :journalTypeId, :bookingDate)")
    @GetGeneratedKeys("journalid")
    long createJournal(@BindBean Journal j);

    @SqlUpdate("INSERT INTO tx(txTypeId, accountId, parentTxId, txVariantId, currencyId, quantity, txReference, creationDate, description) VALUES (\n" +
            " :txTypeId, :accountId, :parentTxId, :txVariantId, :amount.currencyId, :amount.quantity, :txReference, :creationDate, :description)")
    @GetGeneratedKeys("txid")
    long createTx(@BindBean Tx tx);

    @SqlUpdate("INSERT INTO tx(txTypeId, accountId, parentTxId, txVariantId, currencyId, quantity, txReference, creationDate, description) VALUES" +
            " (:txTypeId, :accountId, :parentTxId, :txVariantId, :amount.currencyId, :amount.quantity, :txReference, :creationDate, :description)" +
            " ON CONFLICT (txReference) DO UPDATE SET quantity = EXCLUDED.quantity RETURNING txid")
    @GetGeneratedKeys("txid")
    long createTxIfNotExists(@BindBean Tx tx);

    @SqlUpdate("insert into journalTx(journalId, txId) values (\n" +
            " :journalId, :txId)")
    void createJournalTx(long journalId, long txId);

    @SqlUpdate("insert into journalLine(journalId, batchId, currencyId, quantity) values (\n" +
            " :journalId, :batchId, :currencyId, :quantity)")
    void createJournalLine(@BindBean JournalLine jl);

    @SqlBatch("insert into journalLine(journalId, batchId, currencyId, quantity) values (\n" +
            " :journalId, :batchId, :currencyId, :quantity)")
    void createJournalLinesBulk(@BindBean List<JournalLine> jlines);

    // Batch management begin.
    @SqlUpdate("insert into batch(registerTypeId, accountId, batchNumber, periodBeginDate, periodEndDate) values (\n" +
            " :registerTypeId, :accountId, :batchNumber, :periodBeginDate, :periodEndDate)")
    @GetGeneratedKeys("batchid")
    long createBatch(@BindBean Batch b);

    @SqlQuery("select batchId, registerTypeId, accountId, batchNumber, periodBeginDate, periodEndDate " +
            "  from batch b " +
            " where b.registerTypeId = :registerTypeId " +
            "   and b.accountId = :accountId " +
            "   and b.closedDate is null " +
            "   and now() between b.periodBeginDate and coalesce(b.periodEndDate, now() + interval '1 day')")
    @RegisterBeanMapper(Batch.class)
    Batch getAvailableBatch(int registerTypeId, long accountId);
    // Batch management end.

    @SqlUpdate("""
               insert into orders(txId, platformId, orderNumber, billingAddressCountry, shippingAddressCountry)
                values (:txId, :platformId, :orderNumber, :billingAddressCountry, :shippingAddressCountry)""")
    void createOrders(@BindMethods OrderDTO order);

    @SqlBatch("""
             insert into orderLine(orderId, price, quantity, id, name, sku)
                values (:orderId, :price, :quantity, :id, :name, :sku)""")
    void createOrderItems(@BindMethods OrderItem... items);

    default void createOrder(OrderDTO order) {
        createOrders(order);
        var items = order.orderItems();
        if(!AccountingUtils.isEmptyOrNull(items)) {
            createOrderItems(items.toArray(OrderItem[]::new));
        }
    }

    @SqlQuery("""
              select t.txId
                    ,t.accountId
                    ,a.accountCode
                    ,txTypeCode
                    ,t.parentTxId
                    ,txReference
                    ,t.creationDate
                    ,t.quantity
                    ,t.currencyId
                    ,c.currencyCode
                    ,jt.journalTypeCode
                    ,j.journalId
                    ,j.bookingDate
                from orders o\
                join tx t using (txId)\
                join config.account a using (accountId)\
                join config.currency c using (currencyId)\
                join config.txType txt using (txTypeId)\
                join journalTx jtx using (txId) \
                join journal j using (journalId) \
                join config.journalType jt using (journalTypeId) \
               where o.externalId = :orderId \
            order by j.bookingDate desc \
             limit 1;""")
    @RegisterRowMapper(TxRowMapper.class)
    TxJournal getTxJournalByOrderId(long orderId);

    @SqlQuery("select txId from tx where txReference = :txReference")
    Long getTxIdByReference(String txReference);

    @SqlQuery("select txId, txTypeId, accountId, parentTxId, txVariantId, txReference, quantity, currencyId, creationDate " +
            "    from tx " +
            "   where txReference = :txReference")
    @RegisterBeanMapper(Tx.class)
    Tx getTxByReference(String txReference);

    @SqlQuery("SELECT txId, txTypeId, accountId, parentTxId, txVariantId, txReference, quantity, currencyId, creationDate, " +
            "         j.journalId, j.bookingDate, jt.journalTypeCode, tk.type as tokenType " +
            "    FROM tx " +
            "  LEFT JOIN token tk USING (txId) " +
            "    JOIN journalTx jtx USING (txId) " +
            "    JOIN journal j USING (journalId) " +
            "    JOIN config.journalType jt USING (journalTypeId) " +
            "   WHERE txReference = :txReference " +
            " ORDER BY j.bookingDate desc " +
            "   LIMIT 1;")
    @RegisterBeanMapper(TxStatus.class)
    TxStatus getTxStatusByReference(String txReference);

    @SqlQuery("SELECT txId" +
            "        ,txTypeId" +
            "        ,accountId" +
            "        ,parentTxId" +
            "        ,txVariantId" +
            "        ,txReference" +
            "        ,quantity" +
            "        ,currencyId" +
            "        ,creationDate" +
            "        ,j.journalId" +
            "        ,j.bookingDate" +
            "        ,jt.journalTypeCode " +
            "    FROM tx " +
            "    JOIN journalTx jtx USING (txId) " +
            "    JOIN journal j USING (journalId) " +
            "    JOIN config.journalType jt USING (journalTypeId) " +
            "   WHERE txReference = :txReference " +
            "     AND accountId = :accountId " +
            "ORDER BY j.bookingDate desc " +
            "   LIMIT 1")
    @RegisterBeanMapper(TxStatus.class)
    TxStatus getTxStatusByReferenceAndAccountId(String txReference, long accountId);

    // TODO possibly improve so to avoid duplicated sql (getTxStatusByReferenceAndAccountId and getTxStatusWithIssuerAccount)
    @SqlQuery("  SELECT txId" +
            "          ,txTypeId" +
            "          ,t.accountId" +
            "          ,parentTxId" +
            "          ,txVariantId" +
            "          ,txReference" +
            "          ,t.quantity" +
            "          ,t.currencyId" +
            "          ,t.creationDate " +
            "          ,j.journalId" +
            "          ,j.bookingDate" +
            "          ,jt.journalTypeCode" +
            "          ,(SELECT a.accountId " +
            "              FROM journalLine jl " +
            "              JOIN batch b USING (batchId) " +
            "              JOIN config.registerType rt USING (registerTypeId) " +
            "              JOIN config.account a USING (accountId) " +
            "              JOIN config.accountType at USING (accountTypeId) " +
            "             WHERE jl.journalId = j.journalId " +
            "               AND rt.registerTypeCode = 'CardsPayablePending' " +
            "               AND at.accountTypeCode = 'Issuer') as issuerAccountId " +
            "      FROM tx t " +
            "      JOIN journalTx jtx USING (txId) " +
            "      JOIN journal j USING (journalId) " +
            "      JOIN config.journalType jt USING (journalTypeId) " +
            "     WHERE t.txReference = :txReference " +
            "       AND t.accountId = :accountId " +
            "  ORDER BY j.bookingDate desc " +
            "     LIMIT 1")
    @RegisterBeanMapper(TxStatus.class)
    TxStatus getTxStatusWithIssuerAccountByReferenceAndAccountId(String txReference, long accountId);

    @SqlQuery("""
            SELECT j.journalId j_journalId
                    ,j.journalTypeId j_journalTypeId
                    ,j.bookingDate j_bookingDate
                    ,jl.journalId jl_journalId\s
                    ,jl.journalLineId jl_journalLineId\s
                    ,jl.batchId jl_batchId\s
                    ,jl.quantity jl_quantity\s
                    ,jl.currencyId jl_currencyId\s
                    ,b.registerTypeId jl_registerTypeId\s
                    ,rt.registerTypeCode jl_registerTypeCode\s
                    ,b.accountId jl_accountId\s
                FROM journal j\s
                JOIN journalLine jl USING (journalId)
                JOIN batch b USING (batchId)
                JOIN config.registerType rt using (registerTypeId)
               WHERE j.journalId = (SELECT j.journalId
                                      FROM tx t
                                      JOIN journalTx jtx USING (txId)\s
                                      JOIN journal j USING (journalId)\s
                                      JOIN config.journalType jt USING (journalTypeId)\s
                                     WHERE t.txReference = :txReference\s
                                       AND jt.journalTypeCode = :journalTypeCode\s
                                  ORDER BY j.bookingDate DESC
                                     LIMIT 1)""")
    @UseRowReducer(JournalDTOReducer.class)
    JournalDTO getLatestJournalByTxReferenceAndJournalTypeCode(String txReference, String journalTypeCode);

    @SqlQuery("select txId, txTypeId, accountId, parentTxId, txVariantId, txReference, quantity, currencyId, creationDate " +
            "  from tx " +
            " where parentTxId = :parentTxId")
    @RegisterBeanMapper(Tx.class)
    List<Tx> getTxsByParentTxId(long parentTxId);

    @SqlQuery("SELECT tx.txId, txTypeId," +
            "     accountId, parentTxId, txVariantId," +
            "     txReference, quantity, currencyId, creationDate, tk.type as tokenType " +
            "  FROM tx " +
            "     JOIN token tk on tx.parentTxId = tk.txid " +
            "     JOIN config.txType txType USING (txTypeId) " +
            "  WHERE parentTxId = :parentTxId AND txType.txTypeCode = :txType ")
    @RegisterBeanMapper(TxStatus.class)
    List<TxStatus> getTokenTxByParentIdAndTxTypeId(Long parentTxId, Constants.TxType txType);

    @SqlQuery("SELECT CAST(count(1) AS BIT) " +
            "  FROM tx t" +
            "      JOIN journalTx jtx USING (txId)" +
            "      JOIN journal j USING (journalId)" +
            "      JOIN config.journalType jt USING (journalTypeId)" +
            "  WHERE t.txId = :txId AND jt.journalTypeCode = :journalType")
    boolean doesJournalExist(long txId, Constants.JournalType journalType);

    @SqlQuery("SELECT j.journalId, j.journalTypeId, j.bookingDate " +
            "  FROM tx t" +
            "      JOIN journalTx jtx USING (txId)" +
            "      JOIN journal j USING (journalId)" +
            "      JOIN config.journalType jt USING (journalTypeId)" +
            "  WHERE t.txId = :txId AND jt.journalTypeCode = :journalType")
    @RegisterBeanMapper(Journal.class)
    Journal getJournalByTxIdAndType(long txId, Constants.JournalType journalType);

    @SqlQuery("select txId, txTypeId, accountId, parentTxId, txVariantId, txReference, quantity, currencyId, creationDate " +
            "  from token " +
            "   join tx using (txId) " +
            "  where publishableId = :serialNumber")
    @RegisterBeanMapper(Tx.class)
    Tx getTxByTokenSerialNumber(long serialNumber);

    // N.B. The parameter 'id' is a String, but we are always passing a numeric. Because the numeric can be 19 digits we
    // do not convert to java long (it would not fit).
    @SqlQuery("""
               select txId, txTypeId, accountId, parentTxId, txVariantId, txReference, quantity, currencyId, creationDate,\s
                      j.journalId, j.bookingDate, jt.journalTypeCode\s
                 from tx t\s
                 join journalTx jtx using (txId)\s
                 join journal j using (journalId)\s
                 join config.journalType jt using (journalTypeId)\s
                where \
                accountId = :accountId \
                and t.txId = (select txId\s
                                  from tx t1\s
                                 where t1.txReference = :id\s
                                 union\s
                                select txId\s
                                  from token\s
                                 where publishableId = (:id)::bigint)\s
             order by j.bookingDate desc\s
                limit 1;\
            """)
    @RegisterBeanMapper(TxStatus.class)
    TxStatus getTxStatusByReferenceOrTokenSerialNumber(String id, long accountId);

    @SqlQuery("SELECT" +
            "       jl.quantity AS quantity," +
            "       c.currencyCode AS currencyCode," +
            "       c.currencyId AS currencyId" +
            "  FROM tx t" +
            "       JOIN config.currency c USING (currencyId)" +
            "       JOIN journalTx jtx USING (txId)" +
            "       JOIN journal j USING (journalId)" +
            "       JOIN config.journalType jt USING (journalTypeId) " +
            "       JOIN journalLine jl USING (journalId)" +
            "       JOIN batch b USING (batchId)" +
            "       JOIN config.registerType rt USING (registerTypeId)" +
            "  WHERE t.txId = :txId " +
            "    AND registerTypeCode = 'FeeReceivable'" +
            "    AND jt.journalTypeCode = :journalType")
    @RegisterBeanMapper(Amount.class)
    Amount getReceivedFee(long txId, Constants.JournalType journalType);

}
