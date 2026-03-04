package com.accounting.core.db;

import com.accounting.commons.Constants.BatchStatus;
import com.accounting.core.entities.Batch;
import com.accounting.core.testUtils.LedgerPostgresExtension;
import com.accounting.core.testUtils.TestSqlUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.accounting.commons.Constants.BatchStatus.*;
import static com.accounting.commons.db.AbstractCriteria.SortOrder.ASC;
import static com.accounting.core.db.BatchCriteria.Column.ACCOUNT_ID;
import static com.accounting.core.testUtils.LedgerPostgresExtension.Mode.FOR_EACH_CLASS;
import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class BatchDAOTest {

    @RegisterExtension
    static JdbiExtension db = LedgerPostgresExtension.instance(FOR_EACH_CLASS);

    Handle handle = db.getSharedHandle();
    BatchDAO batchDAO = handle.attach(BatchDAO.class);

    void prepareBatchCriteriaTest(List<Long> accountIds, List<Integer> registerTypeIds) {
        handle.createUpdate("DELETE FROM batch;");
        Random rnd = new Random();
        accountIds.addAll(TestSqlUtils.getListOfLong(handle, "SELECT accountId FROM config.account LIMIT 4;"));
        registerTypeIds.addAll(TestSqlUtils.getListOfInt(handle, "SELECT registerTypeId FROM config.registerType LIMIT 4;"));

        Collections.shuffle(accountIds, rnd);
        Collections.shuffle(registerTypeIds, rnd);

        accountIds.forEach(accountId -> registerTypeIds.forEach(registerTypeId -> {

            int type = rnd.nextInt(4) + 1; // 1-> open, 2-> fixed, 3-> closed, 4-> infinite

            LocalDate periodBeginDate = LocalDate.now();
            LocalDate periodEndDate = null;
            OffsetDateTime closedDate = null;
            switch (type) {
                case 1: //open
                    periodBeginDate = periodBeginDate.withDayOfMonth(1);
                    periodEndDate = periodBeginDate.plusMonths(1);
                    break;
                case 2: //fixed
                    periodBeginDate = periodBeginDate.withDayOfMonth(1).plusMonths(1);
                    periodEndDate = periodBeginDate.plusMonths(1);
                    break;
                case 3: //closed
                    periodBeginDate = periodBeginDate.withDayOfMonth(1).plusMonths(1);
                    periodEndDate = periodBeginDate.plusMonths(1);
                    closedDate = OffsetDateTime.now();
                    break;
                case 4: //infinite
                    periodBeginDate = periodBeginDate.withDayOfMonth(1).plusMonths(1);
                    break;
            }

            batchDAO.insertBatch(Batch.builder()
                    .batchNumber(rnd.nextInt(10))
                    .periodBeginDate(periodBeginDate)
                    .periodEndDate(periodEndDate)
                    .closedDate(closedDate)
                    .registerTypeId(registerTypeId)
                    .accountId(accountId)
                    .build());
        }));
    }


    @Test
    public void testListBatchByCriteriaFilterOnField() {
        List<Long> accountIds = new ArrayList<>();
        List<Integer> registerTypeIds = new ArrayList<>();
        prepareBatchCriteriaTest(accountIds, registerTypeIds);


        List<Batch> result_a1_r1 = batchDAO.listByCriteria(BatchCriteria.builder()
                .registerTypeId(registerTypeIds.get(1))
                .accountId(accountIds.get(1))
                .build());
        assertThat(result_a1_r1).allMatch(batch -> batch.getAccountId() == accountIds.get(1));
        assertThat(result_a1_r1).allMatch(batch -> batch.getRegisterTypeId() == registerTypeIds.get(1));

        List<Batch> result_a1 = batchDAO.listByCriteria(BatchCriteria.builder()
                .accountId(accountIds.get(1))
                .build());
        assertThat(result_a1).allMatch(batch -> batch.getAccountId() == accountIds.get(1));
        assertThat(result_a1).allMatch(batch -> registerTypeIds.stream().anyMatch(i -> i.equals(batch.getRegisterTypeId())));

        List<Batch> result_r1 = batchDAO.listByCriteria(BatchCriteria.builder()
                .registerTypeId(registerTypeIds.get(1))
                .build());
        assertThat(result_r1).allMatch(batch -> accountIds.stream().anyMatch(i -> i.equals(batch.getAccountId())));
        assertThat(result_r1).allMatch(batch -> batch.getRegisterTypeId() == registerTypeIds.get(1));


        List<Batch> result_all = batchDAO.listByCriteria(BatchCriteria.builder()
                .build());
        assertThat(result_all).hasSize(accountIds.size() * registerTypeIds.size());
        assertThat(result_all).allMatch(batch -> accountIds.stream().anyMatch(i -> i.equals(batch.getAccountId())));
        assertThat(result_all).allMatch(batch -> registerTypeIds.stream().anyMatch(i -> i.equals(batch.getRegisterTypeId())));

        List<Batch> result_bn1 = batchDAO.listByCriteria(BatchCriteria.builder()
                .batchNumber(2)
                .build());
        assertThat(result_bn1).allMatch(batch -> batch.getBatchNumber() == 2);

    }

    @Test
    public void testListBatchByCriteriaPaginate() {
        List<Long> accountIds = new ArrayList<>();
        List<Integer> registerTypeIds = new ArrayList<>();
        prepareBatchCriteriaTest(accountIds, registerTypeIds);

        BatchCriteria firstPageCriteria = BatchCriteria.builder()
                .pageSize(5)
                .build();

        BatchCriteria secondPageCriteria = BatchCriteria.builder()
                .page(2)
                .pageSize(5)
                .build();


        List<Batch> result1 = batchDAO.listByCriteria(firstPageCriteria);
        assertThat(result1).hasSize(5);

        List<Batch> result2 = batchDAO.listByCriteria(secondPageCriteria);
        assertThat(result2).hasSize(5);
        assertThat(result2).allMatch(v1 -> !result1.contains(v1));

        long count1 = batchDAO.countByCriteria(firstPageCriteria);
        long count2 = batchDAO.countByCriteria(secondPageCriteria);
        List<Batch> result_all = batchDAO.listByCriteria(BatchCriteria.builder()
                .build());

        assertThat(count1).isEqualTo(count2);
        assertThat(result_all).hasSize((int) count1);
    }

    @SuppressWarnings("unused")
    public static Stream<Arguments> testListBatchByCriteriaOrderingSupplier() {
        return Stream.of(BatchCriteria.Column.values()).map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("testListBatchByCriteriaOrderingSupplier")
    public void testListBatchByCriteriaOrdering(BatchCriteria.Column column) {
        List<Long> accountIds = new ArrayList<>();
        List<Integer> registerTypeIds = new ArrayList<>();
        prepareBatchCriteriaTest(accountIds, registerTypeIds);

        var comparatorAsc = switch (column) {
            case BATCH_ID -> makeComparator(Batch::getBatchId);
            case ACCOUNT_ID -> makeComparator(Batch::getAccountId);
            case REGISTER_TYPE_ID -> makeComparator(Batch::getRegisterTypeId);
            case BATCH_NUMBER -> makeComparator(Batch::getBatchNumber);
            case PERIOD_BEGIN_DATE -> makeComparator(Batch::getPeriodBeginDate);
            case PERIOD_END_DATE -> makeComparator(Batch::getPeriodEndDate);
            case CLOSED_DATE -> makeComparator(Batch::getClosedDate);
            //field not retrieved for now, can't test
            //comparatorAsc = makeComparator(Batch::getCreated);
            case CREATED -> (Comparator<Batch>) null;
        };
        if (isNull(comparatorAsc)) {
            return;
        }
        Comparator<Batch> comparatorDesc = comparatorAsc.reversed();
        comparatorAsc = Comparator.nullsLast(comparatorAsc);
        comparatorDesc = Comparator.nullsLast(comparatorDesc);

        List<Batch> resultAsc = batchDAO.listByCriteria(BatchCriteria.builder()
                .orderBy(Pair.of(column, BatchCriteria.SortOrder.ASC))
                .build());

        assertThat(resultAsc).isSortedAccordingTo(comparatorAsc);

        List<Batch> resultDesc = batchDAO.listByCriteria(BatchCriteria.builder()
                .orderBy(Pair.of(column, BatchCriteria.SortOrder.DESC))
                .build());

        assertThat(resultDesc).isSortedAccordingTo(comparatorDesc);
    }

    @SuppressWarnings("unused")
    public static Stream<Arguments> testListBatchByCriteriaFilterOnBatchStatusSupplier() {
        return Stream.of(BatchStatus.values()).map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("testListBatchByCriteriaFilterOnBatchStatusSupplier")
    public void testListBatchByCriteriaFilterOnBatchStatus(BatchStatus batchStatus) {
        List<Long> accountIds = new ArrayList<>();
        List<Integer> registerTypeIds = new ArrayList<>();
        prepareBatchCriteriaTest(accountIds, registerTypeIds);


        List<Batch> result = batchDAO.listByCriteria(BatchCriteria.builder()
                .batchStatus(batchStatus)
                .build());

        if (batchStatus == OPEN) {
            assertThat(result).allMatch(batch -> isNull(batch.getClosedDate()));
            assertThat(result).allMatch(batch -> !LocalDate.now().isBefore(batch.getPeriodBeginDate()));
            assertThat(result).allMatch(batch -> LocalDate.now().isBefore(batch.getPeriodEndDate()));
        }
        if (batchStatus == FIXED) {
            assertThat(result).allMatch(batch -> isNull(batch.getClosedDate()));
            assertThat(result).allMatch(batch -> !LocalDate.now().isBefore(batch.getPeriodBeginDate()));
            assertThat(result).allMatch(batch -> !LocalDate.now().isBefore(batch.getPeriodEndDate()));
        }
        if (batchStatus == CLOSED) {
            assertThat(result).allMatch(batch -> nonNull(batch.getClosedDate()));
            assertThat(result).allMatch(batch -> !LocalDate.now().isBefore(batch.getPeriodBeginDate()));
            assertThat(result).allMatch(batch -> !LocalDate.now().isBefore(batch.getPeriodEndDate()));
        }
        if (batchStatus == INFINITE) {
            assertThat(result).allMatch(batch -> isNull(batch.getClosedDate()));
            assertThat(result).allMatch(batch -> !LocalDate.now().isBefore(batch.getPeriodBeginDate()));
            assertThat(result).allMatch(batch -> isNull(batch.getPeriodEndDate()));
        }
    }

    @Test
    public void testListBatchByCriteriaFilterOnOpenOrInfinite() {
        List<Long> accountIds = new ArrayList<>();
        List<Integer> registerTypeIds = new ArrayList<>();
        prepareBatchCriteriaTest(accountIds, registerTypeIds);


        List<Batch> result = batchDAO.listByCriteria(BatchCriteria.builder()
                .openOrInfinite(true)
                .build());

        assertThat(result).allMatch(batch -> isNull(batch.getClosedDate()));
        assertThat(result).allMatch(batch -> !LocalDate.now().isBefore(batch.getPeriodBeginDate()));
        assertThat(result).allMatch(batch ->
                isNull(batch.getPeriodEndDate()) || LocalDate.now().isBefore(batch.getPeriodEndDate())
        );
    }


    private <T extends Comparable<? super T>> Comparator<Batch> makeComparator(Function<Batch, ? extends T> keyExtractor) {
        return Comparator.comparing(keyExtractor, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    @Test
    public void testInsertBatch() {
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
        long batchId = batchDAO.insertBatch(expectedBatch);
        expectedBatch.setBatchId(batchId);

        //then
        Batch batchAfterInsert = handle.select("SELECT * FROM public.batch WHERE batchId = ?", batchId)
                .mapToBean(Batch.class)
                .one();

        assertThat(batchAfterInsert).isNotNull();
        assertThat(batchAfterInsert).isEqualTo(expectedBatch);
    }
    @Test
    public void testUpdateBatch() {
        //given
        final Integer accountId = TestSqlUtils.getInt(handle, "SELECT accountId FROM config.account LIMIT 1;");
        final Integer registerTypeId = TestSqlUtils.getInt(handle, "SELECT registerTypeId FROM config.registerType LIMIT 1;");

        assertNotNull(accountId);
        assertNotNull(registerTypeId);

        int batchNumber = 1;
        Batch expectedBatch = Batch.builder()
                .batchNumber(batchNumber)
                .periodBeginDate(LocalDate.now())
                .registerTypeId(registerTypeId)
                .accountId(accountId)
                .build();

        long batchId = batchDAO.insertBatch(expectedBatch);
        expectedBatch.setBatchId(batchId);

        //all allowed changes
        expectedBatch.setPeriodEndDate(LocalDate.now().plusDays(1));
        expectedBatch.setClosedDate(OffsetDateTime.now().withNano(0).minusDays(2));


        //when
        int updatesCount = batchDAO.updateBatch(expectedBatch);


        //then
        assertThat(updatesCount).isEqualTo(1);
        Batch batchAfterInsert = handle.select("SELECT * FROM public.batch WHERE batchId = ?", batchId)
                .mapToBean(Batch.class)
                .one();

        assertThat(batchAfterInsert).isNotNull();
        assertThat(batchAfterInsert).isEqualTo(expectedBatch);
    }


    @Test
    public void testGetOpenOrInfiniteBatchSuccess() {
        //given
        final int batchNumber = 1;
        final Long accountId = TestSqlUtils.getLong(handle, "SELECT accountId FROM config.account LIMIT 1;");
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

        long batchId = batchDAO.insertBatch(expectedBatch);
        expectedBatch.setBatchId(batchId);


        //when
        Batch availableBatch = batchDAO.getByCriteria(BatchCriteria.builder()
                .registerTypeId(registerTypeId)
                .accountId(accountId)
                .openOrInfinite(true)
                .build());

        //then
        assertThat(availableBatch).isNotNull();
        assertThat(availableBatch).isEqualTo(expectedBatch);
    }


    @Test
    public void testGetOpenOrInfiniteNoAvailableBatchFound() {
        //given
        final int batchNumber = 1;
        final Long accountId = TestSqlUtils.getLong(handle, "SELECT accountId FROM config.account LIMIT 1;");
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

        long batchId = batchDAO.insertBatch(expectedBatch);
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
        Batch availableBatch = batchDAO.getByCriteria(BatchCriteria.builder()
                .registerTypeId(registerTypeId)
                .accountId(accountId)
                .openOrInfinite(true)
                .build());

        //then
        assertThat(availableBatch).isNull();
    }

    @Test
    public void testGettingInfiniteBatchesToClose() {

        //given
        final List<Long> accountId = TestSqlUtils.getListOfLong(handle, "SELECT accountId FROM config.account LIMIT 5;");
        final Integer registerTypeId = TestSqlUtils.getInt(handle, "SELECT registerTypeId FROM config.registerType LIMIT 1;");

        assertNotNull(accountId);
        assertNotNull(registerTypeId);


        batchDAO.insertBatch(Batch.builder()
                .batchNumber(1)
                .periodBeginDate(LocalDate.now().minusDays(10))
                .periodEndDate(null)
                .registerTypeId(registerTypeId)
                .accountId(accountId.get(0))
                .build());
        batchDAO.insertBatchPeriod(registerTypeId, accountId.get(0), 'M');

        batchDAO.insertBatch(Batch.builder()
                .batchNumber(3)
                .periodBeginDate(LocalDate.now().minusDays(10))
                .periodEndDate(null)
                .registerTypeId(registerTypeId)
                .accountId(accountId.get(4))
                .build());
        batchDAO.insertBatchPeriod(registerTypeId, accountId.get(4), 'M');

        batchDAO.insertBatch(Batch.builder()
                .batchNumber(5)
                .periodBeginDate(LocalDate.now().minusDays(10))
                .periodEndDate(null)
                .registerTypeId(registerTypeId)
                .accountId(accountId.get(2))
                .build());

        batchDAO.insertBatchPeriod(registerTypeId, accountId.get(3), 'M');

        //when
        List<Batch> infiniteBatchesToClose = batchDAO.listByCriteria(BatchCriteria.builder()
                .batchStatus(BatchStatus.INFINITE)
                .hasPeriodConfiguration(true)
                .accountIds(accountId)
                .registerTypeId(registerTypeId)
                .orderBy(Pair.of(ACCOUNT_ID, ASC))
                .build()
        );

        //then
        assertThat(infiniteBatchesToClose).hasSize(2);
        assertThat(infiniteBatchesToClose.get(0).getAccountId()).isEqualTo(accountId.get(0));
        assertThat(infiniteBatchesToClose.get(1).getAccountId()).isEqualTo(accountId.get(4));
    }

}