package com.accounting.core.db;

import com.accounting.commons.db.BindCriteria;
import com.accounting.core.entities.Batch;
import com.accounting.core.entities.BatchPeriod;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.AllowUnusedBindings;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transactional;

import java.util.List;

public interface BatchDAO extends Transactional<BatchDAO> {

    @SqlQuery("SELECT b.batchId \n" +
            "      ,b.accountId \n" +
            "      ,b.registerTypeId \n" +
            "      ,b.batchNumber\n" +
            "      ,b.periodBeginDate\n" +
            "      ,b.periodEndDate\n" +
            " FROM batch b " +
            " <CRITERIA>")
    @RegisterBeanMapper(Batch.class)
    @AllowUnusedBindings
    Batch getByCriteria(@BindCriteria BatchCriteria batchCriteria);

    @SqlQuery("SELECT b.batchId \n" +
            "      ,b.accountId \n" +
            "      ,b.registerTypeId \n" +
            "      ,b.batchNumber\n" +
            "      ,b.periodBeginDate\n" +
            "      ,b.periodEndDate\n" +
            " FROM batch b " +
            " <CRITERIA>"
    )
    @RegisterBeanMapper(Batch.class)
    List<Batch> listByCriteria(@BindCriteria BatchCriteria batchCriteria);

    @SqlQuery("SELECT count(b.batchId)" +
            " FROM batch b " +
            " <CRITERIA>")
    @RegisterBeanMapper(Batch.class)
    long countByCriteria(@BindCriteria(forCount = true) BatchCriteria batchCriteria);


    @SqlUpdate("INSERT INTO batch(registerTypeId, accountId, batchNumber, periodBeginDate, periodEndDate) " +
            "VALUES (:registerTypeId, :accountId, :batchNumber, :periodBeginDate, :periodEndDate)")
    @GetGeneratedKeys("batchid")
    long insertBatch(@BindBean Batch b);

    @SqlUpdate("UPDATE batch " +
            //Note: so far only this two fields can change
            " SET periodEndDate = :periodEndDate " +
            "    ,closedDate = :closedDate " +
            " WHERE batchId = :batchId")
    int updateBatch(@BindBean Batch b);


    @SqlUpdate("LOCK TABLE batch IN ACCESS EXCLUSIVE MODE")
    void lockTableForNewBatchCreation();


    //BatchPeriod methods

    @SqlQuery("SELECT registerTypeId, accountId, period" +
            " FROM config.batchPeriod" +
            " WHERE registerTypeId = :registerTypeId" +
            "   AND accountId =:accountId")
    @RegisterBeanMapper(BatchPeriod.class)
    BatchPeriod getBatchPeriod(int registerTypeId, long accountId);

    @SqlUpdate("INSERT INTO config.batchPeriod(registerTypeId, accountId, period)" +
            " VALUES (:registerTypeId, :accountId, :period)")
    @RegisterBeanMapper(BatchPeriod.class)
    void insertBatchPeriod(int registerTypeId, long accountId, Character period);

}
