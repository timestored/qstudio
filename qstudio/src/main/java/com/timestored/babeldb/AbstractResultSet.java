/*******************************************************************************
 *
 *   $$$$$$$\            $$\                     
 *   $$  __$$\           $$ |                     
 *   $$ |  $$ |$$\   $$\ $$ | $$$$$$$\  $$$$$$\   
 *   $$$$$$$  |$$ |  $$ |$$ |$$  _____|$$  __$$\  
 *   $$  ____/ $$ |  $$ |$$ |\$$$$$$\  $$$$$$$$ |  
 *   $$ |      $$ |  $$ |$$ | \____$$\ $$   ____|  
 *   $$ |      \$$$$$$  |$$ |$$$$$$$  |\$$$$$$$\  
 *   \__|       \______/ \__|\_______/  \_______|
 *
 *  Copyright c 2022-2023 TimeStored
 *
 *  Licensed under the Reciprocal Public License RPL-1.5
 *  You may obtain a copy of the License at
 *
 *  https://opensource.org/license/rpl-1-5/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/
 
package com.timestored.babeldb;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

/**
 * Converts all calls using labelled columns to index calls, so that only
 * findColumn need be implemented. Converts all native getInt/Double/XXX to castings from
 * getObject, so only getObjects needs implemented.
 */
abstract class AbstractResultSet implements ResultSet {

	@Override
	public InputStream getAsciiStream(String columnLabel) throws SQLException {
		return getAsciiStream(findColumn(columnLabel));
	}

	@Override
	public Array getArray(String columnLabel) throws SQLException {
		return getArray(findColumn(columnLabel));
	}

