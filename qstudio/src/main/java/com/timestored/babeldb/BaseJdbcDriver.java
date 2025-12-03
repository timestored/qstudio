package com.timestored.babeldb;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;

import lombok.RequiredArgsConstructor;


/**
 * JDBC Driver with most functions implement, two ways to usefully use:
 *  1. Inherit and override only executeQry. Useful for creating drivers that query JSON data source.
 *  2. Inherit and call BaseJdbcDriver(String jdbcPrefix, ConnectionSupplier connectionSupplier)
 *     This will allow you to pass through connections and queries.
 */
@RequiredArgsConstructor
abstract class BaseJdbcDriver implements Driver{
	
	private String u = "";
	String pas = "";
	private final String jdbcPrefix;
	
	public abstract ResultSet executeQry(String sql, int millisStalenessPermitted)throws SQLException, IOException;
	public abstract int getMajorVersion();
	public abstract int getMinorVersion();
	public abstract DatabaseMetaData getDatabaseMetaData(Connection conn);
	
	public static interface ConnectionSupplier {
		public Connection connect(String s,Properties p)throws SQLException;
	}
	
	public boolean jdbcCompliant(){return false;}

	public boolean acceptsURL(String s){return s.startsWith(jdbcPrefix);}
	
	public Connection connect(String jdbcURL,Properties p)throws SQLException{
		if(!acceptsURL(jdbcURL)) {
			return null;
		}
		// Else fall back to one that does nothing. No connect really needed.
		String hpe = jdbcURL.substring(jdbcPrefix.length());
		u = p!=null?""+p.get("user"):"";
		pas = p!=null?""+p.get("password"):"";
		return new co(jdbcURL,p);
	}
	
	public DriverPropertyInfo[]getPropertyInfo(String s,Properties p)throws SQLException{return new DriverPropertyInfo[0];}
	
	static void q(String s)throws SQLException{throw new SQLException(s);}
	static void q()throws SQLException{throw new SQLFeatureNotSupportedException("nyi");}
	static void q(Exception e)throws SQLException{throw new SQLException(e.getMessage());}
	
	@RequiredArgsConstructor
	public class co implements Connection{
		 private final String jdbcURL;
		private final Properties p;
		
		 public Object getMoreRows()throws SQLException{
		     throw new SQLException("getMoreRows unsupported");
		 }
		 
