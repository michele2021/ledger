package com.accounting.core.db;

import com.accounting.commons.Constants;
import com.accounting.commons.entities.Amount;
import com.accounting.core.entities.*;
import com.accounting.core.testUtils.LedgerPostgresExtension;
import com.accounting.core.testUtils.TestSqlUtils;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.accounting.core.testUtils.LedgerPostgresExtension.Mode.FOR_EACH_CLASS;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.*;

class JournalDAOTest {

    @RegisterExtension
    static JdbiExtension db = LedgerPostgresExtension.instance(FOR_EACH_CLASS);

    Handle handle = db.getSharedHandle();
    JournalDAO journalDAO = handle.attach(JournalDAO.class);

    @Test
    public void testCreateJournalNow() {
        //given
        final Integer journalTypeId = TestSqlUtils.getInt(handle, "SELECT journalTypeId FROM config.journalType LIMIT 1;");

        //when
        final long journalId = journalDAO.createJournalNow(journalTypeId);

        //then
        Journal journalAfterInsert = handle.select("SELECT * FROM public.journal WHERE journalId = ?", journalId)
                .mapToBean(Journal.class).stream()
                .findFirst()
                .orElse(null);

        assertThat(journalAfterInsert).isNotNull();
        assertThat(journalAfterInsert.getJournalId()).isEqualTo(journalId);
    }

    @Test
    public void testCreateJournal() {
        //given
        final OffsetDateTime bookingDate = OffsetDateTime.now();
        final Integer journalTypeId = TestSqlUtils.getInt(handle, "SELECT journalTypeId FROM config.journalType LIMIT 1;");

        final Journal expectedJournal = Journal.builder()
                .journalTypeId(journalTypeId)
                .bookingDate(bookingDate)
                .build();

        //when
        final long journalId = journalDAO.createJournal(expectedJournal);
        expectedJournal.setJournalId(journalId);

        //then
        Journal journalAfterInsert = handle.select("SELECT * FROM public.journal WHERE journalId = ?", journalId)
                .mapToBean(Journal.class).stream()
                .findFirst()
                .orElse(null);

        assertThat(journalAfterInsert).isNotNull().usingRecursiveComparison().ignoringFields("bookingDate").isEqualTo(expectedJournal);
        assertThat(journalAfterInsert.getBookingDate()).isCloseTo(bookingDate, within(1, ChronoUnit.MICROS));
    }


    @Test
    public void testCreateTx() {
        //given
        final String txReference = "txReference";
        final OffsetDateTime creationDate = OffsetDateTime.now();
        final Amount amount = Amount.of(5000, 1);

        final Integer txTypeId = TestSqlUtils.getInt(handle, "SELECT txtypeId FROM config.txType LIMIT 1;");
        final Integer accountId = TestSqlUtils.getInt(handle, "SELECT accountid FROM config.account LIMIT 1;");
        final Integer txVariantId = TestSqlUtils.getInt(handle, "SELECT txvariantId FROM config.txVariant LIMIT 1;");

        assertNotNull(txTypeId);
        assertNotNull(accountId);
        assertNotNull(txVariantId);

        Tx expectedTx = Tx.builder()
                .txTypeId(txTypeId)
                .accountId(accountId)
                .amount(amount)
                .txVariantId(txVariantId)
                .creationDate(creationDate)
                .txReference(txReference)
                .build();


        Tx txBeforeInsert = handle.select("SELECT * FROM public.tx WHERE txReference = ?", txReference)
                .mapToBean(Tx.class).stream()
                .findFirst()
                .orElse(null);

        assertNull(txBeforeInsert);

        //when
        final long txId = journalDAO.createTx(expectedTx);
        expectedTx.setTxId(txId);

        //then
        final Tx queryResultTx = handle.select("SELECT * FROM public.tx WHERE txid = ?", txId)
                .mapToBean(Tx.class)
                .one();

        assertThat(queryResultTx).isNotNull().usingRecursiveComparison().ignoringFields("creationDate").isEqualTo(expectedTx);
        assertThat(queryResultTx.getCreationDate()).isCloseTo(creationDate, within(1, ChronoUnit.MICROS));
    }

