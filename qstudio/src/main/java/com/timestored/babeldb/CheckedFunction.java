package com.timestored.babeldb;

import java.sql.SQLException;

public interface CheckedFunction<T, R> { R apply(T t) throws SQLException; }