		 private boolean autocommit=true;
		 private boolean readOnly;
		 public void setAutoCommit(boolean b)throws SQLException{autocommit=b;}
		 public boolean getAutoCommit()throws SQLException{return autocommit;}
		 public void rollback()throws SQLException{}
		 public void commit()throws SQLException{}
		 public boolean isClosed()throws SQLException{return false;}
		 public Statement createStatement()throws SQLException{return new st(this);}
		 public DatabaseMetaData getMetaData()throws SQLException{return getDatabaseMetaData(this);}
		 public PreparedStatement prepareStatement(String s)throws SQLException{return new ps(this,s);}
		 public CallableStatement prepareCall(String s)throws SQLException{return new cs(this,s);}
		 public String nativeSQL(String s)throws SQLException{return s;}
		 private int i=TRANSACTION_SERIALIZABLE;
		 private int h=ResultSet.HOLD_CURSORS_OVER_COMMIT;
		 public void setReadOnly(boolean x)throws SQLException{readOnly=x;}
		 public boolean isReadOnly()throws SQLException{return readOnly;}
		 public void setCatalog(String s)throws SQLException{q("setCatalog not supported");}
		 public String getCatalog()throws SQLException{q("getCatalog not supported");return null;}
		 public void setTransactionIsolation(int x)throws SQLException{i=x;}
		 public int getTransactionIsolation()throws SQLException{return i;}
		 public SQLWarning getWarnings()throws SQLException{return null;}
		 public void clearWarnings()throws SQLException{}
		 public void close()throws SQLException{ }
		 public Statement createStatement(int resultSetType,int resultSetConcurrency)throws SQLException{return new st(this);}
		 public PreparedStatement prepareStatement(String s,int resultSetType,int resultSetConcurrency)throws SQLException{return new ps(this,s);}
		 public CallableStatement prepareCall(String s,int resultSetType,int resultSetConcurrency)throws SQLException{return new cs(this,s);}
		 public Map<String, Class<?>> getTypeMap()throws SQLException{return null;}
		 public void setTypeMap(Map map)throws SQLException{}
		//3
		 public void setHoldability(int holdability)throws SQLException{h=holdability;}
		 public int getHoldability()throws SQLException{return h;}
		 public Savepoint setSavepoint()throws SQLException{q("setSavepoint not supported");return null;}
		 public Savepoint setSavepoint(String name)throws SQLException{q("setSavepoint not supported");return null;}
		 public void rollback(Savepoint savepoint)throws SQLException{}
		 public void releaseSavepoint(Savepoint savepoint)throws SQLException{}
		 public Statement createStatement(int resultSetType,int resultSetConcurrency,int resultSetHoldability)throws SQLException{return new st(this);}
		 public PreparedStatement prepareStatement(String s,int resultSetType,int resultSetConcurrency,int resultSetHoldability)throws SQLException{return new ps(this,s);}
		 public CallableStatement prepareCall(String s,int resultSetType,int resultSetConcurrency,int resultSetHoldability)throws SQLException{return new cs(this,s);}
		 public PreparedStatement prepareStatement(String s,int autoGeneratedKeys)throws SQLException{return new ps(this,s);}
		 public PreparedStatement prepareStatement(String s,int[]columnIndexes)throws SQLException{return new ps(this,s);}
		 public PreparedStatement prepareStatement(String s,String[]columnNames)throws SQLException{return new ps(this,s);}
		//4
		 private Properties clientInfo=new Properties();
		 public Clob createClob()throws SQLException{q();return null;}
		 public Blob createBlob()throws SQLException{q();return null;}
		 public NClob createNClob()throws SQLException{q();return null;}
		 public SQLXML createSQLXML()throws SQLException{q();return null;}
		 public boolean isValid(int i)throws SQLException{ return true; }
		 public void setClientInfo(String k, String v)throws SQLClientInfoException{clientInfo.setProperty(k,v);}
		 public void setClientInfo(Properties p)throws SQLClientInfoException{clientInfo=p;}
		 public String getClientInfo(String k)throws SQLException{return (String)clientInfo.get(k);}
		 public Properties getClientInfo()throws SQLException{return clientInfo;}
		 public Array createArrayOf(String string, Object[] os)throws SQLException{q();return null;}
		 public Struct createStruct(String string, Object[] os)throws SQLException{q();return null;}
		 public <T> T unwrap(Class<T> type)throws SQLException{q();return null;}
		 public boolean isWrapperFor(Class<?> type)throws SQLException{q();return false;}
		//1.7
		 public int getNetworkTimeout()throws SQLFeatureNotSupportedException{throw new SQLFeatureNotSupportedException("nyi");}
		 public void setNetworkTimeout(Executor executor,int milliseconds)throws SQLFeatureNotSupportedException{throw new SQLFeatureNotSupportedException("nyi");}
		 public void abort(Executor executor)throws SQLFeatureNotSupportedException{throw new SQLFeatureNotSupportedException("nyi");}
		 public void setSchema(String s){}
		 public String getSchema(){return null;}
	}
	
	public class st implements Statement{
		 private Connection co;
		 private ResultSet resultSet;
		 private int maxRows=0;
		 private int timeOut;
		 private int fetchSize=0;
		 protected Object[]p={};
		 public st(Connection x){co=x;}
		 
		 public int executeUpdate(String s)throws SQLException{
			 executeQuery(s);
			 return 0;
		}
		 
		 public ResultSet executeQuery(String s)throws SQLException{
		   if(!execute(s))q("Statement did not produce a ResultSet");
		   return getResultSet();
		 }
		 
		 public boolean execute(String sql)throws SQLException{
		     try {
		    	 resultSet = executeQry(sql, 0);
			} catch (IOException e) {
				throw new SQLException(e);
			} 
		     return true;
		 }
		 
