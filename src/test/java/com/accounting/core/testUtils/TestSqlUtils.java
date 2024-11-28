package com.accounting.core.testUtils;

import org.jdbi.v3.core.Handle;

import java.util.List;

public class TestSqlUtils {

    public static Integer getInt(Handle handle, String sql, Object... args) {
        return handle.select(sql, args)
                .mapTo(Integer.class)
                .findFirst()
                .orElse(null);
    }

    public static Long getLong(Handle handle, String sql, Object... args) {
        return handle.select(sql, args)
                .mapTo(Long.class)
                .findFirst()
                .orElse(null);
    }

    public static List<Integer> getListOfInt(Handle handle, String sql, Object... args) {
        return handle.select(sql, args)
                .mapTo(Integer.class)
                .list();
    }

    public static List<Long> getListOfLong(Handle handle, String sql, Object... args) {
        return handle.select(sql, args)
                .mapTo(Long.class)
                .list();
    }

}
