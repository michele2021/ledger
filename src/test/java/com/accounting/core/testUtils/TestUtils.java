package com.accounting.core.testUtils;

import com.accounting.commons.repository.*;
import com.accounting.commons.repository.database.AccountDAO;
import com.accounting.core.db.JournalDAO;
import com.accounting.core.db.MismatchedTokensDAO;
import com.accounting.core.queue.SQSAccountingBalanceQueue;
import com.accounting.core.services.RepositoryHelper;
import lombok.NonNull;
import org.jdbi.v3.core.Jdbi;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.mockito.internal.matchers.CapturesArguments;
import org.mockito.internal.matchers.VarargMatcher;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestUtils {


    public static RepositoryHelper setUpMockRepositoryHelper() {
        RepositoryHelper repositoryHelper = mock(RepositoryHelper.class);
        when(repositoryHelper.getJdbi()).thenReturn(mock(Jdbi.class));
        when(repositoryHelper.getAccountRepo()).thenReturn(mock(AccountRepoIf.class));
        when(repositoryHelper.getAccountTypeRepo()).thenReturn(mock(AccountTypeRepoIf.class));
        when(repositoryHelper.getCurrencyRepo()).thenReturn(mock(CurrencyRepoIf.class));
        when(repositoryHelper.getJournalTypeRepo()).thenReturn(mock(JournalTypeRepoIf.class));
        when(repositoryHelper.getRegisterTypeRepo()).thenReturn(mock(RegisterTypeRepoIf.class));
        when(repositoryHelper.getSubsystemRepo()).thenReturn(mock(SubsystemRepoIf.class));
        when(repositoryHelper.getTxTypeRepo()).thenReturn(mock(TxTypeRepoIf.class));
        when(repositoryHelper.getTxVariantRepo()).thenReturn(mock(TxVariantRepoIf.class));
        when(repositoryHelper.getFeeBaseRepo()).thenReturn(mock(FeeBaseRepoIf.class));
        when(repositoryHelper.getAccountDAO()).thenReturn(mock(AccountDAO.class));
        when(repositoryHelper.getJournalDAO()).thenReturn(mock(JournalDAO.class));
        when(repositoryHelper.getMismatchedTokensDAO()).thenReturn(mock(MismatchedTokensDAO.class));
        when(repositoryHelper.getAccountingBalanceQueue()).thenReturn(mock(SQSAccountingBalanceQueue.class));
        return repositoryHelper;
    }


    /**
     * Allows to execute assertion on captured argument, without need to explicitly use an ArgumentCaptor, and all this boilerplate.
     * Does not work for primitive <T>!
     * Example:
     * <pre>
     *     verify(someDao).listByCriteria(assertArg(criteria -> {
     *         assertNotNull(criteria);
     *         assertThat(criteria.getSomething()).isEqualTo(10);
     *     }));
     * </pre>
     * Or single test:
     * <pre>
     *     verify(someDao).listByCriteria(assertArg(criteria -> assertThat(criteria.getSomething()).isEqualTo(10)));
     * </pre>
     *
     * @param lambda the lambda expression with asserts.
     * @param <T>    Type of argument
     */
    public static <T> T assertArg(@NonNull Consumer<T> lambda) {
        return Mockito.argThat(new ArgumentCapturingMatcher<>(lambda));
    }

    /**
     * Version of assertArg to use if method is called many times, the Integer will be call number.
     * Does not work for primitive <T>!
     */
    public static <T> T assertArg(@NonNull BiConsumer<T, Integer> lambda) {
        return Mockito.argThat(new ArgumentCapturingMatcher<>(lambda));
    }

    private static class ArgumentCapturingMatcher<T> implements ArgumentMatcher<T>, CapturesArguments, VarargMatcher {

        private final BiConsumer<T, Integer> verificator;
        private int counter = 0;

        private ArgumentCapturingMatcher(Consumer<T> verificator) {
            this.verificator = (t, i) -> verificator.accept(t);
        }

        private ArgumentCapturingMatcher(BiConsumer<T, Integer> verificator) {
            this.verificator = verificator;
        }

        @Override
        public boolean matches(Object argument) {
            return true;
        }

        @SuppressWarnings("unchecked")
        public void captureFrom(Object argument) {
            verificator.accept((T) argument, ++counter);
        }
    }


}