    @Test
    public void testCreateTxIfNotExistsSuccessNoConflict() {
        //given
        final String txReference = "txReference";
        final OffsetDateTime creationDate = OffsetDateTime.now();
        final Amount amount = Amount.of(5000, 1);

        final Integer txTypeId = TestSqlUtils.getInt(handle, "SELECT txtypeId FROM config.txType LIMIT 1;");
        final Integer accountId = TestSqlUtils.getInt(handle, "SELECT accountid FROM config.account LIMIT 1;");
        final Integer txVariantId = TestSqlUtils.getInt(handle, "SELECT txvariantId FROM config.txVariant LIMIT 1;");

        assertNotNull(txTypeId);
        assertNotNull(accountId);
        assertNotNull(txVariantId);

        Tx expectedTx = Tx.builder()
                .txTypeId(txTypeId)
                .accountId(accountId)
                .amount(amount)
                .txVariantId(txVariantId)
                .creationDate(creationDate)
                .txReference(txReference)
                .build();

        Tx txBeforeInsert = handle.select("SELECT * FROM public.tx WHERE txReference = ?", txReference)
                .mapToBean(Tx.class).stream()
                .findFirst()
                .orElse(null);

        assertNull(txBeforeInsert);

        //when
        long txId = journalDAO.createTxIfNotExists(expectedTx);
        expectedTx.setTxId(txId);

        //then
        Tx queryResultTx = handle.select("SELECT * FROM public.tx WHERE txid =?", txId)
                .mapToBean(Tx.class)
                .one();

        assertThat(queryResultTx).isNotNull().usingRecursiveComparison().ignoringFields("creationDate").isEqualTo(expectedTx);
        assertThat(queryResultTx.getCreationDate()).isCloseTo(creationDate, within(1, ChronoUnit.MICROS));
    }


    @Test
    public void testCreateTxIfNotExistsSuccessTxReferenceConflict() {
        //given
        final String txReference = "txReference";
        final OffsetDateTime creationDate = OffsetDateTime.now();

        final int quantity = 5000;
        final Amount amount = Amount.of(quantity, 1);

        final Integer txTypeId = TestSqlUtils.getInt(handle, "SELECT txtypeId FROM config.txType LIMIT 1;");
        final Integer accountId = TestSqlUtils.getInt(handle, "SELECT accountid FROM config.account LIMIT 1;");
        final Integer txVariantId = TestSqlUtils.getInt(handle, "SELECT txvariantId FROM config.txVariant LIMIT 1;");

        assertNotNull(txTypeId);
        assertNotNull(accountId);
        assertNotNull(txVariantId);

        Tx expectedTx = Tx.builder()
                .txTypeId(txTypeId)
                .accountId(accountId)
                .amount(amount)
                .txVariantId(txVariantId)
                .creationDate(creationDate)
                .txReference(txReference)
                .build();

        // when
        long firstQueryTxId = journalDAO.createTxIfNotExists(expectedTx);

        //then
        expectedTx.setTxId(firstQueryTxId);
        assertThat(firstQueryTxId).isNotZero();

        Tx firstQueryResultTx = handle.select("SELECT * FROM public.tx WHERE txid =?", firstQueryTxId)
                .mapToBean(Tx.class)
                .one();

        assertThat(firstQueryResultTx).isNotNull().usingRecursiveComparison().ignoringFields("creationDate").isEqualTo(expectedTx);
        assertThat(firstQueryResultTx.getCreationDate()).isCloseTo(creationDate, within(1, ChronoUnit.MICROS));


        // given
        int changedQuantity = 5300;
        expectedTx.setAmount(Amount.of(changedQuantity, 1));

        // when
        long secondQueryTxId = journalDAO.createTxIfNotExists(expectedTx);

        // then
        Tx secondQueryResultTx = handle.select("SELECT * FROM public.tx WHERE txid = ?", secondQueryTxId)
                .mapToBean(Tx.class)
                .one();

        assertThat(secondQueryTxId).isEqualTo(firstQueryTxId);

        assertThat(secondQueryResultTx).isNotNull().usingRecursiveComparison().ignoringFields("creationDate").isEqualTo(expectedTx);
        assertThat(secondQueryResultTx.getCreationDate()).isCloseTo(creationDate, within(1, ChronoUnit.MICROS));
        assertThat(secondQueryResultTx.getAmount().getQuantity()).isEqualTo(changedQuantity);
    }

