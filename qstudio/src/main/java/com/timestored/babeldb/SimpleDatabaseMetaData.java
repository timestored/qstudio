package com.timestored.babeldb;

import static com.timestored.babeldb.BabelDBJdbcDriver.q;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class SimpleDatabaseMetaData implements DatabaseMetaData{
	
	private static final ResultSet EMPTY_RS = new SimpleResultSet(new String[] {"tabb"});
	private final Connection co;
	private final int driverMajorVersion;
	private final int driverMinorVersion;
	
	public ResultSet getCatalogs()throws SQLException{return EMPTY_RS;}
	public ResultSet getSchemas()throws SQLException{return EMPTY_RS;}
	public ResultSet getTableTypes()throws SQLException{return EMPTY_RS;}
	public ResultSet getTables(String a,String b,String t,String[] x)throws SQLException{return EMPTY_RS;}
	public ResultSet getTypeInfo()throws SQLException{return EMPTY_RS;}
	public ResultSet getColumns(String a,String b,String t,String c)throws SQLException{ return EMPTY_RS;}
	
	public ResultSet getPrimaryKeys(String a,String b,String t)throws SQLException{q("getPrimaryKeys not supported"); return EMPTY_RS;}
	public ResultSet getImportedKeys(String a,String b,String t)throws SQLException{q("getImportedKeys not supported");return EMPTY_RS;}
	public ResultSet getProcedures(String a,String b,String p)throws SQLException{q("getProcedures not supported");return EMPTY_RS;}
	public ResultSet getExportedKeys(String a,String b,String t)throws SQLException{q("getExportedKeys not supported");return null;}
	public ResultSet getCrossReference(String pa,String pb,String pt,String fa,String fb,String ft)throws SQLException{q("getCrossReference not supported");return null;}
	public ResultSet getIndexInfo(String a,String b,String t,boolean unique,boolean approximate)throws SQLException{q("getIndexInfo not supported");return null;}
	public ResultSet getProcedureColumns(String a,String b,String p,String c)throws SQLException{q("getProcedureColumns not supported");return null;}
	public ResultSet getColumnPrivileges(String a,String b,String table,String columnNamePattern)throws SQLException{q("getColumnPrivileges not supported");return null;}
	public ResultSet getTablePrivileges(String a,String b,String t)throws SQLException{q("getTablePrivileges not supported");return null;}
	public ResultSet getBestRowIdentifier(String a,String b,String t,int scope,boolean nullable)throws SQLException{q("getBestRowIdentifier not supported");return null;}
	public ResultSet getVersionColumns(String a,String b,String t)throws SQLException{q("getVersionColumns not supported");return null;}
	public boolean allProceduresAreCallable()throws SQLException{return true;}
	public boolean allTablesAreSelectable()throws SQLException{return true;}
	public boolean dataDefinitionCausesTransactionCommit()throws SQLException{return false;}
	public boolean dataDefinitionIgnoredInTransactions()throws SQLException{return false;}
	public boolean doesMaxRowSizeIncludeBlobs()throws SQLException{return true;}
	public String getSchemaTerm()throws SQLException{return"schema";}
	public String getProcedureTerm()throws SQLException{return"procedure";}
	public String getCatalogTerm()throws SQLException{return"catalog";}
	public String getCatalogSeparator()throws SQLException{return".";}
	public int getMaxBinaryLiteralLength()throws SQLException{return 0;}
	public int getMaxCharLiteralLength()throws SQLException{return 0;}
	public int getMaxColumnNameLength()throws SQLException{return 0;}
	public int getMaxColumnsInGroupBy()throws SQLException{return 0;}
	public int getMaxColumnsInIndex()throws SQLException{return 0;}
	public int getMaxColumnsInOrderBy()throws SQLException{return 0;}
	public int getMaxColumnsInSelect()throws SQLException{return 0;}
	public int getMaxColumnsInTable()throws SQLException{return 0;}
	public int getMaxConnections()throws SQLException{return 0;}
	public int getMaxCursorNameLength()throws SQLException{return 0;}
	public int getMaxIndexLength()throws SQLException{return 0;}
	public int getMaxSchemaNameLength()throws SQLException{return 0;}
	public int getMaxProcedureNameLength()throws SQLException{return 0;}
	public int getMaxCatalogNameLength()throws SQLException{return 0;}
	public int getMaxRowSize()throws SQLException{return 0;}
	public int getMaxStatementLength()throws SQLException{return 0;}
	public int getMaxStatements()throws SQLException{return 0;}
	public int getMaxTableNameLength()throws SQLException{return 0;}
	public int getMaxTablesInSelect()throws SQLException{return 0;}
	public int getMaxUserNameLength()throws SQLException{return 0;}
	public int getDefaultTransactionIsolation()throws SQLException{return Connection.TRANSACTION_SERIALIZABLE;}
	public String getSQLKeywords()throws SQLException{return"show,meta,load,save";}
	public String getNumericFunctions()throws SQLException{return"";}
	public String getStringFunctions()throws SQLException{return"";}
	public String getSystemFunctions()throws SQLException{return"";}
	public String getTimeDateFunctions()throws SQLException{return"";}
	public String getSearchStringEscape()throws SQLException{return"";}
	public String getExtraNameCharacters()throws SQLException{return"";}
	public String getIdentifierQuoteString()throws SQLException{return"";}
	public String getURL()throws SQLException{return null;}
	public String getUserName()throws SQLException{return"";}
	public String getDatabaseProductName()throws SQLException{return"rest";}
	public String getDatabaseProductVersion()throws SQLException{return"2.0";}
	public String getDriverName()throws SQLException{return"jdbc";}
	public String getDriverVersion()throws SQLException{return driverMajorVersion+"."+driverMinorVersion;}
	public int getDriverMajorVersion(){return driverMajorVersion;}
	public int getDriverMinorVersion(){return driverMinorVersion;}
	public boolean isCatalogAtStart()throws SQLException{return true;}
	public boolean isReadOnly()throws SQLException{return false;}
	public boolean nullsAreSortedHigh()throws SQLException{return false;}
	public boolean nullsAreSortedLow()throws SQLException{return true;}
	public boolean nullsAreSortedAtStart()throws SQLException{return false;}
	public boolean nullsAreSortedAtEnd()throws SQLException{return false;}
	public boolean supportsMixedCaseIdentifiers()throws SQLException{return false;}
	public boolean storesUpperCaseIdentifiers()throws SQLException{return false;}
	public boolean storesLowerCaseIdentifiers()throws SQLException{return false;}
	public boolean storesMixedCaseIdentifiers()throws SQLException{return true;}
	public boolean supportsMixedCaseQuotedIdentifiers()throws SQLException{return true;}
	public boolean storesUpperCaseQuotedIdentifiers()throws SQLException{return false;}
	public boolean storesLowerCaseQuotedIdentifiers()throws SQLException{return false;}
	public boolean storesMixedCaseQuotedIdentifiers()throws SQLException{return true;}
	public boolean supportsAlterTableWithAddColumn()throws SQLException{return true;}
	public boolean supportsAlterTableWithDropColumn()throws SQLException{return true;}
	public boolean supportsTableCorrelationNames()throws SQLException{return true;}
	public boolean supportsDifferentTableCorrelationNames()throws SQLException{return true;}
	public boolean supportsColumnAliasing()throws SQLException{return true;}
	public boolean nullPlusNonNullIsNull()throws SQLException{return true;}
	public boolean supportsExpressionsInOrderBy()throws SQLException{return true;}
	public boolean supportsOrderByUnrelated()throws SQLException{return false;}
	public boolean supportsGroupBy()throws SQLException{return true;}
	public boolean supportsGroupByUnrelated()throws SQLException{return false;}
	public boolean supportsGroupByBeyondSelect()throws SQLException{return false;}
	public boolean supportsLikeEscapeClause()throws SQLException{return false;}
	public boolean supportsMultipleResultSets()throws SQLException{return false;}
	public boolean supportsMultipleTransactions()throws SQLException{return false;}
	public boolean supportsNonNullableColumns()throws SQLException{return true;}
	public boolean supportsMinimumSQLGrammar()throws SQLException{return true;}
	public boolean supportsCoreSQLGrammar()throws SQLException{return true;}
	public boolean supportsExtendedSQLGrammar()throws SQLException{return false;}
	public boolean supportsANSI92EntryLevelSQL()throws SQLException{return true;}
	public boolean supportsANSI92IntermediateSQL()throws SQLException{return false;}
	public boolean supportsANSI92FullSQL()throws SQLException{return false;}
	public boolean supportsIntegrityEnhancementFacility()throws SQLException{return false;}
	public boolean supportsOuterJoins()throws SQLException{return false;}
	public boolean supportsFullOuterJoins()throws SQLException{return false;}
	public boolean supportsLimitedOuterJoins()throws SQLException{return false;}
	public boolean supportsConvert()throws SQLException{return false;}
	public boolean supportsConvert(int fromType,int toType)throws SQLException{return false;}
	public boolean supportsSchemasInDataManipulation()throws SQLException{return false;}
	public boolean supportsSchemasInProcedureCalls()throws SQLException{return false;}
	public boolean supportsSchemasInTableDefinitions()throws SQLException{return false;}
	public boolean supportsSchemasInIndexDefinitions()throws SQLException{return false;}
	public boolean supportsSchemasInPrivilegeDefinitions()throws SQLException{return false;}
	public boolean supportsCatalogsInDataManipulation()throws SQLException{return false;}
	public boolean supportsCatalogsInProcedureCalls()throws SQLException{return false;}
	public boolean supportsCatalogsInTableDefinitions()throws SQLException{return false;}
	public boolean supportsCatalogsInIndexDefinitions()throws SQLException{return false;}
	public boolean supportsCatalogsInPrivilegeDefinitions()throws SQLException{return false;}
	public boolean supportsSelectForUpdate()throws SQLException{return false;}
	public boolean supportsPositionedDelete()throws SQLException{return false;}
	public boolean supportsPositionedUpdate()throws SQLException{return false;}
	public boolean supportsOpenCursorsAcrossCommit()throws SQLException{return true;}
	public boolean supportsOpenCursorsAcrossRollback()throws SQLException{return true;}
	public boolean supportsOpenStatementsAcrossCommit()throws SQLException{return true;}
	public boolean supportsOpenStatementsAcrossRollback()throws SQLException{return true;}
	public boolean supportsStoredProcedures()throws SQLException{return false;}
	public boolean supportsSubqueriesInComparisons()throws SQLException{return true;}
	public boolean supportsSubqueriesInExists()throws SQLException{return true;}
	public boolean supportsSubqueriesInIns()throws SQLException{return true;}
	public boolean supportsSubqueriesInQuantifieds()throws SQLException{return true;}
	public boolean supportsCorrelatedSubqueries()throws SQLException{return true;}
	public boolean supportsUnion()throws SQLException{return true;}
	public boolean supportsUnionAll()throws SQLException{return true;}
	public boolean supportsTransactions()throws SQLException{return true;}
	public boolean supportsTransactionIsolationLevel(int level)throws SQLException{return true;}
	public boolean supportsDataDefinitionAndDataManipulationTransactions()throws SQLException{return true;}
	public boolean supportsDataManipulationTransactionsOnly()throws SQLException{return false;}
	public boolean usesLocalFiles()throws SQLException{return false;}
	public boolean usesLocalFilePerTable()throws SQLException{return false;}
	public boolean supportsResultSetType(int type)throws SQLException{return type!=ResultSet.TYPE_SCROLL_SENSITIVE;}
	public boolean supportsResultSetConcurrency(int type,int concurrency)throws SQLException{return type==ResultSet.CONCUR_READ_ONLY;}
	public boolean ownUpdatesAreVisible(int type)throws SQLException{return false;}
	public boolean ownDeletesAreVisible(int type)throws SQLException{return false;}
	public boolean ownInsertsAreVisible(int type)throws SQLException{return false;}
	public boolean othersUpdatesAreVisible(int type)throws SQLException{return false;}
	public boolean othersDeletesAreVisible(int type)throws SQLException{return false;}
	public boolean othersInsertsAreVisible(int type)throws SQLException{return false;}
	public boolean updatesAreDetected(int type)throws SQLException{return false;}
	public boolean deletesAreDetected(int type)throws SQLException{return false;}
	public boolean insertsAreDetected(int type)throws SQLException{return false;}
	public boolean supportsBatchUpdates()throws SQLException{return false;}
	public ResultSet getUDTs(String catalog,String schemaPattern,String typeNamePattern,int[]types)throws SQLException{return null;}
	public Connection getConnection()throws SQLException{return co;}
	public boolean supportsSavepoints()throws SQLException{return false;}
	public boolean supportsNamedParameters()throws SQLException{return false;}
	public boolean supportsMultipleOpenResults()throws SQLException{return false;}
	public boolean supportsGetGeneratedKeys()throws SQLException{return false;}
	public ResultSet getSuperTypes(String catalog,String schemaPattern,String typeNamePattern)throws SQLException{return null;}
	public ResultSet getSuperTables(String catalog,String schemaPattern,String tableNamePattern)throws SQLException{return null;}
	public ResultSet getAttributes(String catalog,String schemaPattern,String typeNamePattern,String attributeNamePattern)throws SQLException{return null;}
	public boolean supportsResultSetHoldability(int holdability)throws SQLException{return false;}
	public int getResultSetHoldability()throws SQLException{return 0;}
	public int getDatabaseMajorVersion()throws SQLException{return 0;}
	public int getDatabaseMinorVersion()throws SQLException{return 0;}
	public int getJDBCMajorVersion()throws SQLException{return 0;}
	public int getJDBCMinorVersion()throws SQLException{return 0;}
	public int getSQLStateType()throws SQLException{return 0;}
	public boolean locatorsUpdateCopy()throws SQLException{return false;}
	public boolean supportsStatementPooling()throws SQLException{return false;}
	public RowIdLifetime getRowIdLifetime()throws SQLException{q();return null;}
	public ResultSet getSchemas(String string, String string1)throws SQLException{q();return null;}
	public boolean supportsStoredFunctionsUsingCallSyntax()throws SQLException{q();return false;}
	public boolean autoCommitFailureClosesAllResultSets()throws SQLException{q();return false;}
	public ResultSet getClientInfoProperties()throws SQLException{q();return null;}
	public ResultSet getFunctions(String string, String string1, String string2)throws SQLException{q();return null;}
	public ResultSet getFunctionColumns(String string, String string1, String string2, String string3)throws SQLException{q();return null;}
	public <T> T unwrap(Class<T> type)throws SQLException{q();return null;}
	public boolean isWrapperFor(Class<?> type)throws SQLException{q();return false;}
	public boolean generatedKeyAlwaysReturned(){return false;}
	public ResultSet getPseudoColumns(String catalog,String schemaPattern,String tableNamePattern,String columnNamePattern)throws SQLFeatureNotSupportedException{throw new SQLFeatureNotSupportedException("nyi");}
}