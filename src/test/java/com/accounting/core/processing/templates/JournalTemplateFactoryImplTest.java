package com.accounting.core.processing.templates;

import com.accounting.commons.entities.Account;
import com.accounting.commons.entities.Amount;
import com.accounting.commons.entities.JournalType;
import com.accounting.commons.entities.TxType;
import com.accounting.commons.queue.QueueMessage;
import com.accounting.core.entities.JournalTemplate;
import com.accounting.core.exceptions.AccountingTemplateException;
import com.accounting.core.processing.JournalMaker;
import com.accounting.core.services.RepositoryHelper;
import com.accounting.core.testUtils.TestQueueMessage;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.accounting.core.testUtils.TestUtils.setUpMockRepositoryHelper;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class JournalTemplateFactoryImplTest {

    @Mock
    public JournalMaker journalMaker;

    public RepositoryHelper repositoryHelper = setUpMockRepositoryHelper();

    @InjectMocks
    public JournalTemplateFactoryImpl subject;


    @Test
    void shouldMakeJournalTemplateInstance() throws Exception {
        //given

        final QueueMessage message = TestQueueMessage.tokenCreate();
        final JournalTemplate journalTemplate = JournalTemplate.builder()
                .clazz(JournalTemplateIf4Test.class.getSimpleName())
                .build();
        final Account account = new Account();
        final Amount amount = new Amount();
        final JournalType journalType = new JournalType();
        final TxType txType = new TxType();

        //when
        JournalTemplateIf result = subject.makeJournalTemplateInstance(message, journalTemplate, account, amount, journalType, txType);

        //then
        assertThat(result).isInstanceOf(JournalTemplateIf4Test.class);
        JournalTemplateIf4Test result2 = (JournalTemplateIf4Test) result;

        assertThat(result2.journalType).isSameAs(journalType);
        assertThat(result2.journalMaker).isSameAs(journalMaker);
        assertThat(result2.account).isSameAs(account);
        assertThat(result2.amount).isSameAs(amount);
        assertThat(result2.journalType).isSameAs(journalType);
        assertThat(result2.txType).isSameAs(txType);
        assertThat(result2.message).isSameAs(message);
        assertThat(result2.repositoryHelper).isSameAs(repositoryHelper);

    }
    @Test
    void shouldFailMakeJournalTemplateInstanceIfNoClass() throws Exception {
        //given

        final QueueMessage message = TestQueueMessage.tokenCreate();
        final JournalTemplate journalTemplate = JournalTemplate.builder()
                .clazz(JournalTemplateIf4Test.class.getSimpleName())
                .build();
        final Account account = new Account();
        final Amount amount = new Amount();
        final JournalType journalType = new JournalType();
        final TxType txType = new TxType();

        //when
        JournalTemplateIf result = subject.makeJournalTemplateInstance(message, journalTemplate, account, amount, journalType, txType);

        //then
        assertThat(result).isInstanceOf(JournalTemplateIf4Test.class);
        JournalTemplateIf4Test result2 = (JournalTemplateIf4Test) result;

        assertThat(result2.journalType).isSameAs(journalType);
        assertThat(result2.journalMaker).isSameAs(journalMaker);
        assertThat(result2.account).isSameAs(account);
        assertThat(result2.amount).isSameAs(amount);
        assertThat(result2.journalType).isSameAs(journalType);
        assertThat(result2.txType).isSameAs(txType);
        assertThat(result2.message).isSameAs(message);
        assertThat(result2.repositoryHelper).isSameAs(repositoryHelper);

    }
    @Test
    void shouldFailMakeJournalTemplateInstanceIfNoCtor() {
        //given

        final QueueMessage message = TestQueueMessage.tokenCreate();
        final JournalTemplate journalTemplate = JournalTemplate.builder()
                .clazz(JournalTemplateIf4TestNoCtor.class.getSimpleName())
                .fileName("TestTemplate.json")
                .build();
        final Account account = new Account();
        final Amount amount = new Amount();
        final JournalType journalType = new JournalType();
        final TxType txType = new TxType();

        //when
        AccountingTemplateException exception = Assertions.assertThrows(AccountingTemplateException.class, () -> subject.makeJournalTemplateInstance(message, journalTemplate, account, amount, journalType, txType));

        //then
        AssertionsForClassTypes.assertThat(exception.getMessage()).startsWith("Exception while trying to instantiate class JournalTemplateIf4TestNoCtor for the template TestTemplate.json");

    }

}