	@Override
	public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
		return getBigDecimal(findColumn(columnLabel));
	}

	@Override
	public BigDecimal getBigDecimal(String columnLabel, int scale)
			throws SQLException {
		return getBigDecimal(findColumn(columnLabel));
	}

	@Override
	public InputStream getBinaryStream(String columnLabel) throws SQLException {
		return getBinaryStream(findColumn(columnLabel));
	}

	@Override
	public Blob getBlob(String columnLabel) throws SQLException {
		return getBlob(findColumn(columnLabel));
	}

	@Override
	public byte[] getBytes(String columnLabel) throws SQLException {
		return getBytes(findColumn(columnLabel));
	}

	@Override
	public Reader getCharacterStream(String columnLabel) throws SQLException {
		return getCharacterStream(findColumn(columnLabel));
	}

	@Override
	public Clob getClob(String columnLabel) throws SQLException {
		return getClob(findColumn(columnLabel));
	}

	@Override
	public Date getDate(String columnLabel) throws SQLException {
		return getDate(findColumn(columnLabel));
	}

	@Override
	public Date getDate(String columnLabel, Calendar cal) throws SQLException {
		return getDate(findColumn(columnLabel));
	}

	@Override
	public double getDouble(String columnLabel) throws SQLException {
		return getDouble(findColumn(columnLabel));
	}

	@Override
	public float getFloat(String columnLabel) throws SQLException {
		return getFloat(findColumn(columnLabel));
	}

	@Override
	public int getInt(String columnLabel) throws SQLException {
		return getInt(findColumn(columnLabel));
	}

	@Override
	public Reader getNCharacterStream(String columnLabel) throws SQLException {
		return getNCharacterStream(findColumn(columnLabel));
	}

	@Override
	public NClob getNClob(String columnLabel) throws SQLException {
		return getNClob(findColumn(columnLabel));
	}

	@Override
	public String getNString(String columnLabel) throws SQLException {
		return getNString(findColumn(columnLabel));
	}

	@Override
	public Object getObject(String columnLabel) throws SQLException {
		return getObject(findColumn(columnLabel));
	}

	@Override
	public Object getObject(String columnLabel, Map<String, Class<?>> map)
			throws SQLException {
		return getObject(findColumn(columnLabel));
	}

	@Override
	public Ref getRef(String columnLabel) throws SQLException {
		return getRef(findColumn(columnLabel));
	}

	@Override
	public RowId getRowId(String columnLabel) throws SQLException {
		return getRowId(findColumn(columnLabel));
	}

	@Override
	public SQLXML getSQLXML(String columnLabel) throws SQLException {
		return getSQLXML(findColumn(columnLabel));
	}

	@Override
	public short getShort(String columnLabel) throws SQLException {
		return getShort(findColumn(columnLabel));
	}

	@Override
	public String getString(String columnLabel) throws SQLException {
		return getString(findColumn(columnLabel));
	}

	@Override
	public Time getTime(String columnLabel) throws SQLException {
		return getTime(findColumn(columnLabel));
	}

	@Override
	public Time getTime(String columnLabel, Calendar cal) throws SQLException {
		return getTime(findColumn(columnLabel));
	}

	@Override
	public Timestamp getTimestamp(String columnLabel) throws SQLException {
		return getTimestamp(findColumn(columnLabel));
	}

	@Override
	public Timestamp getTimestamp(String columnLabel, Calendar cal)
			throws SQLException {
		return getTimestamp(findColumn(columnLabel));
	}

	@Override
	public URL getURL(String columnLabel) throws SQLException {
		return getURL(findColumn(columnLabel));
	}

	@Override
	public InputStream getUnicodeStream(String columnLabel) throws SQLException {
		return getUnicodeStream(findColumn(columnLabel));
	}

	@Override
	public void updateArray(String columnLabel, Array x) throws SQLException {
		updateArray(findColumn(columnLabel), x);

	}

	@Override
	public void updateAsciiStream(String columnLabel, InputStream x)
			throws SQLException {
		updateAsciiStream(findColumn(columnLabel), x);

	}

	@Override
	public void updateAsciiStream(String columnLabel, InputStream x, int length)
			throws SQLException {
		updateAsciiStream(findColumn(columnLabel), x, length);

	}

	@Override
	public void updateAsciiStream(String columnLabel, InputStream x, long length)
			throws SQLException {
		updateAsciiStream(findColumn(columnLabel), x, length);

	}

	@Override
	public void updateBinaryStream(String columnLabel, InputStream x)
			throws SQLException {
		updateBinaryStream(findColumn(columnLabel), x);

	}

	@Override
	public void updateBinaryStream(String columnLabel, InputStream x, int length)
			throws SQLException {
		updateBinaryStream(findColumn(columnLabel), x, length);

	}

	@Override
	public void updateBinaryStream(String columnLabel, InputStream x,
			long length) throws SQLException {
		updateBinaryStream(findColumn(columnLabel), x, length);

	}

	@Override
	public void updateBlob(String columnLabel, Blob x) throws SQLException {
		updateBlob(findColumn(columnLabel), x);

	}

	@Override
	public void updateBlob(String columnLabel, InputStream inputStream,
			long length) throws SQLException {
		updateBlob(findColumn(columnLabel), inputStream, length);

	}

	@Override
	public void updateBlob(String columnLabel, InputStream inputStream)
			throws SQLException {
		updateBlob(findColumn(columnLabel), inputStream);

	}

	@Override
	public void updateBoolean(String columnLabel, boolean x)
			throws SQLException {
		updateBoolean(findColumn(columnLabel), x);

	}

	@Override
	public void updateByte(String columnLabel, byte x) throws SQLException {
		updateByte(findColumn(columnLabel), x);

	}

	@Override
	public void updateBytes(String columnLabel, byte[] x) throws SQLException {
		updateBytes(findColumn(columnLabel), x);

	}

	@Override
	public void updateCharacterStream(String columnLabel, Reader reader)
			throws SQLException {
		updateCharacterStream(findColumn(columnLabel), reader);

	}

	@Override
	public void updateCharacterStream(String columnLabel, Reader reader,
			int length) throws SQLException {
		updateCharacterStream(findColumn(columnLabel), reader, length);

	}

	@Override
	public void updateClob(String columnLabel, Clob x) throws SQLException {
		updateClob(findColumn(columnLabel), x);

	}

	@Override
	public void updateClob(String columnLabel, Reader reader)
			throws SQLException {
		updateClob(findColumn(columnLabel), reader);

	}

	@Override
	public void updateClob(String columnLabel, Reader reader, long length)
			throws SQLException {
		updateClob(findColumn(columnLabel), reader, length);

	}

	@Override
	public void updateDate(String columnLabel, Date x) throws SQLException {
		updateDate(findColumn(columnLabel), x);

	}

	@Override
	public void updateDouble(String columnLabel, double x) throws SQLException {
		updateDouble(findColumn(columnLabel), x);

	}

	@Override
	public void updateFloat(String columnLabel, float x) throws SQLException {
		updateFloat(findColumn(columnLabel), x);

	}

	@Override
	public void updateInt(String columnLabel, int x) throws SQLException {
		updateInt(findColumn(columnLabel), x);

	}

	@Override
	public void updateLong(String columnLabel, long x) throws SQLException {
		updateLong(findColumn(columnLabel), x);

	}

	@Override
	public void updateNCharacterStream(String columnLabel, Reader reader)
			throws SQLException {
		updateNCharacterStream(findColumn(columnLabel), reader);
	}

	@Override
	public void updateNCharacterStream(String columnLabel, Reader reader,
			long length) throws SQLException {
		updateNCharacterStream(findColumn(columnLabel), reader, length);

	}

	@Override
	public void updateNClob(String columnLabel, NClob nClob)
			throws SQLException {
		updateNClob(findColumn(columnLabel), nClob);

	}

	@Override
	public void updateNClob(String columnLabel, Reader reader)
			throws SQLException {
		updateNClob(findColumn(columnLabel), reader);

	}

	@Override
	public void updateNClob(String columnLabel, Reader reader, long length)
			throws SQLException {
		updateNClob(findColumn(columnLabel), reader, length);

	}

	@Override
	public void updateNString(String columnLabel, String nString)
			throws SQLException {
		updateNString(findColumn(columnLabel), nString);

	}

	@Override
	public void updateNull(String columnLabel) throws SQLException {
		updateNull(findColumn(columnLabel));

	}

	@Override
	public void updateObject(String columnLabel, Object x) throws SQLException {
		updateObject(findColumn(columnLabel), x);

	}

	@Override
	public void updateObject(String columnLabel, Object x, int scaleOrLength)
			throws SQLException {
		updateObject(findColumn(columnLabel), x);

	}

	@Override
	public void updateRef(String columnLabel, Ref x) throws SQLException {
		updateRef(findColumn(columnLabel), x);

	}

	@Override
	public byte getByte(String columnLabel) throws SQLException {
		return getByte(findColumn(columnLabel));
	}

	@Override
	public void updateRowId(String columnLabel, RowId x) throws SQLException {
		updateRowId(findColumn(columnLabel), x);

	}

	@Override
	public void updateCharacterStream(String columnLabel, Reader reader,
			long length) throws SQLException {
		updateCharacterStream(findColumn(columnLabel), reader, length);

	}

	@Override
	public long getLong(String columnLabel) throws SQLException {
		return getLong(findColumn(columnLabel));
	}

	@Override
	public void updateBigDecimal(String columnLabel, BigDecimal x)
			throws SQLException {
		updateBigDecimal(findColumn(columnLabel), x);

	}

	@Override
	public void updateSQLXML(String columnLabel, SQLXML xmlObject)
			throws SQLException {
		updateSQLXML(findColumn(columnLabel), xmlObject);

	}

	@Override
	public void updateShort(String columnLabel, short x) throws SQLException {
		updateShort(findColumn(columnLabel), x);

	}

	@Override
	public void updateString(String columnLabel, String x) throws SQLException {
		updateString(findColumn(columnLabel), x);
	}

	@Override
	public void updateTime(String columnLabel, Time x) throws SQLException {
		updateTime(findColumn(columnLabel), x);

	}

	@Override
	public boolean getBoolean(String columnLabel) throws SQLException {
		return getBoolean(findColumn(columnLabel));

	}

	@Override
	public void updateTimestamp(String columnLabel, Timestamp x)
			throws SQLException {
		updateTimestamp(findColumn(columnLabel), x);

	}
	
	/**********************************************************************
	 * BELOW ALL USE THE getObject call
	 ***********************************************************************/
	

	@Override
	public java.sql.Array getArray(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public InputStream getAsciiStream(int columnIndex) throws SQLException {
		return (InputStream) getObject(columnIndex);
	}

	
	@Override
	public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
		return (BigDecimal) getObject(columnIndex);
	}


	@Override
	public BigDecimal getBigDecimal(int columnIndex, int scale)
			throws SQLException {
		return (BigDecimal) getObject(columnIndex);
	}

	

	@Override
	public InputStream getBinaryStream(int columnIndex) throws SQLException {
		return (InputStream) getObject(columnIndex);
	}
	
	@Override
	public Blob getBlob(int columnIndex) throws SQLException {
		return (Blob) getObject(columnIndex);
	}

	@Override
	public boolean getBoolean(int columnIndex) throws SQLException {
		return (boolean) (Boolean) getObject(columnIndex);
	}

	@Override
	public byte getByte(int columnIndex) throws SQLException {
		return (byte) (Byte) getObject(columnIndex);
	}

	@Override
	public byte[] getBytes(int columnIndex) throws SQLException {
		return (byte[]) getObject(columnIndex);
	}

	@Override
	public Reader getCharacterStream(int columnIndex) throws SQLException {
		return (Reader) getObject(columnIndex);
	}


	@Override
	public Clob getClob(int columnIndex) throws SQLException {
		return (Clob) getObject(columnIndex);
	}

	

	@Override
	public Reader getNCharacterStream(int columnIndex) throws SQLException {
		return (Reader) getObject(columnIndex);
	}

	@Override
	public NClob getNClob(int columnIndex) throws SQLException {
		return (NClob) getObject(columnIndex);
	}



	@Override
	public String getNString(int columnIndex) throws SQLException {
		return (String) getObject(columnIndex);
	}
	

	@Override
	public URL getURL(int columnIndex) throws SQLException {
		return (URL) getObject(columnIndex);
	}


	@Override
	public InputStream getUnicodeStream(int columnIndex) throws SQLException {
		return (InputStream) getObject(columnIndex);
	}


	
	

	@Override
	public short getShort(int columnIndex) throws SQLException {
		return (short) (Short) getObject(columnIndex);
	}


	@Override
	public String getString(int columnIndex) throws SQLException {
		return (String) getObject(columnIndex);
	}

	@Override
	public Time getTime(int columnIndex) throws SQLException {
		return (Time) getObject(columnIndex);
	}


	@Override
	public Time getTime(int columnIndex, Calendar cal) throws SQLException {
		return (Time) getObject(columnIndex);
	}
	
	@Override
	public Timestamp getTimestamp(int columnIndex) throws SQLException {
		return (Timestamp) getObject(columnIndex);
	}

	@Override
	public Timestamp getTimestamp(int columnIndex, Calendar cal)
			throws SQLException {
		return (Timestamp) getObject(columnIndex);
	}
	

	@Override
	public float getFloat(int columnIndex) throws SQLException {
		Object o = getObject(columnIndex); return o == null ? 0 :  (float) (Float)  o;
	}

	

	@Override
	public Date getDate(int columnIndex) throws SQLException {
		return (Date) getObject(columnIndex);
	}


	@Override
	public Date getDate(int columnIndex, Calendar cal) throws SQLException {
		return (Date) getObject(columnIndex);
	}

	@Override public double getDouble(int columnIndex) throws SQLException {
		Object o = getObject(columnIndex); return o == null ? 0.0 : (double) (Double) o;
	}

	@Override
	public int getInt(int columnIndex) throws SQLException {
		Object o = getObject(columnIndex); return o == null ? 0 : (int) (Integer) o;
	}

	@Override
	public long getLong(int columnIndex) throws SQLException {
		Object o = getObject(columnIndex); return o == null ? 0 : (long) (Long)  o;
	}

	public <T> T getObject(int columnIndex, Class<T> arg1) throws SQLException {
		return (T) getObject(columnIndex);
	}
	
}
