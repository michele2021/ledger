package com.accounting.core.processing.templates;

import com.accounting.commons.Constants;
import com.accounting.commons.entities.Account;
import com.accounting.commons.entities.Amount;
import com.accounting.commons.entities.JournalType;
import com.accounting.commons.exceptions.AccountingMakerException;
import com.accounting.commons.repository.RegisterTypeRepoIf;
import com.accounting.core.db.JournalDAO;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

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
    private final Account account = new Account(16, "DEFAULT", 7,
            Constants.AccountType.Client.name(), 15L, "EPAY", "", null, null, false, OffsetDateTime.now(), "user1");

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

}