    @Test
    public void testCreateJournalTx() {
        //given
        final String txReference = "txReference";
        final OffsetDateTime dateTime = OffsetDateTime.now();
        final Amount amount = Amount.of(5000, 1);

        final Integer txTypeId = TestSqlUtils.getInt(handle, "SELECT txtypeId FROM config.txType LIMIT 1;");
        final Integer accountId = TestSqlUtils.getInt(handle, "SELECT accountid FROM config.account LIMIT 1;");
        final Integer txVariantId = TestSqlUtils.getInt(handle, "SELECT txvariantId FROM config.txVariant LIMIT 1;");
        final Integer journalTypeId = TestSqlUtils.getInt(handle, "SELECT journalTypeId FROM config.journalType LIMIT 1;");

        assertNotNull(txTypeId);
        assertNotNull(accountId);
        assertNotNull(txVariantId);
        assertNotNull(journalTypeId);

        final Tx expectedTx = Tx.builder()
                .txTypeId(txTypeId)
                .accountId(accountId)
                .amount(amount)
                .txVariantId(txVariantId)
                .creationDate(dateTime)
                .txReference(txReference)
                .build();

        final Journal expectedJournal = Journal.builder()
                .journalTypeId(journalTypeId)
                .bookingDate(dateTime)
                .build();

        long txId = journalDAO.createTx(expectedTx);
        long journalId = journalDAO.createJournal(expectedJournal);
        expectedTx.setTxId(txId);
        expectedJournal.setJournalId(journalId);

        final JournalDb expectedJournalDb = new JournalDb();
        expectedJournalDb.setJournalId(journalId);
        expectedJournalDb.setTxId(txId);

        JournalDb journalDbBeforeInsert = handle.select("SELECT * FROM public.journalTx WHERE journalId = ? AND txId = ?", journalId, txId)
                .mapToBean(JournalDb.class)
                .findFirst()
                .orElse(null);

        assertNull(journalDbBeforeInsert);

        //when
        journalDAO.createJournalTx(journalId, txId);

        //then
        JournalDb journalDbAfterInsert = handle.select("SELECT * FROM public.journalTx WHERE journalId = ? AND txId = ?", journalId, txId)
                .mapToBean(JournalDb.class)
                .findFirst()
                .orElse(null);

        assertThat(journalDbAfterInsert).isNotNull();
        assertThat(journalDbAfterInsert).isEqualTo(expectedJournalDb);
    }

    @Test
    public void testCreateJournalLine() {
        //given
        final int currencyId = 1;
        final int quantity = 5000;
        final Amount amount = Amount.of(quantity, currencyId);
        final OffsetDateTime bookingDate = OffsetDateTime.now();

        final Integer accountId = TestSqlUtils.getInt(handle, "SELECT accountId FROM config.account LIMIT 1;");
        final Integer journalTypeId = TestSqlUtils.getInt(handle, "SELECT journalTypeId FROM config.journalType LIMIT 1;");
        final Integer registerTypeId = TestSqlUtils.getInt(handle, "SELECT registerTypeId FROM config.registerType LIMIT 1;");

        assertNotNull(accountId);
        assertNotNull(journalTypeId);
        assertNotNull(registerTypeId);

        final Journal journal = Journal.builder()
                .journalTypeId(journalTypeId)
                .bookingDate(bookingDate)
                .build();

        long journalId = journalDAO.createJournal(journal);
        journal.setJournalId(journalId);

        final Batch batch = Batch.builder()
                .batchNumber(1)
                .periodBeginDate(LocalDate.now())
                .periodEndDate(LocalDate.now().plusDays(1))
                .registerTypeId(registerTypeId)
                .accountId(accountId)
                .build();

        long batchId = journalDAO.createBatch(batch);
        batch.setBatchId(batchId);

        final JournalLine expectedJournalLine = JournalLine.builder()
                .journalId(journalId)
                .debit(amount)
                .batchId(batchId)
                .currencyId(currencyId)
                .quantity(quantity)
                .build();

        JournalLine journalLineBeforeInsert = handle.select(
                        "SELECT * FROM public.journalLine WHERE journalId = ? AND quantity = ?" +
                                " AND batchId = ? AND currencyId = ?", journalId, quantity, batchId, currencyId)

                .mapToBean(JournalLine.class)
                .findFirst()
                .orElse(null);

        assertNull(journalLineBeforeInsert);

        //when
        journalDAO.createJournalLine(expectedJournalLine);

        //then
        JournalLine journalLineAfterInsert = handle.select(
                        "SELECT * FROM public.journalLine WHERE journalId = ? AND quantity = ?" +
                                " AND batchId = ? AND currencyId = ?", journalId, quantity, batchId, currencyId)
                .mapToBean(JournalLine.class)
                .one();

        assertThat(journalLineAfterInsert).isNotNull();
        assertThat(journalLineAfterInsert).isEqualTo(expectedJournalLine);
    }

