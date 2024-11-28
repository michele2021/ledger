package com.accounting.core.processing.templates;

import com.accounting.commons.Constants;
import com.accounting.commons.entities.Account;
import com.accounting.commons.entities.Amount;
import com.accounting.commons.entities.JournalType;
import com.accounting.commons.entities.RegisterType;
import com.accounting.commons.exceptions.AccountingMakerException;
import com.accounting.commons.repository.RegisterTypeRepoIf;
import com.accounting.core.db.JournalDAO;
import com.accounting.core.entities.JournalLine;
import com.accounting.core.entities.Tx;
import com.accounting.core.processing.JournalMaker;
import com.accounting.core.reports.model.DailySalesReportRow;
import com.accounting.core.testUtils.TestDailySalesReportsUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenMatchTest {

    @Spy
    private final Tx tokenReconciliationTx = Tx.builder()
            .txId(2L)
            .parentTxId(1L)
            .amount(Amount.of(5300, 1)) // tokenReconciliationTx amount includes fees
            .accountId(7)
            .txReference(UUID.randomUUID().toString())
            .txVariantId(1)
            .creationDate(OffsetDateTime.now())
            .build();
    @Spy
    private final DailySalesReportRow reportRow = TestDailySalesReportsUtils.getReportRow();

    @Spy
    private final Account account = new Account(16, "EPAY_IT", 7,
            Constants.AccountType.Distributor.name(), 15L, "EPAY", "", null, null, false, OffsetDateTime.now(), "user1");

    @Spy
    private final JournalType journalType = new JournalType(14, Constants.JournalType.Matched.name());

    @Mock
    private JournalDAO journalDAO;

    @Mock
    private JournalMaker journalMaker;

    @Mock
    private RegisterTypeRepoIf registerTypeRepo;

    @InjectMocks
    private TokenMatch tokenMatch;

    @AfterEach
    void reset() {
        Mockito.reset(journalDAO, journalMaker, registerTypeRepo, tokenReconciliationTx, reportRow, journalType);
    }

    @Test
    void testAddNewTx() {
        assertFalse(tokenMatch.addNewTx());
    }

    @Test
    void testGetTxId() {
        assertEquals(tokenReconciliationTx.getTxId(), tokenMatch.getTxId());
    }

    @Test
    void testAddSpecializedTx() {
        assertFalse(tokenMatch.addSpecializedTx());
    }

    @Test
    void testBuildSpecializedTx() {
        assertThrows(UnsupportedOperationException.class, () -> tokenMatch.buildSpecializedTxs(1L));
    }

    @Test
    void testProcess() throws AccountingMakerException {
        // when
        tokenMatch.process();

        // then
        verify(journalMaker).createJournal(tokenMatch);
        verifyNoMoreInteractions(journalMaker);
    }

    @Test
    void testBuildJournalLinesWithFeeReceivable_Activated() throws AccountingMakerException {
        // given
        long journalId = 1L;
        int currencyId = reportRow.getCurrencyId();

        String saleMatchedString = Constants.RegisterType.SaleMatched.name();
        String tokenMatchedString = Constants.RegisterType.TokenMatched.name();
        String feeMatchedString = Constants.RegisterType.FeeMatched.name();

        RegisterType salesMatched = new RegisterType(1, 1, saleMatchedString, null);
        RegisterType tokenReceivable = new RegisterType(1, 2, tokenMatchedString, null);
        RegisterType feeReceivable = new RegisterType(1, 3, feeMatchedString, null);

        long epaySalesMatchedBatchId = 1L;
        long epayTokenReceivableBatchId = 2L;
        long epayFeeReceivableBatchId = 3L;

        long feeQuantity = 300L;
        long txQuantity = tokenReconciliationTx.getAmount().getQuantity() - feeQuantity;
        Long rowQuantity = reportRow.getGrossAmountQuantity();
        Constants.JournalType rowJournalType = reportRow.getJournalType();

        JournalLine salesMatchedLine = JournalLine.builder()
                .journalId(journalId)
                .batchId(epaySalesMatchedBatchId)
                .debit(Amount.of(rowQuantity, currencyId))
                .build();

        JournalLine tokenReceivableLine = JournalLine.builder()
                .journalId(journalId)
                .batchId(epayTokenReceivableBatchId)
                .credit(Amount.of(txQuantity, currencyId))
                .build();

        Amount feeAmount = Amount.of(feeQuantity, currencyId);
        JournalLine feeReceivableLine = JournalLine.builder()
                .journalId(journalId)
                .batchId(epayFeeReceivableBatchId)
                .credit(feeAmount)
                .build();

        List<JournalLine> expectedJournalLines = List.of(feeReceivableLine, tokenReceivableLine, salesMatchedLine);

        when(journalDAO.getReceivedFee(tokenReconciliationTx.getParentTxId(), rowJournalType)).thenReturn(feeAmount);
        when(registerTypeRepo.get(saleMatchedString)).thenReturn(salesMatched);
        when(registerTypeRepo.get(tokenMatchedString)).thenReturn(tokenReceivable);
        when(registerTypeRepo.get(feeMatchedString)).thenReturn(feeReceivable);

        when(journalMaker.getOrCreateAvailableBatch(salesMatched.getRegisterTypeId(), account.getAccountId())).thenReturn(epaySalesMatchedBatchId);
        when(journalMaker.getOrCreateAvailableBatch(tokenReceivable.getRegisterTypeId(), account.getAccountId())).thenReturn(epayTokenReceivableBatchId);
        when(journalMaker.getOrCreateAvailableBatch(feeReceivable.getRegisterTypeId(), account.getAccountId())).thenReturn(epayFeeReceivableBatchId);

        // when
        List<JournalLine> actualJournalLines = tokenMatch.buildJournalLines(journalId);

        // then
        assertThat(actualJournalLines).hasSize(3);
        assertIterableEquals(expectedJournalLines, actualJournalLines);

        long journalLinesSum = actualJournalLines.stream()
                .mapToLong(JournalLine::getQuantity)
                .sum();

        assertThat(journalLinesSum).isZero();
        verify(journalDAO).getReceivedFee(tokenReconciliationTx.getParentTxId(), rowJournalType);

        verify(registerTypeRepo).get(saleMatchedString);
        verify(registerTypeRepo).get(tokenMatchedString);
        verify(registerTypeRepo).get(feeMatchedString);

        verify(journalMaker).getOrCreateAvailableBatch(salesMatched.getRegisterTypeId(), account.getAccountId());
        verify(journalMaker).getOrCreateAvailableBatch(tokenReceivable.getRegisterTypeId(), account.getAccountId());
        verify(journalMaker).getOrCreateAvailableBatch(feeReceivable.getRegisterTypeId(), account.getAccountId());

    }

    @Test
    void testBuildJournalLinesWithFeeReceivableNotFound_Cancelled() throws AccountingMakerException {
        // given
        int currencyId = 1;
        long journalId = 1L;

        String saleMatchedString = Constants.RegisterType.SaleMatched.name();
        String tokenMatchedString = Constants.RegisterType.TokenMatched.name();

        RegisterType salesMatched = new RegisterType(1, 1, saleMatchedString, null);
        RegisterType tokenReceivable = new RegisterType(1, 2, tokenMatchedString, null);

        long epaySalesMatchedBatchId = 1L;
        long epayTokenReceivableBatchId = 2L;

        Long rowQuantity = reportRow.getGrossAmountQuantity();
        Long txQuantity = tokenReconciliationTx.getAmount().getQuantity();

        JournalLine salesMatchedLine = JournalLine.builder()
                .journalId(journalId)
                .batchId(epaySalesMatchedBatchId)
                .debit(Amount.of(rowQuantity, currencyId))
                .build();

        JournalLine tokenReceivableLine = JournalLine.builder()
                .journalId(journalId)
                .batchId(epayTokenReceivableBatchId)
                .credit(Amount.of(txQuantity, currencyId))
                .build();

        List<JournalLine> expectedJournalLines = List.of(tokenReceivableLine, salesMatchedLine);

        when(registerTypeRepo.get(saleMatchedString)).thenReturn(salesMatched);
        when(registerTypeRepo.get(tokenMatchedString)).thenReturn(tokenReceivable);

        when(journalMaker.getOrCreateAvailableBatch(salesMatched.getRegisterTypeId(), account.getAccountId())).thenReturn(epaySalesMatchedBatchId);
        when(journalMaker.getOrCreateAvailableBatch(tokenReceivable.getRegisterTypeId(), account.getAccountId())).thenReturn(epayTokenReceivableBatchId);

        // when
        List<JournalLine> actualJournalLines = tokenMatch.buildJournalLines(journalId);

        // then
        assertThat(actualJournalLines).hasSize(2);
        assertIterableEquals(expectedJournalLines, actualJournalLines);

        long journalLinesSum = actualJournalLines.stream()
                .mapToLong(JournalLine::getQuantity)
                .sum();

        assertThat(journalLinesSum).isZero();

        verify(registerTypeRepo).get(saleMatchedString);
        verify(registerTypeRepo).get(tokenMatchedString);

        verify(journalMaker).getOrCreateAvailableBatch(salesMatched.getRegisterTypeId(), account.getAccountId());
        verify(journalMaker).getOrCreateAvailableBatch(tokenReceivable.getRegisterTypeId(), account.getAccountId());
    }

}
