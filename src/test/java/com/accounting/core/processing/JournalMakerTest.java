package com.accounting.core.processing;

import com.accounting.commons.Constants;
import com.accounting.commons.entities.Amount;
import com.accounting.commons.entities.JournalType;
import com.accounting.commons.exceptions.AccountingMakerException;
import com.accounting.core.db.JournalDAO;
import com.accounting.core.entities.JournalDb;
import com.accounting.core.entities.JournalLine;
import com.accounting.core.entities.OrderDTO;
import com.accounting.core.entities.Tx;
import com.accounting.core.processing.templates.TokenMatch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JournalMakerTest {

    @Mock
    private JournalDAO journalDAO;

    @Mock
    private TokenMatch tokenMatch;

    @InjectMocks
    private JournalMaker journalMaker;

    @AfterEach
    void reset() {
        Mockito.reset(journalDAO, tokenMatch);
    }

    @Test
    void testCreateJournalNowSuccess() throws AccountingMakerException {
        // given
        int currencyId = 1;
        long journalId = 1L;

        long feeQuantity = 300L;
        long txQuantity = 5000L;
        long rowQuantity = 5300L;

        long epaySalesMatchedBatchId = 1L;
        long epayTokenReceivableBatchId = 2L;
        long epayFeeReceivableBatchId = 3L;

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

        long txId = 1L;
        JournalType journalType = new JournalType(1, Constants.JournalType.Matched.name());
        JournalDb expectedJournalDb = JournalDb.builder()
                .txId(txId)
                .journalId(journalId)
                .build();

        when(tokenMatch.buildJournalLines(journalId)).thenReturn(expectedJournalLines);
        when(tokenMatch.getJournalType()).thenReturn(journalType);
        when(tokenMatch.linkTxs()).thenReturn(List.of(txId));
        when(journalDAO.createJournalAt(anyInt(), any())).thenReturn(journalId);

        when(tokenMatch.addNewTx()).thenReturn(false);
        when(tokenMatch.addSpecializedTx()).thenReturn(false);

        // when
        JournalDb actualJournalDb = journalMaker.createJournal(tokenMatch);
        assertEquals(expectedJournalDb, actualJournalDb);

        // then

        // no interaction expected
        verify(journalDAO, times(0)).createTx(any(Tx.class));
        verify(journalDAO, times(0)).createOrder(any(OrderDTO.class));

        verify(tokenMatch).linkTxs();
        verify(tokenMatch).getJournalType();
        verify(tokenMatch).updateSpecializedTx();

        verify(journalDAO).createJournalAt(eq(journalType.getJournalTypeId()), isNull());

        verify(journalDAO).createJournalTxBulk(journalId, List.of(txId));
        verify(journalDAO).createJournalLinesBulk(expectedJournalLines);

        verifyNoMoreInteractions(journalDAO);
    }

    @Test
    void testCreateJournalAtSuccess() throws AccountingMakerException {
        // given
        int currencyId = 1;
        long journalId = 100001L;

        long feeQuantity = 300L;
        long txQuantity = 5000L;
        long rowQuantity = 5300L;

        long epaySalesMatchedBatchId = 1L;
        long epayTokenReceivableBatchId = 2L;
        long epayFeeReceivableBatchId = 3L;

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

        long txId = 101L;
        OffsetDateTime bookingDate = OffsetDateTime.of(2022, 5, 12, 11, 0, 0, 0, ZoneOffset.UTC);
        JournalType journalType = new JournalType(1, Constants.JournalType.Matched.name());
        JournalDb expectedJournalDb = JournalDb.builder()
                .txId(txId)
                .journalId(journalId)
                .build();

        when(tokenMatch.buildJournalLines(1L)).thenReturn(expectedJournalLines);
        when(tokenMatch.getJournalType()).thenReturn(journalType);
        when(tokenMatch.linkTxs()).thenReturn(List.of(txId));
        when(tokenMatch.getJournalBookingDate()).thenReturn(bookingDate);
        when(journalDAO.createJournalAt(anyInt(), any(OffsetDateTime.class))).thenReturn(journalId);

        when(tokenMatch.addNewTx()).thenReturn(false);
        when(tokenMatch.addSpecializedTx()).thenReturn(false);

        // when
        JournalDb actualJournalDb = journalMaker.createJournal(tokenMatch);
        assertEquals(expectedJournalDb, actualJournalDb);

        // then

        // no interaction expected
        verify(journalDAO, times(0)).createTx(any(Tx.class));
        verify(journalDAO, times(0)).createOrder(any(OrderDTO.class));

        verify(tokenMatch).linkTxs();
        verify(tokenMatch).getJournalType();
        verify(tokenMatch).updateSpecializedTx();

        verify(journalDAO).createJournalAt(eq(journalType.getJournalTypeId()), eq(bookingDate));

        verify(journalDAO).createJournalTxBulk(journalId, List.of(txId));
        verify(journalDAO).createJournalLinesBulk(expectedJournalLines);

        verifyNoMoreInteractions(journalDAO);
    }


    @Test
    void testCreateJournalLinesNotBalanced() {
        // given
        int currencyId = 1;
        long journalId = 1L;
        long quantity1 = 5000L;
        long quantity2 = 5300L;

        JournalLine line1 = JournalLine.builder()
                .journalId(journalId)
                .batchId(1L)
                .credit(Amount.of(quantity1, currencyId))
                .build();

        JournalLine line2 = JournalLine.builder()
                .journalId(journalId)
                .batchId(2L)
                .debit(Amount.of(quantity2, currencyId))
                .build();

        List<JournalLine> expectedJournalLines = List.of(line1, line2);

        JournalType journalType = new JournalType(1, Constants.JournalType.Matched.name());
        when(tokenMatch.buildJournalLines(journalId)).thenReturn(expectedJournalLines);
        when(tokenMatch.getJournalType()).thenReturn(journalType);


        // when
        Exception exception = assertThrows(AccountingMakerException.class, () -> journalMaker.createJournal(tokenMatch));

        // then
        assertThat(exception.getMessage()).contains("Journal Lines are not balanced");
        verifyNoInteractions(journalDAO);

        verify(tokenMatch).getJournalType();
        verify(tokenMatch).buildJournalLines(journalId);
        verify(tokenMatch).isTestAccount();
        verifyNoMoreInteractions(tokenMatch);
    }

}