    @Test
    public void testCreateJournalLineBulk() {
        //given
        final int currencyId = 1;
        final int quantity = 5000;
        final Amount amount = Amount.of(quantity, currencyId);
        final OffsetDateTime bookingDate = OffsetDateTime.now();

        final Integer accountId = TestSqlUtils.getInt(handle, "SELECT accountId FROM config.account LIMIT 1;");
        final Integer journalTypeId = TestSqlUtils.getInt(handle, "SELECT journalTypeId FROM config.journalType LIMIT 1;");
        final Integer registerTypeId = TestSqlUtils.getInt(handle, "SELECT registerTypeId FROM config.registerType LIMIT 1;");


        assertNotNull(accountId);
        assertNotNull(journalTypeId);
        assertNotNull(registerTypeId);

        final Journal journal = Journal.builder()
                .journalTypeId(journalTypeId)
                .bookingDate(bookingDate)
                .build();

        long journalId = journalDAO.createJournal(journal);
        journal.setJournalId(journalId);

        final Batch batch = Batch.builder()
                .batchNumber(1)
                .periodBeginDate(LocalDate.now())
                .periodEndDate(LocalDate.now().plusDays(1))
                .registerTypeId(registerTypeId)
                .accountId(accountId)
                .build();

        long batchId = journalDAO.createBatch(batch);
        batch.setBatchId(batchId);

        JournalLine expectedJournalLine1 = JournalLine.builder()
                .journalId(journalId)
                .debit(amount)
                .batchId(batchId)
                .currencyId(currencyId)
                .quantity(quantity)
                .build();

        JournalLine expectedJournalLine2 = JournalLine.builder()
                .journalId(journalId)
                .credit(amount)
                .batchId(batchId)
                .currencyId(currencyId)
                .quantity(quantity)
                .build();
        List<JournalLine> expectedJournalLines = List.of(expectedJournalLine1, expectedJournalLine2);

        //when
        journalDAO.createJournalLinesBulk(expectedJournalLines);

        //then
        List<JournalLine> result = handle.select("SELECT * FROM public.journalLine WHERE journalId = ? AND batchId = ?", journalId, batchId)
                .mapToBean(JournalLine.class)
                .list();

        assertThat(result).isNotNull();
        assertIterableEquals(expectedJournalLines, result);
    }

    @Test
    public void testCreateBatch() {
        //given
        final Integer accountId = TestSqlUtils.getInt(handle, "SELECT accountId FROM config.account LIMIT 1;");
        final Integer registerTypeId = TestSqlUtils.getInt(handle, "SELECT registerTypeId FROM config.registerType LIMIT 1;");

        assertNotNull(accountId);
        assertNotNull(registerTypeId);

        int batchNumber = 1;
        Batch expectedBatch = Batch.builder()
                .batchNumber(batchNumber)
                .periodBeginDate(LocalDate.now())
                .periodEndDate(LocalDate.now().plusDays(1))
                .registerTypeId(registerTypeId)
                .accountId(accountId)
                .build();

        Batch batchBeforeInsert = handle.select(
                        "SELECT * FROM public.batch WHERE accountId = ? AND registerTypeId = ? AND batchNumber = ?",
                        accountId, registerTypeId, batchNumber)
                .mapToBean(Batch.class)
                .findFirst()
                .orElse(null);

        assertNull(batchBeforeInsert);

        //when
        long batchId = journalDAO.createBatch(expectedBatch);
        expectedBatch.setBatchId(batchId);

        //then
        Batch batchAfterInsert = handle.select("SELECT * FROM public.batch WHERE batchId = ?", batchId)
                .mapToBean(Batch.class)
                .one();

        assertThat(batchAfterInsert).isNotNull();
        assertThat(batchAfterInsert).isEqualTo(expectedBatch);
    }

    @Test
    public void testGetAvailableBatchSuccess() {
        //given
        final int batchNumber = 1;
        final Integer accountId = TestSqlUtils.getInt(handle, "SELECT accountId FROM config.account LIMIT 1;");
        final Integer registerTypeId = TestSqlUtils.getInt(handle, "SELECT registerTypeId FROM config.registerType LIMIT 1;");

        assertNotNull(accountId);
        assertNotNull(registerTypeId);

        Batch expectedBatch = Batch.builder()
                .batchNumber(batchNumber)
                .periodBeginDate(LocalDate.now())
                .periodEndDate(LocalDate.now().plusDays(1))
                .registerTypeId(registerTypeId)
                .accountId(accountId)
                .build();

        long batchId = journalDAO.createBatch(expectedBatch);
        expectedBatch.setBatchId(batchId);

        //when
        Batch availableBatch = journalDAO.getAvailableBatch(registerTypeId, accountId);

        //then
        assertThat(availableBatch).isNotNull();
        assertThat(availableBatch).isEqualTo(expectedBatch);
    }