		 public ResultSet getResultSet()throws SQLException{return resultSet;}
		 public int getUpdateCount(){return -1;}
		 public int getMaxRows()throws SQLException{return maxRows;}
		 public void setMaxRows(int n)throws SQLException{
		   if(n<0)
		     q("setMaxRows(int), rows must be >=0. Passed "+n);
		   maxRows=n;
		 }
		 public int getQueryTimeout()throws SQLException{return timeOut;}
		 public void setQueryTimeout(int i)throws SQLException{timeOut=i;}
		 // truncate excess BINARY,VARBINARY,LONGVARBINARY,CHAR,VARCHAR,and LONGVARCHAR fields
		 public int getMaxFieldSize()throws SQLException{return 0;}
		 public void setMaxFieldSize(int i)throws SQLException{}
		 public void setEscapeProcessing(boolean b)throws SQLException{}
		 public void cancel()throws SQLException{}
		 public SQLWarning getWarnings()throws SQLException{return null;}
		 public void clearWarnings()throws SQLException{}
		 // positioned update? different statement?
		 public void setCursorName(String name)throws SQLException{q("setCursorName not supported");}
		 public boolean getMoreResults()throws SQLException{return false;}
		 public void close()throws SQLException{
		  if(resultSet!=null)
		    resultSet.close();
		  resultSet=null;
		  co=null;
		 }
		 public void setFetchDirection(int direction)throws SQLException{q("setFetchDirection not supported");}
		 public int getFetchDirection()throws SQLException{return 0;}
		 public void setFetchSize(int rows)throws SQLException{
		  if(fetchSize<0)
		    throw new SQLException("setFetchSize(rows), rows must be >=0. Passed"+fetchSize);
		  fetchSize=rows;
		 }
		 public int getFetchSize()throws SQLException{return fetchSize;}
		 public int getResultSetConcurrency()throws SQLException{return ResultSet.CONCUR_READ_ONLY;}
		 public int getResultSetType()throws SQLException{return ResultSet.TYPE_SCROLL_INSENSITIVE;}
		 public void addBatch(String sql)throws SQLException{q("addBatch not supported");}
		 public void clearBatch()throws SQLException{}
		 public int[]executeBatch()throws SQLException{return new int[0];}
		 public Connection getConnection()throws SQLException{return co;}
		//3
		 public boolean getMoreResults(int current)throws SQLException{return false;}
		 public ResultSet getGeneratedKeys()throws SQLException{return null;}
		 public int executeUpdate(String sql,int autoGeneratedKeys)throws SQLException{q("a");return 0;}
		 public int executeUpdate(String sql,int[]columnIndexes)throws SQLException{q("a");return 0;}
		 public int executeUpdate(String sql,String[]columnNames)throws SQLException{q("a");return 0;}
		 public boolean execute(String sql,int autoGeneratedKeys)throws SQLException{q("a");return false;}
		 public boolean execute(String sql,int[]columnIndexes)throws SQLException{q("a");return false;}
		 public boolean execute(String sql,String[]columnNames)throws SQLException{q("a");return false;}
		 public int getResultSetHoldability()throws SQLException{return ResultSet.HOLD_CURSORS_OVER_COMMIT;}
		//4
		 boolean poolable=false;
		 public boolean isClosed()throws SQLException{return co==null||co.isClosed();}
		 public void setPoolable(boolean b)throws SQLException{
		  if(isClosed())
		    throw new SQLException("Closed");
		  poolable=b;
		 }
		 public boolean isPoolable()throws SQLException{
		  if(isClosed())
		    throw new SQLException("Closed");
		  return poolable;
		 }
		 public <T> T unwrap(Class<T> type)throws SQLException{q();return null;}
		 public boolean isWrapperFor(Class<?> type)throws SQLException{q();return false;}
		//1.7
		 boolean _closeOnCompletion=false;
		 public void closeOnCompletion(){_closeOnCompletion=true;}
		 public boolean isCloseOnCompletion(){return _closeOnCompletion;}
	}
	
