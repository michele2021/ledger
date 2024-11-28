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

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

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
    public void shouldCreateTokenTx() {
        //given
        final long txId = 123456879542L;
        final Long orderId = 5421L;
        final String publishableId = "324534";
        final String orderItemId = "0123456789";
        final LocalDate expiryDate = LocalDate.of(2024, 1, 25);
        final String locationRef = "location";
        final String activatingLocationRef = "activating location";
        final String activationTimezone = "Europe/Warsaw";
        final OffsetDateTime activationTimestamp = OffsetDateTime.ofInstant(
                LocalDateTime.of(2020, 6, 20, 10, 20, 30).toInstant(ZoneOffset.UTC),
                ZoneId.of(activationTimezone));

        handle.createUpdate("INSERT INTO public.tx (" +
                        "txid," +
                        "txtypeid," +
                        "accountid," +
                        "txvariantid," +
                        "currencyid," +
                        "quantity," +
                        "creationdate" +
                        ") VALUES (" +
                        txId + "," +
                        "7," + //txtypeid
                        "1," + //accountid
                        "1," + //txvariantid
                        "1," + //currencyid
                        "1," + //quantity
                        "'2022-01-01 01:01'" + //creationdate
                        ")")
                .execute();

        final CCToken ccToken = CCToken.builder()
                .txId(txId)
                .orderId(orderId)
                .publishableId(publishableId)
                .expiryDate(expiryDate)
                .locationRef(locationRef)
                .activatingLocationRef(activatingLocationRef)
                .activationTimestamp(activationTimestamp)
                .activationTimezone(activationTimezone)
                .orderItemId(orderItemId)
                .type(Constants.TokenType.POSA)
                .build();

        //when
        journalDAO.createTokenTx(ccToken);

        //then
        List<CCToken> result = handle.select("SELECT * FROM public.token WHERE txid =?", txId)
                .mapToBean(CCToken.class)
                .list();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTxId()).isEqualTo(txId);
        assertThat(result.get(0).getOrderId()).isEqualTo(orderId);
        assertThat(result.get(0).getPublishableId()).isEqualTo(publishableId);
        assertThat(result.get(0).getActivatingLocationRef()).isEqualTo(activatingLocationRef);
        assertThat(result.get(0).getLocationRef()).isEqualTo(locationRef);
        assertThat(result.get(0).getActivationTimestamp()).isEqualTo(activationTimestamp);
        assertThat(result.get(0).getActivationTimezone()).isEqualTo(activationTimezone);
        assertThat(result.get(0).getOrderItemId()).isEqualTo(orderItemId);
        assertThat(result.get(0).getType()).isEqualTo(Constants.TokenType.POSA);

        List<Map<String, Object>> resultMap = handle.select("SELECT created, updated FROM public.token WHERE txid =?", txId)
                .mapToMap()
                .list();

        assertThat(resultMap).hasSize(1);
        assertThat(resultMap.get(0).get("created")).isNotNull();
        assertThat(resultMap.get(0).get("updated")).isNotNull();
    }


    @Test
    public void shouldUpdateTokenTx() {
        //given
        final long txId = 123456879542L;
        final Long orderId = 5421L;
        final String publishableId = "324534";
        final String orderItemId = "0123456789";
        final LocalDate expiryDate = LocalDate.of(2024, 1, 25);
        final String locationRef = "location";
        final String activatingLocationRef = "activating location";
        final String activationTimezone = "Europe/Warsaw";
        final String prepaidCardExternalId = "CardExternalId";
        final OffsetDateTime activationTimestamp = OffsetDateTime.ofInstant(
                LocalDateTime.of(2020, 6, 20, 10, 20, 30).toInstant(ZoneOffset.UTC),
                ZoneId.of(activationTimezone));

        handle.createUpdate("INSERT INTO public.tx (" +
                        "txid," +
                        "txtypeid," +
                        "accountid," +
                        "txvariantid," +
                        "currencyid," +
                        "quantity," +
                        "creationdate" +
                        ") VALUES (" +
                        txId + "," +
                        "7," + //txtypeid
                        "1," + //accountid
                        "1," + //txvariantid
                        "1," + //currencyid
                        "1," + //quantity
                        "'2022-01-01 01:01'" + //creationdate
                        ")")
                .execute();

        journalDAO.createTokenTx(CCToken.builder()
                .txId(txId)
                .orderId(orderId)
                .locationRef(locationRef)
                .publishableId(publishableId)
                .orderItemId(orderItemId)
                .type(Constants.TokenType.POSA)
                .build());


        //when
        journalDAO.updateTokenTx(
                txId,
                locationRef,
                activatingLocationRef,
                expiryDate,
                activationTimestamp,
                activationTimezone,
                prepaidCardExternalId
        );

        //then
        List<CCToken> result = handle.select("SELECT * FROM public.token WHERE txid =?", txId)
                .mapToBean(CCToken.class)
                .list();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTxId()).isEqualTo(txId);
        assertThat(result.get(0).getOrderId()).isEqualTo(orderId);
        assertThat(result.get(0).getPublishableId()).isEqualTo(publishableId);
        assertThat(result.get(0).getActivatingLocationRef()).isEqualTo(activatingLocationRef);
        assertThat(result.get(0).getActivationTimestamp()).isEqualTo(activationTimestamp);
        assertThat(result.get(0).getActivationTimezone()).isEqualTo(activationTimezone);
        assertThat(result.get(0).getPrepaidCardExternalId()).isEqualTo(prepaidCardExternalId);
        //make sure thi are not changed
        assertThat(result.get(0).getLocationRef()).isEqualTo(locationRef);
        assertThat(result.get(0).getOrderItemId()).isEqualTo(orderItemId);
        assertThat(result.get(0).getType()).isEqualTo(Constants.TokenType.POSA);


        List<Map<String, Object>> resultMap = handle.select("SELECT created, updated FROM public.token WHERE txid =?", txId)
                .mapToMap()
                .list();

        assertThat(resultMap).hasSize(1);
        assertThat(resultMap.get(0).get("created")).isNotNull();
        assertThat(resultMap.get(0).get("updated")).isNotNull();
    }

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

        final Constants.JournalType journalType = Constants.JournalType.Activated;

        final Integer journalTypeId = TestSqlUtils.getInt(handle, "SELECT journalTypeId FROM config.journalType " +
                "WHERE journalTypeCode = ?;", journalType);
        final Integer registerTypeId = TestSqlUtils.getInt(handle, "SELECT registerTypeId FROM config.registerType " +
                "WHERE registerTypeCode = ?;", Constants.RegisterType.FeeReceivable);

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
        Amount receivedFee2 = journalDAO.getReceivedFee(txId, Constants.JournalType.Cancelled);

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

        final Constants.JournalType journalType = Constants.JournalType.Activated;

        final Integer journalTypeId = TestSqlUtils.getInt(handle, "SELECT journalTypeId FROM config.journalType " +
                "WHERE journalTypeCode = ?;", journalType);
        final Integer registerTypeId = TestSqlUtils.getInt(handle, "SELECT registerTypeId FROM config.registerType " +
                "WHERE registerTypeCode = ?;", Constants.RegisterType.FeeReceivable);

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
        boolean exist2 = journalDAO.doesJournalExist(txId, Constants.JournalType.Cancelled);

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
                "WHERE journalTypeCode = ?;", Constants.JournalType.Created);
        final Integer tokenCreatedRegisterTypeId = TestSqlUtils.getInt(handle, "SELECT registerTypeId FROM config.registerType " +
                "WHERE registerTypeCode = ?;", Constants.RegisterType.TokenCreated);
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
                "WHERE journalTypeCode = ?;", Constants.JournalType.Activated);
        final Integer cardsPayablePendingRegisterTypeId = TestSqlUtils.getInt(handle, "SELECT registerTypeId FROM config.registerType " +
                "WHERE registerTypeCode = ?;", Constants.RegisterType.CardsPayablePending);
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

        final Constants.JournalType journalType = Constants.JournalType.Activated;

        final Integer activatedJournalTypeId = TestSqlUtils.getInt(handle, "SELECT journalTypeId FROM config.journalType " +
                "WHERE journalTypeCode = ?;", journalType);
        final Integer registerTypeId = TestSqlUtils.getInt(handle, "SELECT registerTypeId FROM config.registerType " +
                "WHERE registerTypeCode = ?;", Constants.RegisterType.FeeReceivable);

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
        Journal journalCancelled = journalDAO.getJournalByTxIdAndType(txId, Constants.JournalType.Cancelled);

        // then
        assertThat(journalActivated).isNotNull();
        assertThat(journalActivated.getJournalTypeId()).isEqualTo(activatedJournalTypeId);

        assertThat(journalCancelled).isNull();
    }


    @Test
    void testGetTokenTxByParentIdAndTxTypeId() {
        // given
        final int currencyId = 1;
        final int quantity = 5000;
        final Long orderId = 5421L;
        final String publishableId = "324534";
        final String orderItemId = "0123456789";
        final String locationRef = "location";
        final String txReference = "txReference";
        final OffsetDateTime dateTime = OffsetDateTime.now();
        final Amount amount = Amount.of(quantity, currencyId);

        final Integer txTypeId = TestSqlUtils.getInt(handle, "SELECT txtypeId FROM config.txType t WHERE t.txTypeCode = 'TokenReconciliation' LIMIT 1;");
        final Integer accountId = TestSqlUtils.getInt(handle, "SELECT accountid FROM config.account LIMIT 1;");
        final Integer txVariantId = TestSqlUtils.getInt(handle, "SELECT txvariantId FROM config.txVariant LIMIT 1;");


        assertNotNull(txTypeId);
        assertNotNull(accountId);
        assertNotNull(txVariantId);

        final Tx parentTx = TxStatus.builder()
                .txTypeId(txTypeId)
                .accountId(accountId)
                .amount(amount)
                .txReference(txReference)
                .txVariantId(txVariantId)
                .creationDate(dateTime)
                .build();

        final long parentTxId = journalDAO.createTx(parentTx);

        final CCToken token = CCToken.builder()
                .txId(parentTxId)
                .orderId(orderId)
                .locationRef(locationRef)
                .publishableId(publishableId)
                .orderItemId(orderItemId)
                .type(Constants.TokenType.POSA)
                .build();

        journalDAO.createTokenTx(token);

        final TxStatus childTx = new TxStatus();
        childTx.setParentTxId(parentTxId);
        childTx.setTxTypeId(txTypeId);
        childTx.setAccountId(accountId);
        childTx.setAmount(amount);
        childTx.setTxVariantId(txVariantId);
        childTx.setCreationDate(dateTime);

        final long childTxId = journalDAO.createTx(childTx);
        childTx.setTxId(childTxId);

        //when
        List<TxStatus> childTxs = journalDAO.getTokenTxByParentIdAndTxTypeId(parentTxId, Constants.TxType.TokenReconciliation);

        // then
        assertThat(childTxs).isNotEmpty().hasSize(1);

        TxStatus actualTx = childTxs.get(0);

        assertThat(actualTx.getTokenType()).isNotNull();
        assertThat(actualTx.getTxId()).isEqualTo(childTx.getTxId());
        assertThat(actualTx.getParentTxId()).isEqualTo(childTx.getParentTxId());
        assertThat(actualTx.getTxTypeId()).isEqualTo(childTx.getTxTypeId());
        assertThat(actualTx.getTxVariantId()).isEqualTo(childTx.getTxVariantId());
        assertThat(actualTx.getAccountId()).isEqualTo(childTx.getAccountId());
        assertThat(actualTx.getAmount()).isEqualTo(childTx.getAmount());
        assertThat(actualTx.getTxReference()).isEqualTo(childTx.getTxReference());

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
        Order expectedOrder = Order.builder()
                .cardProgram("Card Program")
                .externalId(externalId)
                .sku("sku")
                .title("title")
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