    @Test
    public void testGetAvailableBatchNoAvailableBatchFound() {
        //given
        final int batchNumber = 1;
        final Integer accountId = TestSqlUtils.getInt(handle, "SELECT accountId FROM config.account LIMIT 1;");
        final Integer registerTypeId = TestSqlUtils.getInt(handle, "SELECT registerTypeId FROM config.registerType LIMIT 1;");

        assertNotNull(accountId);
        assertNotNull(registerTypeId);

        Batch expectedBatch = Batch.builder()
                .batchNumber(batchNumber)
                .periodBeginDate(LocalDate.now())
                .periodEndDate(LocalDate.now().plusDays(1))
                .registerTypeId(registerTypeId)
                .accountId(accountId)
                .build();

        long batchId = journalDAO.createBatch(expectedBatch);
        expectedBatch.setBatchId(batchId);
        String updateSql = format("UPDATE public.batch SET closedDate = '%s' WHERE batchId = %s", OffsetDateTime.now(), batchId);

        Integer doesBatchExist = handle.select("SELECT 1 FROM public.batch WHERE batchId = ?", batchId)
                .mapTo(Integer.class)
                .findFirst()
                .orElse(null);

        assertThat(doesBatchExist).isOne();

        int execute = handle.createUpdate(updateSql).execute();
        assertThat(execute).isOne();

        //when
        Batch availableBatch = journalDAO.getAvailableBatch(registerTypeId, accountId);

        //then
        assertThat(availableBatch).isNull();
    }

    @Test
    void testGetReceivedFee() {
        // given
        final int batchNumber = 1;
        final int currencyId = 1;
        final int quantity = 5000;
        final String txReference = "txReference";
        final OffsetDateTime dateTime = OffsetDateTime.now();
        final Amount amount = Amount.of(quantity, currencyId);

        final Integer txTypeId = TestSqlUtils.getInt(handle, "SELECT txtypeId FROM config.txType LIMIT 1;");
        final Integer accountId = TestSqlUtils.getInt(handle, "SELECT accountid FROM config.account LIMIT 1;");
        final Integer txVariantId = TestSqlUtils.getInt(handle, "SELECT txvariantId FROM config.txVariant LIMIT 1;");

        final Constants.JournalType journalType = Constants.JournalType.PaymentReceived;

        final Integer journalTypeId = TestSqlUtils.getInt(handle, "SELECT journalTypeId FROM config.journalType " +
                "WHERE journalTypeCode = ?;", journalType);
        final Integer registerTypeId = TestSqlUtils.getInt(handle, "SELECT registerTypeId FROM config.registerType " +
                "WHERE registerTypeCode = ?;", Constants.RegisterType.Fee);

        assertNotNull(txTypeId);
        assertNotNull(accountId);
        assertNotNull(txVariantId);
        assertNotNull(journalTypeId);
        assertNotNull(registerTypeId);

        final Tx expectedTx = Tx.builder()
                .txTypeId(txTypeId)
                .accountId(accountId)
                .amount(amount)
                .txVariantId(txVariantId)
                .creationDate(dateTime)
                .txReference(txReference)
                .build();

        long txId = journalDAO.createTx(expectedTx);
        expectedTx.setTxId(txId);

        final Journal expectedJournal = Journal.builder()
                .journalTypeId(journalTypeId)
                .bookingDate(dateTime)
                .build();

        long journalId = journalDAO.createJournal(expectedJournal);
        expectedJournal.setJournalId(journalId);

        final JournalDb expectedJournalDb = new JournalDb();
        expectedJournalDb.setJournalId(journalId);
        expectedJournalDb.setTxId(txId);

        journalDAO.createJournalTx(journalId, txId);

        final Batch batch = Batch.builder()
                .batchNumber(batchNumber)
                .periodBeginDate(LocalDate.now())
                .periodEndDate(LocalDate.now().plusDays(1))
                .registerTypeId(registerTypeId)
                .accountId(accountId)
                .build();

        long batchId = journalDAO.createBatch(batch);
        batch.setBatchId(batchId);

        final JournalLine expectedJournalLine = JournalLine.builder()
                .journalId(journalId)
                .debit(amount)
                .batchId(batchId)
                .currencyId(currencyId)
                .quantity(quantity)
                .build();


        //when
        journalDAO.createJournalLine(expectedJournalLine);

        Amount receivedFee1 = journalDAO.getReceivedFee(txId, journalType);
        Amount receivedFee2 = journalDAO.getReceivedFee(txId, Constants.JournalType.Received);

        // then
        assertThat(receivedFee1).isNotNull();
        assertThat(receivedFee1.getCurrencyId()).isEqualTo(currencyId);
        assertThat(receivedFee1.getQuantity()).isEqualTo(quantity);

        assertThat(receivedFee2).isNull();
    }