	public class ps extends st implements PreparedStatement{
		 private String s;
		 public ps(Connection co,String x){super(co);s=x;}
		 public ResultSet executeQuery()throws SQLException{return executeQuery(s);}
		 public int executeUpdate()throws SQLException{return executeUpdate(s);}
		 public boolean execute()throws SQLException{return execute(s);}
		 public void clearParameters()throws SQLException{q("err");}
		 public void setObject(int i,Object x)throws SQLException{q("err"); }
		 public void setObject(int i,Object x,int targetSqlType)throws SQLException{setObject(i,x);}
		 public void setObject(int i,Object x,int targetSqlType,int scale)throws SQLException{setObject(i,x);}
		 public void setNull(int i,int t)throws SQLException{q("err");}
		 public void setBoolean(int i,boolean x)throws SQLException{setObject(i,Boolean.valueOf(x));}
		 public void setByte(int i,byte x)throws SQLException{setObject(i,Byte.valueOf(x));}
		 public void setShort(int i,short x)throws SQLException{setObject(i,Short.valueOf(x));}
		 public void setInt(int i,int x)throws SQLException{setObject(i,Integer.valueOf(x));}
		 public void setLong(int i,long x)throws SQLException{setObject(i,Long.valueOf(x));}
		 public void setFloat(int i,float x)throws SQLException{setObject(i,Float.valueOf(x));}
		 public void setDouble(int i,double x)throws SQLException{setObject(i,Double.valueOf(x));}
		 public void setString(int i,String x)throws SQLException{setObject(i,x);}
		 public void setDate(int i,Date x)throws SQLException{setObject(i,x);}
		 public void setTime(int i,Time x)throws SQLException{setObject(i,x);}
		 public void setTimestamp(int i,Timestamp x)throws SQLException{setObject(i,x);}
		 public void setBytes(int i,byte[] x)throws SQLException{q();}
		 public void setBigDecimal(int i,BigDecimal x)throws SQLException{q();}
		 public void setAsciiStream(int i,InputStream x,int length)throws SQLException{q();}
		 @Deprecated
		 public void setUnicodeStream(int i,InputStream x,int length)throws SQLException{q();}
		 public void setBinaryStream(int i,InputStream x,int length)throws SQLException{q();}
		 public void addBatch()throws SQLException{}
		 public void setCharacterStream(int parameterIndex,Reader reader,int length)throws SQLException{q();}
		 public void setRef(int i,Ref x)throws SQLException{q();}
		 public void setBlob(int i,Blob x)throws SQLException{q();}
		 public void setClob(int i,Clob x)throws SQLException{q();}
		 public void setArray(int i,Array x)throws SQLException{q();}
		 public ResultSetMetaData getMetaData()throws SQLException{q("getMetaData not supported");return null;}
		 public void setDate(int parameterIndex,Date x,Calendar cal)throws SQLException{q();}
		 public void setTime(int parameterIndex,Time x,Calendar cal)throws SQLException{q();}
		 public void setTimestamp(int parameterIndex,Timestamp x,Calendar cal)throws SQLException{q();}
		 public void setNull(int paramIndex,int sqlType,String typeName)throws SQLException{q();}
		//3
		 public void setURL(int parameterIndex,URL x)throws SQLException{q();}
		 public ParameterMetaData getParameterMetaData()throws SQLException{q("getParameterMetaData not supported");return null;}
		//4
		 public void setRowId(int i,RowId rowid)throws SQLException{q();}
		 public void setNString(int i,String string)throws SQLException{q();}
		 public void setNCharacterStream(int i,Reader reader,long l)throws SQLException{q();}
		 public void setNClob(int i,NClob nclob)throws SQLException{q();}
		 public void setClob(int i,Reader reader, long l)throws SQLException{q();}
		 public void setBlob(int i,InputStream in, long l)throws SQLException{q();}
		 public void setNClob(int i,Reader reader, long l)throws SQLException{q();}
		 public void setSQLXML(int i,SQLXML sqlxml)throws SQLException{q();}
		 public void setAsciiStream(int i,InputStream in,long l)throws SQLException{q();}
		 public void setBinaryStream(int i,InputStream in,long l)throws SQLException{q();}
		 public void setCharacterStream(int i,Reader reader,long l)throws SQLException{q();}
		 public void setAsciiStream(int i,InputStream in)throws SQLException{q();}
		 public void setBinaryStream(int i,InputStream in)throws SQLException{q();}
		 public void setCharacterStream(int i,Reader reader)throws SQLException{q();}
		 public void setNCharacterStream(int i,Reader reader)throws SQLException{q();}
		 public void setClob(int i,Reader reader)throws SQLException{q();}
		 public void setBlob(int i,InputStream in)throws SQLException{q();}
		 public void setNClob(int i,Reader reader)throws SQLException{q();}
	}
	
