package com.timestored.babeldb;

import java.sql.SQLException;

@FunctionalInterface
interface CheckedConsumer<T> {
   void apply(T t) throws SQLException;
}
