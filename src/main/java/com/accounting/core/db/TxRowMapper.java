package com.accounting.core.db;

import com.accounting.commons.Constants;
import com.accounting.core.entities.views.TxView;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;

public class TxRowMapper implements RowMapper<TxView> {

    @Override
    public TxView map(ResultSet rs, StatementContext ctx) throws SQLException {
        TxView txv = new TxView();
        Constants.Currencies currency = Constants.Currencies.valueOf(rs.getString("currencyCode"));
        txv.setTxId(rs.getLong("txId"));
        txv.setAccountCode(rs.getString("accountCode"));
        txv.setAccountId(rs.getLong("accountId"));
        txv.setQuantityAsLong(rs.getLong("quantity"));
        txv.setCurrencyCode(currency);
        txv.setCurrencyId(rs.getInt("currencyId"));
        long parentTxId = rs.getLong("parentTxId");
        txv.setParentTxId(rs.wasNull() ? null : parentTxId);
        txv.setTxTypeCode(Constants.TxType.valueOf(rs.getString("txTypeCode")));
        txv.setCreationDateAsOffset(rs.getObject("creationDate", OffsetDateTime.class));
        return txv;
    }
}
