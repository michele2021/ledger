package com.accounting.core.db;

import com.accounting.commons.entities.Amount;
import com.accounting.core.entities.MismatchedToken;
import com.accounting.core.entities.Tx;
import com.accounting.core.reports.enums.ErrorStatus;
import com.accounting.core.testUtils.LedgerPostgresExtension;
import com.accounting.core.testUtils.TestSqlUtils;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.OffsetDateTime;

import static com.accounting.core.testUtils.LedgerPostgresExtension.Mode.FOR_EACH_CLASS;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class MismatchedTokensDAOTest {

    @RegisterExtension
    static JdbiExtension db = LedgerPostgresExtension.instance(FOR_EACH_CLASS);

    Handle handle = db.getSharedHandle();
    JournalDAO journalDAO = handle.attach(JournalDAO.class);
    MismatchedTokensDAO mismatchedTokensDAO = handle.attach(MismatchedTokensDAO.class);


    @Test
    void testCreateMismatchedToken() {
        // given
        final long reportRowNumber = 2;
        final String txReference = "txReference";
        final OffsetDateTime creationDate = OffsetDateTime.now();
        final Amount amount = Amount.of(5000, 1);

        final Integer txTypeId = TestSqlUtils.getInt(handle, "SELECT txtypeId FROM config.txType LIMIT 1;");
        final Integer accountId = TestSqlUtils.getInt(handle, "SELECT accountid FROM config.account LIMIT 1;");
        final Integer txVariantId = TestSqlUtils.getInt(handle, "SELECT txvariantId FROM config.txVariant LIMIT 1;");

        assertNotNull(txTypeId);
        assertNotNull(accountId);
        assertNotNull(txVariantId);

        Tx tx = Tx.builder()
                .txTypeId(txTypeId)
                .accountId(accountId)
                .amount(amount)
                .txVariantId(txVariantId)
                .creationDate(creationDate)
                .txReference(txReference)
                .build();

        long txId = journalDAO.createTx(tx);
        tx.setTxId(txId);

        MismatchedToken expectedMismatchToken = MismatchedToken.builder()
                .txId(txId)
                .tokenTxId(null)
                .txReference(txReference)
                .status(ErrorStatus.NOT_FOUND)
                .errorMessage(format("Row number: [%d]. Token not found.", reportRowNumber))
                .build();

        MismatchedToken mismatchedTokenBeforeInsert = handle.select("SELECT * FROM public.mismatchedtoken WHERE txId =?", txId)
                .mapToBean(MismatchedToken.class).stream()
                .findFirst()
                .orElse(null);

        assertNull(mismatchedTokenBeforeInsert);

        //when
        mismatchedTokensDAO.createMismatchedToken(expectedMismatchToken);

        //then
        MismatchedToken mismatchedTokenAfterInsert = handle.select("SELECT * FROM public.mismatchedtoken WHERE txId =?", txId)
                .mapToBean(MismatchedToken.class).stream()
                .findFirst()
                .orElse(null);

        assertThat(mismatchedTokenAfterInsert).isNotNull();
        assertThat(mismatchedTokenAfterInsert).isEqualTo(expectedMismatchToken);
    }
}