	public class cs extends ps implements CallableStatement{
		 public cs(Connection c,String s){super(c,s);}
		 public void registerOutParameter(int i,int sqlType)throws SQLException{}
		 public void registerOutParameter(int i,int sqlType,int scale)throws SQLException{}
		 public boolean wasNull()throws SQLException{return false;}
		 public String getString(int i)throws SQLException{return null;}
		 public boolean getBoolean(int i)throws SQLException{return false;}
		 public byte getByte(int i)throws SQLException{return 0;}
		 public short getShort(int i)throws SQLException{return 0;}
		 public int getInt(int i)throws SQLException{return 0;}
		 public long getLong(int i)throws SQLException{return 0;}
		 public float getFloat(int i)throws SQLException{return(float)0.0;}
		 public double getDouble(int i)throws SQLException{return 0.0;}
		 @Deprecated
		 public BigDecimal getBigDecimal(int i,int scale)throws SQLException{return null;}
		 public Date getDate(int i)throws SQLException{return null;}
		 public Time getTime(int i)throws SQLException{return null;}
		 public Timestamp getTimestamp(int i)throws SQLException{return null;}
		 public byte[]getBytes(int i)throws SQLException{return null;}
		 public Object getObject(int i)throws SQLException{return null;}
		 public BigDecimal getBigDecimal(int parameterIndex)throws SQLException{q();return null;}
		 public Object getObject(int i,Map map)throws SQLException{q();return null;}
		 public Ref getRef(int i)throws SQLException{q();return null;}
		 public Blob getBlob(int i)throws SQLException{q();return null;}
		 public Clob getClob(int i)throws SQLException{q();return null;}
		 public Array getArray(int i)throws SQLException{q();return null;}
		 public Date getDate(int parameterIndex,Calendar cal)throws SQLException{q();return null;}
		 public Time getTime(int parameterIndex,Calendar cal)throws SQLException{q();return null;}
		 public Timestamp getTimestamp(int parameterIndex,Calendar cal)throws SQLException{q();return null;}
		 public void registerOutParameter(int paramIndex,int sqlType,String typeName)throws SQLException{q();}
		//3
		 public void registerOutParameter(String parameterName,int sqlType)throws SQLException{q();}
		 public void registerOutParameter(String parameterName,int sqlType,int scale)throws SQLException{q();}
		 public void registerOutParameter(String parameterName,int sqlType,String typeName)throws SQLException{q();}
		 public URL getURL(int parameterIndex)throws SQLException{q();return null;}
		 public void setURL(String parameterName,URL val)throws SQLException{q();}
		 public void setNull(String parameterName,int sqlType)throws SQLException{q();}
		 public void setBoolean(String parameterName,boolean x)throws SQLException{q();}
		 public void setByte(String parameterName,byte x)throws SQLException{q();}
		 public void setShort(String parameterName,short x)throws SQLException{q();}
		 public void setInt(String parameterName,int x)throws SQLException{q();}
		 public void setLong(String parameterName,long x)throws SQLException{q();}
		 public void setFloat(String parameterName,float x)throws SQLException{q();}
		 public void setDouble(String parameterName,double x)throws SQLException{q();}
		 public void setBigDecimal(String parameterName,BigDecimal x)throws SQLException{q();}
		 public void setString(String parameterName,String x)throws SQLException{q();}
		 public void setBytes(String parameterName,byte[]x)throws SQLException{q();}
		 public void setDate(String parameterName,Date x)throws SQLException{q();}
		 public void setTime(String parameterName,Time x)throws SQLException{q();}
		 public void setTimestamp(String parameterName,Timestamp x)throws SQLException{q();}
		 public void setAsciiStream(String parameterName,InputStream x,int length)throws SQLException{q();}
		 public void setBinaryStream(String parameterName,InputStream x,int length)throws SQLException{q();}
		 public void setObject(String parameterName,Object x,int targetSqlType,int scale)throws SQLException{q();}
		 public void setObject(String parameterName,Object x,int targetSqlType)throws SQLException{q();}
		 public void setObject(String parameterName,Object x)throws SQLException{q();}
		 public void setCharacterStream(String parameterName,Reader reader,int length)throws SQLException{q();}
		 public void setDate(String parameterName,Date x,Calendar cal)throws SQLException{q();}
		 public void setTime(String parameterName,Time x,Calendar cal)throws SQLException{q();}
		 public void setTimestamp(String parameterName,Timestamp x,Calendar cal)throws SQLException{q();}
		 public void setNull(String parameterName,int sqlType,String typeName)throws SQLException{q();}
		 public String getString(String parameterName)throws SQLException{return null;}
		 public boolean getBoolean(String parameterName)throws SQLException{return false;}
		 public byte getByte(String parameterName)throws SQLException{return 0;}
		 public short getShort(String parameterName)throws SQLException{return 0;}
		 public int getInt(String parameterName)throws SQLException{return 0;}
		 public long getLong(String parameterName)throws SQLException{return 0;}
		 public float getFloat(String parameterName)throws SQLException{return 0;}
		 public double getDouble(String parameterName)throws SQLException{return 0;}
		 public byte[]getBytes(String parameterName)throws SQLException{return null;}
		 public Date getDate(String parameterName)throws SQLException{return null;}
		 public Time getTime(String parameterName)throws SQLException{return null;}
		 public Timestamp getTimestamp(String parameterName)throws SQLException{return null;}
		 public Object getObject(String parameterName)throws SQLException{return null;}
		 public BigDecimal getBigDecimal(String parameterName)throws SQLException{return null;}
		 public Object getObject(String parameterName,Map map)throws SQLException{return null;}
		 public Ref getRef(String parameterName)throws SQLException{return null;}
		 public Blob getBlob(String parameterName)throws SQLException{return null;}
		 public Clob getClob(String parameterName)throws SQLException{return null;}
		 public Array getArray(String parameterName)throws SQLException{return null;}
		 public Date getDate(String parameterName,Calendar cal)throws SQLException{return null;}
		 public Time getTime(String parameterName,Calendar cal)throws SQLException{return null;}
		 public Timestamp getTimestamp(String parameterName,Calendar cal)throws SQLException{return null;}
		 public URL getURL(String parameterName)throws SQLException{return null;}
		//4
		 public RowId getRowId(int i)throws SQLException{q();return null;}
		 public RowId getRowId(String string)throws SQLException{q();return null;}
		 public void setRowId(String string, RowId rowid)throws SQLException{q();}
		 public void setNString(String string, String string1)throws SQLException{q();}
		 public void setNCharacterStream(String string, Reader reader, long l)throws SQLException{q();}
		 public void setNClob(String string, NClob nclob)throws SQLException{q();}
		 public void setClob(String string, Reader reader, long l)throws SQLException{q();}
		 public void setBlob(String string, InputStream in, long l)throws SQLException{q();}
		 public void setNClob(String string, Reader reader, long l)throws SQLException{q();}
		 public NClob getNClob(int i)throws SQLException{q();return null;}
		 public NClob getNClob(String string)throws SQLException{q();return null;}
		 public void setSQLXML(String string, SQLXML sqlxml)throws SQLException{q();}
		 public SQLXML getSQLXML(int i)throws SQLException{q();return null;}
		 public SQLXML getSQLXML(String string)throws SQLException{q();return null;}
		 public String getNString(int i)throws SQLException{q();return null;}
		 public String getNString(String string)throws SQLException{q();return null;}
		 public Reader getNCharacterStream(int i)throws SQLException{q();return null;}
		 public Reader getNCharacterStream(String string)throws SQLException{q();return null;}
		 public Reader getCharacterStream(int i)throws SQLException{q();return null;}
		 public Reader getCharacterStream(String string)throws SQLException{q();return null;}
		 public void setBlob(String string, Blob blob)throws SQLException{q();}
		 public void setClob(String string, Clob clob)throws SQLException{q();}
		 public void setAsciiStream(String string, InputStream in, long l)throws SQLException{q();}
		 public void setBinaryStream(String string, InputStream in, long l)throws SQLException{q();}
		 public void setCharacterStream(String string, Reader reader, long l)throws SQLException{q();}
		 public void setAsciiStream(String string, InputStream in)throws SQLException{q();}
		 public void setBinaryStream(String string, InputStream in)throws SQLException{q();}
		 public void setCharacterStream(String string, Reader reader)throws SQLException{q();}
		 public void setNCharacterStream(String string, Reader reader)throws SQLException{q();}
		 public void setClob(String string, Reader reader)throws SQLException{q();}
		 public void setBlob(String string, InputStream in)throws SQLException{q();}
		 public void setNClob(String string, Reader reader)throws SQLException{q();}
		//1.7
		 public <T>T getObject(String s,Class<T> t)throws SQLFeatureNotSupportedException{throw new SQLFeatureNotSupportedException("nyi");}
		 public <T>T getObject(int parameterIndex,Class<T>t)throws SQLFeatureNotSupportedException{throw new SQLFeatureNotSupportedException("nyi");} 
	}
	
	//1.7
	 public Logger getParentLogger()throws SQLFeatureNotSupportedException{throw new SQLFeatureNotSupportedException("nyi");}



}

