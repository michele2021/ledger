package com.accounting.core.db;

import com.accounting.commons.Constants.BatchStatus;
import com.accounting.commons.db.AbstractCriteria;
import com.accounting.commons.db.BindCriteria;
import lombok.*;
import org.apache.commons.lang3.tuple.Pair;
import org.jdbi.v3.core.statement.SqlStatement;

import java.util.List;

import static com.accounting.core.db.BatchCriteria.Column.*;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.BooleanUtils.isFalse;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class BatchCriteria extends AbstractCriteria<BatchCriteria.Column> {

    @Getter
    @SuppressWarnings("unused")
    @AllArgsConstructor
    public enum Column implements CriteriaColumn {
        BATCH_ID("b.batchId"),
        ACCOUNT_ID("b.accountId"),
        REGISTER_TYPE_ID("b.registerTypeId"),
        BATCH_NUMBER("b.batchNumber"),
        PERIOD_BEGIN_DATE("b.periodBeginDate"),
        PERIOD_END_DATE("b.periodEndDate"),
        CLOSED_DATE("b.closedDate"),
        CREATED("b.created");

        final String columnName;


        @Override
        public String getParameterName() {
            return name();
        }
    }

    private Integer registerTypeId;
    private List<Long> accountIds;
    private Integer batchNumber;
    private BatchStatus batchStatus;
    private boolean openOrInfinite;
    private Boolean hasPeriodConfiguration;


    @Builder(toBuilder = true)
    protected BatchCriteria(
            @Singular List<Pair<Column, SortOrder>> orderBys,
            Integer page, Integer pageSize,
            Integer registerTypeId,
            @Singular List<Long> accountIds,
            Integer batchNumber,
            BatchStatus batchStatus,
            boolean openOrInfinite,
            Boolean hasPeriodConfiguration) {
        super(orderBys, page, pageSize);
        this.registerTypeId = registerTypeId;
        this.batchNumber = batchNumber;
        this.accountIds = accountIds;
        this.batchStatus = batchStatus;
        this.openOrInfinite = openOrInfinite;
        this.hasPeriodConfiguration = hasPeriodConfiguration;
    }


    @Override
    protected String generateSQLAndBindParams(BindCriteria bindCriteria, SqlStatement<?> stmt) {
        SQLBuilder<Column> b = new SQLBuilder<>(bindCriteria, stmt);

        b.appendColumnCriterion(registerTypeId, REGISTER_TYPE_ID);
        b.appendColumnCriterion(accountIds, ACCOUNT_ID);
        b.appendColumnCriterion(batchNumber, BATCH_NUMBER);

        appendBatchStateCriterion(b);
        appendOpenOrInfiniteCriterion(b);
        appendHasPeriodConfiguration(b);

        b.processSortingAndPaging(this);
        return b.build();
    }

    private void appendOpenOrInfiniteCriterion(SQLBuilder<Column> b) {
        if (openOrInfinite) {
            b.appendCriterion("b.closedDate IS NULL");
            b.appendCriterion("CURRENT_DATE >= b.periodBeginDate");
            b.appendCriterion("(b.periodEndDate IS NULL OR CURRENT_DATE < b.periodEndDate)");
        }
    }

    private void appendHasPeriodConfiguration(SQLBuilder<Column> b) {
        if (nonNull(hasPeriodConfiguration)) {
            b.appendCriterion(String.format("%s%s",
                    isFalse(hasPeriodConfiguration) ? "NOT " : "",
                    "EXISTS(SELECT 1 FROM config.batchPeriod WHERE accountId = b.accountId and registerTypeId = b.registerTypeId)"));
        }
    }

    private void appendBatchStateCriterion(SQLBuilder<Column> b) {
        if (isNull(batchStatus)) return;
        switch (batchStatus) {
            case OPEN:
                b.appendCriterion("b.closedDate IS NULL");
                b.appendCriterion("CURRENT_DATE >= b.periodBeginDate");
                b.appendCriterion("CURRENT_DATE < b.periodEndDate");
                break;
            case FIXED:
                b.appendCriterion("b.closedDate IS NULL");
                b.appendCriterion("CURRENT_DATE >= b.periodBeginDate");
                b.appendCriterion("CURRENT_DATE >= b.periodEndDate");
                break;
            case CLOSED:
                b.appendCriterion("b.closedDate IS NOT NULL");
                b.appendCriterion("CURRENT_DATE >= b.periodBeginDate");
                b.appendCriterion("CURRENT_DATE >= b.periodEndDate");
                break;
            case INFINITE:
                b.appendCriterion("b.closedDate IS NULL");
                b.appendCriterion("CURRENT_DATE >= b.periodBeginDate");
                b.appendCriterion("b.periodEndDate IS NULL");
                break;
        }

    }

}