    @Test
    void testDoesJournalExist() {
        // given
        final int batchNumber = 1;
        final int currencyId = 1;
        final int quantity = 5000;
        final String txReference = "txReference";
        final OffsetDateTime dateTime = OffsetDateTime.now();
        final Amount amount = Amount.of(quantity, currencyId);

        final Integer txTypeId = TestSqlUtils.getInt(handle, "SELECT txtypeId FROM config.txType LIMIT 1;");
        final Integer accountId = TestSqlUtils.getInt(handle, "SELECT accountid FROM config.account LIMIT 1;");
        final Integer txVariantId = TestSqlUtils.getInt(handle, "SELECT txvariantId FROM config.txVariant LIMIT 1;");

        final Constants.JournalType journalType = Constants.JournalType.Received;

        final Integer journalTypeId = TestSqlUtils.getInt(handle, "SELECT journalTypeId FROM config.journalType " +
                "WHERE journalTypeCode = ?;", journalType);
        final Integer registerTypeId = TestSqlUtils.getInt(handle, "SELECT registerTypeId FROM config.registerType " +
                "WHERE registerTypeCode = ?;", Constants.RegisterType.Fee);

        assertNotNull(txTypeId);
        assertNotNull(accountId);
        assertNotNull(txVariantId);
        assertNotNull(journalTypeId);
        assertNotNull(registerTypeId);

        final Tx expectedTx = Tx.builder()
                .txTypeId(txTypeId)
                .accountId(accountId)
                .amount(amount)
                .txVariantId(txVariantId)
                .creationDate(dateTime)
                .txReference(txReference)
                .build();

        long txId = journalDAO.createTx(expectedTx);
        expectedTx.setTxId(txId);

        final Journal expectedJournal = Journal.builder()
                .journalTypeId(journalTypeId)
                .bookingDate(dateTime)
                .build();

        long journalId = journalDAO.createJournal(expectedJournal);
        expectedJournal.setJournalId(journalId);

        final JournalDb expectedJournalDb = new JournalDb();
        expectedJournalDb.setJournalId(journalId);
        expectedJournalDb.setTxId(txId);

        journalDAO.createJournalTx(journalId, txId);

        final Batch batch = Batch.builder()
                .batchNumber(batchNumber)
                .periodBeginDate(LocalDate.now())
                .periodEndDate(LocalDate.now().plusDays(1))
                .registerTypeId(registerTypeId)
                .accountId(accountId)
                .build();

        long batchId = journalDAO.createBatch(batch);
        batch.setBatchId(batchId);

        final JournalLine expectedJournalLine = JournalLine.builder()
                .journalId(journalId)
                .debit(amount)
                .batchId(batchId)
                .currencyId(currencyId)
                .quantity(quantity)
                .build();


        //when
        journalDAO.createJournalLine(expectedJournalLine);
        boolean exist1 = journalDAO.doesJournalExist(txId, journalType);
        boolean exist2 = journalDAO.doesJournalExist(txId, Constants.JournalType.Received);

        // then
        assertThat(exist1).isTrue();
        assertThat(exist2).isFalse();
    }

