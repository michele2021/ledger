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
import com.accounting.core.processing.templates.JournalTemplateIf;
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

    @Mock
    private JournalTemplateIf orderTemplate;

    @InjectMocks
    private JournalMaker journalMaker;

    @AfterEach
    void reset() {
        Mockito.reset(journalDAO, tokenMatch, orderTemplate);
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
                .txId(0)
                .journalId(journalId)
                .build();

        when(tokenMatch.buildJournalLines(journalId)).thenReturn(expectedJournalLines);
        when(tokenMatch.getJournalType()).thenReturn(journalType);
        when(tokenMatch.linkTxs()).thenReturn(List.of(txId));
        when(journalDAO.createJournalWithInfoAt(anyInt(), any(), any())).thenReturn(journalId);

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

        verify(journalDAO).createJournalWithInfoAt(eq(journalType.getJournalTypeId()), isNull(), any());

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
                .txId(0)
                .journalId(journalId)
                .build();

        when(tokenMatch.buildJournalLines(1L)).thenReturn(expectedJournalLines);
        when(tokenMatch.getJournalType()).thenReturn(journalType);
        when(tokenMatch.linkTxs()).thenReturn(List.of(txId));
        when(tokenMatch.getJournalBookingDate()).thenReturn(bookingDate);
        when(journalDAO.createJournalWithInfoAt(anyInt(), any(OffsetDateTime.class), any())).thenReturn(journalId);

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

        verify(journalDAO).createJournalWithInfoAt(eq(journalType.getJournalTypeId()), eq(bookingDate), any());

        verify(journalDAO).createJournalTxBulk(journalId, List.of(txId));
        verify(journalDAO).createJournalLinesBulk(expectedJournalLines);

        verifyNoMoreInteractions(journalDAO);
    }


    @Test
    void testCreateJournalWithNewTxAndSpecializedTx() throws AccountingMakerException {
        // given
        int currencyId = 1;
        long journalId = 50L;
        long mainTxId = 10L;

        long debitQuantity = 10000L;
        long creditQuantity = 10000L;

        JournalLine debitLine = JournalLine.builder()
                .journalId(journalId)
                .batchId(1L)
                .debit(Amount.of(debitQuantity, currencyId))
                .build();

        JournalLine creditLine = JournalLine.builder()
                .journalId(journalId)
                .batchId(2L)
                .credit(Amount.of(creditQuantity, currencyId))
                .build();

        List<JournalLine> expectedJournalLines = List.of(debitLine, creditLine);

        JournalType journalType = new JournalType(2, Constants.JournalType.Received.name());
        Tx tx = Tx.builder().txTypeId(1).accountId(100L).build();

        OrderDTO orderDTO = OrderDTO.builder()
                .txId(mainTxId)
                .platformId(1L)
                .orderNumber(12345L)
                .build();

        when(orderTemplate.getJournalType()).thenReturn(journalType);
        when(orderTemplate.buildJournalLines(1L)).thenReturn(expectedJournalLines);
        when(orderTemplate.addNewTx()).thenReturn(true);
        when(orderTemplate.addSpecializedTx()).thenReturn(true);
        when(orderTemplate.buildTx()).thenReturn(tx);
        when(orderTemplate.buildSpecializedTxs(mainTxId)).thenReturn(List.of(orderDTO));

        when(journalDAO.createJournalWithInfoAt(anyInt(), any(), any())).thenReturn(journalId);
        when(journalDAO.createTx(tx)).thenReturn(mainTxId);

        // when
        JournalDb actualJournalDb = journalMaker.createJournal(orderTemplate);

        // then
        JournalDb expectedJournalDb = JournalDb.builder()
                .journalId(journalId)
                .txId(mainTxId)
                .build();
        assertEquals(expectedJournalDb, actualJournalDb);

        verify(orderTemplate).getJournalType();
        verify(orderTemplate).buildJournalLines(1L);
        verify(orderTemplate).addNewTx();
        verify(orderTemplate).addSpecializedTx();
        verify(orderTemplate).buildTx();
        verify(orderTemplate).buildSpecializedTxs(mainTxId);

        verify(journalDAO).createJournalWithInfoAt(eq(journalType.getJournalTypeId()), any(), any());
        verify(journalDAO).createTx(tx);
        verify(journalDAO).createOrder(orderDTO);
        verify(journalDAO).createJournalTxBulk(journalId, List.of(mainTxId));
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
        verifyNoMoreInteractions(tokenMatch);
    }

}
