package com.accounting.core.processing.templates;


import com.accounting.commons.Constants;
import com.accounting.commons.entities.*;
import com.accounting.commons.entities.TxVariant;
import com.accounting.commons.exceptions.AccountingMakerException;
import com.accounting.commons.queue.QueueMessage;
import com.accounting.core.entities.*;
import com.accounting.core.processing.JournalMaker;
import com.accounting.core.services.RepositoryHelper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.accounting.core.testUtils.TestUtils.setUpMockRepositoryHelper;
import static java.util.Arrays.asList;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenCreateTest {

    @Mock
    JournalMaker journalMaker;

    @Mock
    Account account;

    @Mock
    Amount amount;

    @Mock
    JournalType journalType;

    @Mock
    TxType txType;

    @Mock
    QueueMessage message;

    @Mock
    TxStatus txStatus;

    RepositoryHelper repositoryHelper = setUpMockRepositoryHelper();
    JsonObject additionalData = new Gson().fromJson("{}", JsonObject.class);

    @BeforeEach
    void init() {
        when(message.additionalData()).thenReturn(additionalData);
    }

    @Test
    void shouldFailToCreateWithoutUUID() {
        //given
        when(message.uuid()).thenReturn(null);
        JsonObject additionalData = new Gson().fromJson("{}", JsonObject.class);
        when(message.additionalData()).thenReturn(additionalData);

        //when
        AccountingMakerException exception = assertThrows(AccountingMakerException.class,
                () -> new TokenCreate(
                        journalMaker, account, amount,
                        journalType, txType, message, repositoryHelper
                ));

        //then
        assertThat(exception).message().isEqualTo("UUID is mandatory for TOKEN_CREATE");
    }

    @Test
    void shouldProcessOK() throws Exception {
        //given
        when(message.uuid()).thenReturn("TxReference");
        JsonObject additionalData = new Gson().fromJson("{}", JsonObject.class);
        when(message.additionalData()).thenReturn(additionalData);

        TokenCreate subject = new TokenCreate(
                journalMaker, account, amount,
                journalType, txType, message, repositoryHelper
        );

        //when
        subject.process();

        //then
        verify(journalMaker).createJournal(same(subject));
    }


    @Test
    void shouldCreateNewTx() throws AccountingMakerException {
        //given
        when(message.uuid()).thenReturn("TxReference");
        JsonObject additionalData = new Gson().fromJson("{}", JsonObject.class);
        when(message.additionalData()).thenReturn(additionalData);

        TokenCreate subject = new TokenCreate(
                journalMaker, account, amount,
                journalType, txType, message, repositoryHelper
        );

        //when & then
        assertThat(subject.addNewTx()).isTrue();
    }

    @Test
    void shouldNotUpdateSpecializedTx() throws Exception {
        //given
        when(message.uuid()).thenReturn("TxReference");
        JsonObject additionalData = new Gson().fromJson("{}", JsonObject.class);
        when(message.additionalData()).thenReturn(additionalData);

        TokenCreate subject = new TokenCreate(
                journalMaker, account, amount,
                journalType, txType, message, repositoryHelper
        );

        //when
        subject.updateSpecializedTx();

        //then
        // verify(repositoryHelper.getJournalDAO(), never()).updateTokenTx(anyLong(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldBuildTx() throws AccountingMakerException {
        //given

        when(message.uuid()).thenReturn("TxReference");
        when(account.getAccountId()).thenReturn(4587L);
        when(txType.getTxTypeId()).thenReturn(345);

        TxVariant txVariant = new TxVariant(123, "code");
        when(repositoryHelper.getTxVariantRepo().get(anyString())).thenReturn(txVariant);

        Tx expectedResult = Tx.builder()
                .txTypeId(345)
                .accountId(4587L)
                .amount(amount)
                .txReference("TxReference")
                .creationDate(null)
                .txVariantId(txVariant.getTxVariantId())
                .build();

        JsonObject additionalData = new Gson().fromJson("{}", JsonObject.class);
        when(message.additionalData()).thenReturn(additionalData);

        TokenCreate subject = new TokenCreate(
                journalMaker, account, amount,
                journalType, txType, message, repositoryHelper
        );

        //when
        Tx result = subject.buildTx();

        //then
        verify(repositoryHelper.getTxVariantRepo()).get("BASE");
        assertThat(result).usingRecursiveComparison().ignoringFields("creationDate").isEqualTo(expectedResult);
        assertThat(result.getCreationDate()).isCloseTo(OffsetDateTime.now(), Assertions.within(1, ChronoUnit.SECONDS));
    }

    @Test
    void shouldBuildSpecializedTx() throws Exception {
        //given
        long txId = 34234L;

        when(message.uuid()).thenReturn("TxReference");

        JsonObject additionalData = new Gson().fromJson("{ " +
                "\"publishableId\" : \"4587458\"," +
                "\"orderItemId\" : \"0123456789\"," +
                "\"orderId\" : \"1234567\"," +
                "\"orderRef\" : \"OrderRef1\"," +
                "\"activatingLocationRef\" : \"POS-LOC2\"" +
                "}", JsonObject.class);
        when(message.additionalData()).thenReturn(additionalData);

        SpecializedTx expectedTokenResult = CCToken.builder()
                .txId(txId)
                .publishableId("4587458")
                .locationRef("POS-LOC2")
                .orderItemId("0123456789")
                .orderId(1234567L)
                .type(Constants.TokenType.POSA)
                .build();

        SpecializedTx expectedOrderResult = OrderDTO.builder()
                .orderNumber(1234567)
                .orderRef("OrderRef1")
                .build();

        TokenCreate subject = new TokenCreate(
                journalMaker, account, amount,
                journalType, txType, message, repositoryHelper
        );

        //when
        List<SpecializedTx> result = subject.buildSpecializedTxs(txId);

        //then
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo(expectedTokenResult);
        assertThat(result.get(1)).isEqualTo(expectedOrderResult);
    }


    @Test
    void shouldBuildJournalLines() throws Exception {
        //given
        final long journalId = 8921738921L;
        final long accountId = 756863L;
        final int currencyId = 342;

        final int registerTypeTokenCreatedId = 564654;
        final int registerTypePhysicalTokenPurchasesId = 7357832;

        final long registerTypeTokenCreatedBatchId = registerTypeTokenCreatedId * 100;
        final long registerTypePhysicalTokenPurchasesBatchId = registerTypePhysicalTokenPurchasesId * 100;

        when(message.uuid()).thenReturn("TxReference");
        JsonObject additionalData = new Gson().fromJson("{}", JsonObject.class);
        when(message.additionalData()).thenReturn(additionalData);

        when(amount.getQuantity()).thenReturn(1000L);
        when(amount.getCurrencyId()).thenReturn(currencyId);


        when(repositoryHelper.getJournalDAO().getTxStatusByReferenceAndAccountId(anyString(), anyLong())).thenReturn(txStatus);


        when(account.getAccountId()).thenReturn(accountId);

        when(repositoryHelper.getRegisterTypeRepo().get("TokenCreated")).thenReturn(RegisterType.builder().registerTypeId(registerTypeTokenCreatedId).build());
        when(repositoryHelper.getRegisterTypeRepo().get("PhysicalTokenPurchases")).thenReturn(RegisterType.builder().registerTypeId(registerTypePhysicalTokenPurchasesId).build());

        when(journalMaker.getOrCreateAvailableBatch(registerTypeTokenCreatedId, accountId)).thenReturn(registerTypeTokenCreatedBatchId);
        when(journalMaker.getOrCreateAvailableBatch(registerTypePhysicalTokenPurchasesId, accountId)).thenReturn(registerTypePhysicalTokenPurchasesBatchId);

        List<JournalLine> expectedJournalLines = asList(
                JournalLine.builder().batchId(registerTypeTokenCreatedBatchId).quantity(1000).journalId(journalId).currencyId(currencyId).build(),
                JournalLine.builder().batchId(registerTypePhysicalTokenPurchasesBatchId).quantity(-1000).journalId(journalId).currencyId(currencyId).build()
        );


        TokenCreate subject = new TokenCreate(
                journalMaker, account, amount,
                journalType, txType, message, repositoryHelper
        );
        subject.process();


        //when
        List<JournalLine> result = subject.buildJournalLines(journalId);

        //then
        verify(journalMaker).getOrCreateAvailableBatch(registerTypeTokenCreatedId, accountId);
        verify(journalMaker).getOrCreateAvailableBatch(registerTypePhysicalTokenPurchasesId, accountId);

        assertThat(result).isEqualTo(expectedJournalLines);

    }

    @Test
    void shouldUseAdditionalDataBookingDate() throws Exception {
        //given
        String bookingDateAsString = "2022-05-15T11:38:32Z";

        additionalData = new Gson().fromJson("{ " +
                "\"publishableId\" : \"4587458\"," +
                "\"bookingDate\" : \"" + bookingDateAsString + "\"," +
                "\"activatingLocationRef\" : \"POS-LOC2\"" +
                "}", JsonObject.class);
        when(message.additionalData()).thenReturn(additionalData);
        when(message.uuid()).thenReturn("uuid1");
        TxVariant txv = new TxVariant();
        txv.setTxVariantCode("BASE");
        txv.setTxVariantId(1);
        when(repositoryHelper.getTxVariantRepo().get(anyString())).thenReturn(txv);

        TokenCreate subject = new TokenCreate(
                journalMaker, account, amount,
                journalType, txType, message, repositoryHelper);

        //when
        OffsetDateTime bd = subject.getJournalBookingDate();
        Tx tx = subject.buildTx();

        //then
        assertThat(bd.toString()).isEqualTo(bookingDateAsString);
        assertThat(tx.getCreationDate().toString()).isEqualTo(bookingDateAsString);
    }

    @Test
    void shouldFailWhenBookingDateInTheFuture() {
        //given
        additionalData = new Gson().fromJson("{ " +
                "\"publishableId\" : \"4587458\"," +
                "\"bookingDate\" : \"2028-12-30T11:00:00.00Z\"," +
                "\"activatingLocationRef\" : \"POS-LOC2\"" +
                "}", JsonObject.class);
        when(message.additionalData()).thenReturn(additionalData);
        when(message.uuid()).thenReturn("uuid1");

        // when
        AccountingMakerException exception = assertThrows(AccountingMakerException.class,
                () -> new TokenCreate(journalMaker, account, amount, journalType, txType, message, repositoryHelper));

        //then
        assertThat(exception.getMessage()).contains("Invalid message.additionalData: the 'bookingDate' cannot be in the future");
    }
}