    @Test
    void testGetTxStatusWithIssuerAccountByReferenceAndAccountId() {
        // given
        final int batchNumber = 1;
        final int currencyId = 1;
        final int quantity = 5000;
        final String txReference = "txReference";
        final Amount amount = Amount.of(quantity, currencyId);

        final Integer txTypeId = TestSqlUtils.getInt(handle, "SELECT txtypeId FROM config.txType LIMIT 1;");
        final Long accountId = TestSqlUtils.getLong(handle, "SELECT accountid FROM config.account LIMIT 1;");
        final Long issuerAccountId = TestSqlUtils.getLong(handle, "SELECT accountid FROM config.account JOIN config.accountType USING (accountTypeId) WHERE accountTypeCode = 'Issuer' LIMIT 1;");
        final Integer txVariantId = TestSqlUtils.getInt(handle, "SELECT txvariantId FROM config.txVariant LIMIT 1;");

        assertNotNull(txTypeId);
        assertNotNull(accountId);
        assertNotNull(txVariantId);

        final Tx tx = Tx.builder()
                .txTypeId(txTypeId)
                .accountId(accountId)
                .amount(amount)
                .txVariantId(txVariantId)
                .creationDate(OffsetDateTime.now())
                .txReference(txReference)
                .build();

        long txId = journalDAO.createTx(tx);
        tx.setTxId(txId);

        // prepare the TokenCreated journal
        final Integer createdJournalTypeId = TestSqlUtils.getInt(handle, "SELECT journalTypeId FROM config.journalType " +
                "WHERE journalTypeCode = ?;", Constants.JournalType.Received);
        final Integer tokenCreatedRegisterTypeId = TestSqlUtils.getInt(handle, "SELECT registerTypeId FROM config.registerType " +
                "WHERE registerTypeCode = ?;", Constants.RegisterType.Receivable);
        assertNotNull(createdJournalTypeId);
        assertNotNull(tokenCreatedRegisterTypeId);

        final Journal createdJournal = Journal.builder()
                .journalTypeId(createdJournalTypeId)
                .bookingDate(OffsetDateTime.now())
                .build();
        long createdJournalId = journalDAO.createJournal(createdJournal);
        createdJournal.setJournalId(createdJournalId);
        journalDAO.createJournalTx(createdJournalId, txId);

        final Batch tokenCreatedBatch = Batch.builder()
                .batchNumber(batchNumber)
                .periodBeginDate(LocalDate.now())
                .periodEndDate(LocalDate.now().plusDays(1))
                .registerTypeId(tokenCreatedRegisterTypeId)
                .accountId(accountId)
                .build();
        long tokenCreatedBatchId = journalDAO.createBatch(tokenCreatedBatch);
        tokenCreatedBatch.setBatchId(tokenCreatedBatchId);

        final JournalLine tokenCreatedJournalLine = JournalLine.builder()
                .journalId(createdJournalId)
                .debit(amount)
                .batchId(tokenCreatedBatchId)
                .currencyId(currencyId)
                .quantity(quantity)
                .build();
        journalDAO.createJournalLine(tokenCreatedJournalLine);

        // prepare the Activated journal
        final Integer activatedJournalTypeId = TestSqlUtils.getInt(handle, "SELECT journalTypeId FROM config.journalType " +
                "WHERE journalTypeCode = ?;", Constants.JournalType.Received);
        final Integer cardsPayablePendingRegisterTypeId = TestSqlUtils.getInt(handle, "SELECT registerTypeId FROM config.registerType " +
                "WHERE registerTypeCode = ?;", Constants.RegisterType.Fee);
        assertNotNull(activatedJournalTypeId);
        assertNotNull(cardsPayablePendingRegisterTypeId);

        final Journal activatedJournal = Journal.builder()
                .journalTypeId(activatedJournalTypeId)
                .bookingDate(OffsetDateTime.now())
                .build();
        long activatedJournalId = journalDAO.createJournal(activatedJournal);
        activatedJournal.setJournalId(activatedJournalId);

        journalDAO.createJournalTx(activatedJournalId, txId);

        final Batch cardsPayablePendingBatch = Batch.builder()
                .batchNumber(batchNumber+1)
                .periodBeginDate(LocalDate.now())
                .periodEndDate(LocalDate.now().plusDays(1))
                .registerTypeId(cardsPayablePendingRegisterTypeId)
                .accountId(issuerAccountId)
                .build();
        long cardsPayablePendingBatchId = journalDAO.createBatch(cardsPayablePendingBatch);
        cardsPayablePendingBatch.setBatchId(cardsPayablePendingBatchId);

        final JournalLine cardsPayablePendingJournalLine = JournalLine.builder()
                .journalId(activatedJournalId)
                .debit(amount)
                .batchId(cardsPayablePendingBatchId)
                .currencyId(currencyId)
                .quantity(quantity)
                .build();
        journalDAO.createJournalLine(cardsPayablePendingJournalLine);

        //when
        TxStatus txStatus = journalDAO.getTxStatusWithIssuerAccountByReferenceAndAccountId(txReference, accountId);

        // then
        assertThat(txStatus).isNotNull();
        assertThat(txStatus.getAccountId()).isEqualTo(accountId);
        assertThat(txStatus.getIssuerAccountId()).isNotNull();
        assertThat(txStatus.getIssuerAccountId()).isEqualTo(issuerAccountId);
    }

