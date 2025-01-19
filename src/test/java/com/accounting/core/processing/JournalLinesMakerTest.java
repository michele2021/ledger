package com.accounting.core.processing;

import com.accounting.commons.Constants;
import com.accounting.commons.entities.Amount;
import com.accounting.commons.entities.RegisterType;
import com.accounting.commons.repository.RegisterTypeRepoIf;
import com.accounting.core.entities.JournalLine;
import com.accounting.core.processing.templates.TokenMatch;
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

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JournalLinesMakerTest {

    @Spy
    private final DailySalesReportRow reportRow = TestDailySalesReportsUtils.getReportRow();

    @Mock
    private JournalMaker journalMaker;

    @Mock
    private RegisterTypeRepoIf registerTypeRepo;

    @InjectMocks
    private TokenMatch tokenMatch;


    @AfterEach
    void reset() {
        Mockito.reset(journalMaker, registerTypeRepo, reportRow);
    }


    @Test
    void testAddDebitLine() {
        // given
        List<JournalLine> journalLines = new ArrayList<>();

        long batchId = 5L;
        long accountId = 2L;
        long journalId = 1L;
        int registerTypeId = 1;
        Amount amount = Amount.of(5000L, 1);

        JournalLine expectedJournalLine = JournalLine.builder()
                .journalId(journalId)
                .batchId(batchId)
                .debit(amount)
                .build();

        when(journalMaker.getOrCreateAvailableBatch(registerTypeId, accountId)).thenReturn(batchId);

        // when
        tokenMatch.addDebitLine(journalId, journalLines, registerTypeId, accountId, amount);

        // then
        assertThat(journalLines).hasSize(1);
        assertThat(journalLines).contains(expectedJournalLine);
        assertThat(journalLines.get(0).getQuantity()).isPositive();

        verify(journalMaker).getOrCreateAvailableBatch(registerTypeId, accountId);
        verifyNoMoreInteractions(registerTypeRepo, journalMaker);
    }

    @Test
    void testAddDebitLine2() {
        // given
        List<JournalLine> journalLines = new ArrayList<>();
        String registerTypeCode = Constants.RegisterType.Fee.name();

        RegisterType registerType = RegisterType.builder()
                .registerTypeId(1)
                .registerTypeCode(registerTypeCode)
                .subsystemId(1)
                .build();

        long batchId = 5L;
        long accountId = 2L;
        long journalId = 1L;
        int registerTypeId = registerType.getRegisterTypeId();
        Amount amount = Amount.of(-5000L, 1);

        JournalLine expectedJournalLine = JournalLine.builder()
                .journalId(journalId)
                .batchId(batchId)
                .debit(amount)
                .build();

        when(registerTypeRepo.get(registerTypeCode)).thenReturn(registerType);
        when(journalMaker.getOrCreateAvailableBatch(registerTypeId, accountId)).thenReturn(batchId);

        // when
        tokenMatch.addDebitLine(journalId, journalLines, Constants.RegisterType.Fee, accountId, amount);

        // then
        assertThat(journalLines).hasSize(1);
        assertThat(journalLines).contains(expectedJournalLine);
        assertThat(journalLines.get(0).getQuantity()).isPositive();

        verify(registerTypeRepo).get(registerTypeCode);
        verify(journalMaker).getOrCreateAvailableBatch(registerTypeId, accountId);
        verifyNoMoreInteractions(registerTypeRepo, journalMaker);
    }

    @Test
    void testAddCreditLine() {
        // given
        long batchId = 5L;
        long accountId = 2L;
        long journalId = 1L;
        int registerTypeId = 3;
        List<JournalLine> journalLines = new ArrayList<>();
        Amount amount = Amount.of(5000L, 1);

        JournalLine expectedJournalLine = JournalLine.builder()
                .journalId(journalId)
                .batchId(batchId)
                .credit(amount)
                .build();

        when(journalMaker.getOrCreateAvailableBatch(registerTypeId, accountId)).thenReturn(batchId);

        // when
        tokenMatch.addCreditLine(journalId, journalLines, registerTypeId, accountId, amount);

        // then
        assertThat(journalLines).hasSize(1);
        assertThat(journalLines).contains(expectedJournalLine);
        assertThat(journalLines.get(0).getQuantity()).isNegative();

        verify(journalMaker).getOrCreateAvailableBatch(registerTypeId, accountId);
        verifyNoMoreInteractions(registerTypeRepo, journalMaker);
    }

    @Test
    void testAddCreditLine2() {
        // given
        List<JournalLine> journalLines = new ArrayList<>();
        String registerTypeCode = Constants.RegisterType.TokenReceivable.name();

        RegisterType registerType = RegisterType.builder()
                .registerTypeId(1)
                .registerTypeCode(registerTypeCode)
                .subsystemId(1)
                .build();

        long batchId = 5L;
        long accountId = 2L;
        long journalId = 1L;
        int registerTypeId = registerType.getRegisterTypeId();
        Amount amount = Amount.of(-5000L, 1);

        JournalLine expectedJournalLine = JournalLine.builder()
                .journalId(journalId)
                .batchId(batchId)
                .credit(amount)
                .build();

        when(registerTypeRepo.get(registerTypeCode)).thenReturn(registerType);
        when(journalMaker.getOrCreateAvailableBatch(registerTypeId, accountId)).thenReturn(batchId);

        // when
        tokenMatch.addCreditLine(journalId, journalLines, Constants.RegisterType.TokenReceivable, accountId, amount);

        // then
        assertThat(journalLines).hasSize(1);
        assertThat(journalLines).contains(expectedJournalLine);
        assertThat(journalLines.get(0).getQuantity()).isNegative();

        verify(registerTypeRepo).get(registerTypeCode);
        verify(journalMaker).getOrCreateAvailableBatch(registerTypeId, accountId);
        verifyNoMoreInteractions(registerTypeRepo, journalMaker);
    }

    @Test
    void getRegisterTypeId() {
        // given
        String registerTypeCode = Constants.RegisterType.Fee.name();

        RegisterType expectedRegisterType = RegisterType.builder()
                .registerTypeId(1)
                .registerTypeCode(registerTypeCode)
                .subsystemId(1)
                .build();

        when(registerTypeRepo.get(registerTypeCode)).thenReturn(expectedRegisterType);

        // when
        int actualRegisterTypeId = tokenMatch.getRegisterTypeId(Constants.RegisterType.Fee);

        // then
        assertEquals(expectedRegisterType.getRegisterTypeId(), actualRegisterTypeId);
        verify(registerTypeRepo).get(registerTypeCode);
        verifyNoMoreInteractions(registerTypeRepo);
    }
}