    @Test
    void testGetJournalByTxIdAndType() {
        // given
        final int batchNumber = 1;
        final int currencyId = 1;
        final int quantity = 5000;
        final String txReference = "txReference";
        final OffsetDateTime dateTime = OffsetDateTime.now();
        final Amount amount = Amount.of(quantity, currencyId);

        final Integer txTypeId = TestSqlUtils.getInt(handle, "SELECT txtypeId FROM config.txType LIMIT 1;");
        final Integer accountId = TestSqlUtils.getInt(handle, "SELECT accountid FROM config.account LIMIT 1;");
        final Integer txVariantId = TestSqlUtils.getInt(handle, "SELECT txvariantId FROM config.txVariant LIMIT 1;");

        final Constants.JournalType journalType = Constants.JournalType.Received;

        final Integer activatedJournalTypeId = TestSqlUtils.getInt(handle, "SELECT journalTypeId FROM config.journalType " +
                "WHERE journalTypeCode = ?;", journalType);
        final Integer registerTypeId = TestSqlUtils.getInt(handle, "SELECT registerTypeId FROM config.registerType " +
                "WHERE registerTypeCode = ?;", Constants.RegisterType.Fee);

        assertNotNull(txTypeId);
        assertNotNull(accountId);
        assertNotNull(txVariantId);
        assertNotNull(activatedJournalTypeId);
        assertNotNull(registerTypeId);

        final Tx expectedTx = Tx.builder()
                .txTypeId(txTypeId)
                .accountId(accountId)
                .amount(amount)
                .txVariantId(txVariantId)
                .creationDate(dateTime)
                .txReference(txReference)
                .build();

        long txId = journalDAO.createTx(expectedTx);
        expectedTx.setTxId(txId);

        final Journal expectedJournal = Journal.builder()
                .journalTypeId(activatedJournalTypeId)
                .bookingDate(dateTime)
                .build();

        long journalId = journalDAO.createJournal(expectedJournal);
        expectedJournal.setJournalId(journalId);

        final JournalDb expectedJournalDb = new JournalDb();
        expectedJournalDb.setJournalId(journalId);
        expectedJournalDb.setTxId(txId);

        journalDAO.createJournalTx(journalId, txId);

        final Batch batch = Batch.builder()
                .batchNumber(batchNumber)
                .periodBeginDate(LocalDate.now())
                .periodEndDate(LocalDate.now().plusDays(1))
                .registerTypeId(registerTypeId)
                .accountId(accountId)
                .build();

        long batchId = journalDAO.createBatch(batch);
        batch.setBatchId(batchId);

        final JournalLine expectedJournalLine = JournalLine.builder()
                .journalId(journalId)
                .debit(amount)
                .batchId(batchId)
                .currencyId(currencyId)
                .quantity(quantity)
                .build();


        //when
        journalDAO.createJournalLine(expectedJournalLine);

        Journal journalActivated = journalDAO.getJournalByTxIdAndType(txId, journalType);
        Journal journalCancelled = journalDAO.getJournalByTxIdAndType(txId, Constants.JournalType.Received);

        // then
        assertThat(journalActivated).isNotNull();
        assertThat(journalActivated.getJournalTypeId()).isEqualTo(activatedJournalTypeId);

        assertThat(journalCancelled).isNull();
    }

    @Test
    @Disabled("no longer supported the possibility to have a tx of type \"Order\"")
    public void testCreateOrderTx() {
        //given
        final String txReference = "txReference";
        final OffsetDateTime creationDate = OffsetDateTime.now();
        final Amount amount = Amount.of(5000, 1);

        final Integer txTypeId = TestSqlUtils.getInt(handle, "SELECT txtypeId FROM config.txType LIMIT 1;");
        final Integer accountId = TestSqlUtils.getInt(handle, "SELECT accountid FROM config.account LIMIT 1;");
        final Integer txVariantId = TestSqlUtils.getInt(handle, "SELECT txvariantId FROM config.txVariant LIMIT 1;");

        assertNotNull(txTypeId);
        assertNotNull(accountId);
        assertNotNull(txVariantId);

        final Tx tx = Tx.builder()
                .txTypeId(txTypeId)
                .accountId(accountId)
                .amount(amount)
                .txVariantId(txVariantId)
                .creationDate(creationDate)
                .txReference(txReference)
                .build();

        long txId = journalDAO.createTx(tx);
        tx.setTxId(txId);

        final long externalId = 3L;
        OrderDTO expectedOrder = OrderDTO.builder()
                .orderNumber(externalId)
                .build();

        Order orderBeforeInsert = handle.select("SELECT * FROM public.orders WHERE txId = ?", txId)
                .mapToBean(Order.class).stream()
                .findFirst()
                .orElse(null);

        assertNull(orderBeforeInsert);

        //when
        journalDAO.createOrder(expectedOrder);

        //then
        Order orderAfterInsert = handle.select("SELECT * FROM public.orders WHERE txId = ?", txId)
                .mapToBean(Order.class)
                .one();

        assertThat(orderAfterInsert).isNotNull();
        assertThat(orderAfterInsert).isEqualTo(expectedOrder);
    }

}

