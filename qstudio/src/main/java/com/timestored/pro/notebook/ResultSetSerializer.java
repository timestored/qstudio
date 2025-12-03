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
 
package com.timestored.pro.notebook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import javax.sql.rowset.serial.SerialArray;
import javax.sql.rowset.serial.SerialException;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import kx.c.KException;
import com.timestored.kdb.QueryResultI;
import com.timestored.qstudio.kdb.KdbHelper;

import kx.c;
import kx.c.Dict;
import static kx.c.n;
import static kx.c.at;

/**
 *  GOLDEN COPY IS IN PULSE.       GOLDEN COPY IS IN PULSE.       
 *  GOLDEN COPY IS IN PULSE.       GOLDEN COPY IS IN PULSE.       
 *  GOLDEN COPY IS IN PULSE.       GOLDEN COPY IS IN PULSE.       
 *  GOLDEN COPY IS IN PULSE.       GOLDEN COPY IS IN PULSE.       
 *  GOLDEN COPY IS IN PULSE.       GOLDEN COPY IS IN PULSE.       
 */

public class ResultSetSerializer extends JsonSerializer<ResultSet> {
	
	private static final Logger LOG = Logger.getLogger(ResultSetSerializer.class.getName());
	private final boolean extendedFormatWithTypes;
	private final boolean convertUndersoresToCamel;
	
	public ResultSetSerializer(boolean sanitizeHtml) { this(true, false, sanitizeHtml); }
	
	public ResultSetSerializer(boolean extendedFormatWithTypes, boolean convertUndersoresToCamel, boolean sanitizeHtml) { 
		this.extendedFormatWithTypes = extendedFormatWithTypes;
		this.convertUndersoresToCamel = convertUndersoresToCamel;
	}

    public static class ResultSetSerializerException extends JsonProcessingException{
        private static final long serialVersionUID = -914957626413580734L;

        public ResultSetSerializerException(Throwable cause){
            super(cause);
        }
    }

    @Override
    public Class<ResultSet> handledType() {
        return ResultSet.class;
    }

    
    
    public String toString(QueryResultI qr) throws IOException, JsonProcessingException {
    	ByteArrayOutputStream baos = new ByteArrayOutputStream(128);
    	JsonGenerator jgen = new JsonFactory().createGenerator(baos);
    	SimpleModule module = new SimpleModule();
    	module.addSerializer(Dict.class, new DictSerializer());
    	module.addSerializer(UUID.class, new UUIDSerializer());
    	ObjectMapper mapper = new ObjectMapper();
    	mapper.registerModule(module);
    	jgen.setCodec(mapper);
    	
        jgen.writeStartObject();
        if(qr.getRs() != null) {
            jgen.writeFieldName("tbl");
        	serialize(qr.getRs(), jgen, null);
        }
        
        if(qr.getConsoleView() != null) {
	        jgen.writeFieldName("console");
	        ws(jgen, qr.getConsoleView());
        }
        if(qr.getE() != null) {
            jgen.writeFieldName("exception");
            if(qr.getE() instanceof KException) {
            	KException ke = (KException) qr.getE();
            	ws(jgen, ke.getLocalizedMessage());
            } else {
            	ws(jgen, qr.getE().getLocalizedMessage());
            }
        }

        if(qr.isExceededMax()) {
	        jgen.writeFieldName("exceededMaxRows");
	        jgen.writeBoolean(qr.isExceededMax());
        }
        
        // TODO convert K to json
        if(qr.getK() != null && qr.getRs() == null) {
            jgen.writeFieldName("k");
            String ks = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(qr.getK());
            jgen.writeRawValue(ks);
        }        
        
        jgen.writeEndObject();
        jgen.close();
        return baos.toString("UTF-8");
    }

    class UUIDSerializer extends StdSerializer<UUID> {
        
        public UUIDSerializer() { this(null); }
        public UUIDSerializer(Class<UUID> t) { super(t); }

        @Override public void serialize(UUID uuid, JsonGenerator jgen, SerializerProvider provider) 
          throws IOException, JsonProcessingException {
            jgen.writeString(uuid.toString());
        }
    }
    
    class DictSerializer extends StdSerializer<Dict> {
        
        public DictSerializer() { this(null); }
        public DictSerializer(Class<Dict> t) { super(t); }

        @Override public void serialize(Dict dict, JsonGenerator jgen, SerializerProvider provider) 
          throws IOException, JsonProcessingException {
            jgen.writeStartObject();
            for(int i = 0; i<n(dict); i++) {
                jgen.writeFieldName(""+at(dict.x,i));
                provider.defaultSerializeValue(at(dict.y,i), jgen);
            }
            jgen.writeEndObject();
        }
    }

    public String toStringSingleRowOnly(ResultSet rs) throws IOException, JsonProcessingException {
    	ByteArrayOutputStream baos = new ByteArrayOutputStream(128);
    	JsonGenerator jgen = new JsonFactory().createGenerator(baos);
        serialize(rs, jgen, null, true);
        jgen.close();
        return baos.toString("UTF-8");
    }
    
    public String toString(ResultSet rs) throws IOException, JsonProcessingException {
    	ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
    	JsonGenerator jgen = new JsonFactory().createGenerator(baos);
        serialize(rs, jgen, null);
        jgen.close();
        return baos.toString("UTF-8");
    }
    
    public String toString(ResultSet rs, boolean exceededMaxRows) throws IOException, JsonProcessingException {
    	ByteArrayOutputStream baos = new ByteArrayOutputStream(128);
    	JsonGenerator jgen = new JsonFactory().createGenerator(baos);
        jgen.writeStartObject();
        if(exceededMaxRows) {
	        jgen.writeFieldName("exceededMaxRows");
	        jgen.writeBoolean(exceededMaxRows);
        }
        jgen.writeFieldName("tbl");
        serialize(rs, jgen, null);
        jgen.writeEndObject();
        jgen.close();

        return baos.toString("UTF-8");
    }

    private static long toEpochSecond(LocalTime t,LocalDate d,ZoneOffset o){
      long epochDay=d.toEpochDay();
      long secs=epochDay*86400+t.toSecondOfDay();
      secs-=o.getTotalSeconds();
      return secs;
    }
    public static final LocalTime LOCAL_TIME_NULL = LocalTime.ofNanoOfDay(1);
    static final long MILLS_IN_DAY = 86400000L;
    /**
     * Write LocalTime to serialization buffer in big endian format
     * @param t Time to serialize
     */
    public static long toEpochSecond(LocalTime t){
       return (t==LOCAL_TIME_NULL)?Integer.MIN_VALUE:(int)((toEpochSecond(t,LocalDate.of(1970,1,1),ZoneOffset.ofTotalSeconds(0))*1000+t.getNano()/1000000)%MILLS_IN_DAY);
    }


    @Override public void serialize(ResultSet rs, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
    	serialize(rs, jgen, provider, false);
    }
    
    private void ws(JsonGenerator jgen, String s) throws IOException {
    	jgen.writeString(s);
    }
    
    private void serialize(ResultSet rs, JsonGenerator jgen, SerializerProvider provider, boolean singleRowOnly) throws IOException, JsonProcessingException {

        try {
            ResultSetMetaData rsmd = rs.getMetaData();
            int numColumns = rsmd.getColumnCount();
            String[] columnNames = new String[numColumns];
            int[] columnTypes = new int[numColumns];

            for (int i = 0; i < columnNames.length; i++) {
                columnNames[i] = rsmd.getColumnLabel(i + 1);
                columnTypes[i] = rsmd.getColumnType(i + 1);
            }
            if(extendedFormatWithTypes) {
	            jgen.writeStartObject();
	            jgen.writeFieldName("data");
            }

    		rs.beforeFirst();
    		String[] niceNames = this.convertUndersoresToCamel ? convertToCamel(columnNames) : columnNames;
    		
            Map<String, String> colNamesToJsTypes = serialiseRSarray(rs, jgen, provider, columnNames, niceNames, columnTypes, singleRowOnly);

            if(extendedFormatWithTypes) {
	            jgen.writeFieldName("types");
	            jgen.writeStartObject();
	
	            for(int c = 0; c < columnNames.length; c++) {
					jgen.writeFieldName(niceNames[c]);
					String typ = colNamesToJsTypes.get(columnNames[c]);
					if(typ != null) {
	    				ws(jgen, typ);
					} else {
			             switch (columnTypes[c]) {
			                case Types.INTEGER:
			                case Types.NUMERIC:
			                case Types.DECIMAL:
			                case Types.FLOAT:
			                case Types.REAL:
			                case Types.DOUBLE:
			                case Types.BIGINT:
			    				ws(jgen, "number");
			    				break;
			                case Types.JAVA_OBJECT:
			            		// https://github.com/duckdb/duckdb/issues/9585  
			            		// DuckDB returns type=2000 generic java object for all pivots. Need this to force conversion.
			                	//boolean isDuckDBNum = "HUGEINT".equals(columnTypeNames[c]);
			                	ws(jgen, rs.getObject(c+1) instanceof Number ? "number" : "");
			                	break;
		                    case Types.NVARCHAR:
		                    case Types.VARCHAR:
		                    case Types.LONGNVARCHAR:
		                    case Types.LONGVARCHAR:
			    				ws(jgen, "string");
			    				break;
							default:
			    				ws(jgen, "");
			    				break;
			                }	
					}
	            }
	
	            jgen.writeEndObject();
	            jgen.writeEndObject();
            }
        } catch (SQLException e) {
            throw new ResultSetSerializerException(e);
        }
    }

    private static String toProperCase(String s) { return s == null ? null : s.length() > 1 ? s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase() : s.toUpperCase(); }
    
	static String[] convertToCamel(String[] columnNames) {
		String[] r = new String[columnNames.length];
		for(int i=0; i<r.length; i++) {
			r[i] = columnNames[i].toLowerCase();
			if(r[i] != null && r[i].contains("_")) {
			   String[] parts = r[i].split("_");
	    	   String camelCaseString = parts.length > 0 ? parts[0] : "";
	    	   for(int j=1; j<parts.length; j++) {
		    	      camelCaseString = camelCaseString + toProperCase(parts[j]);
	    	   }
	    	   r[i] = camelCaseString;
			}
		}
		return r;
	}
	private Map<String, String> serialiseRSarray(ResultSet rs, JsonGenerator jgen, SerializerProvider provider,
			String[] columnNames, String[] niceNames, int[] columnTypes, boolean singleRowOnly) throws IOException, SQLException {
		if(!singleRowOnly) {
			jgen.writeStartArray();
		}
		Map<String,String> colNamesToJsTypes = new HashMap<>(3);
		int row = 0;
		
		while (rs.next()) {
			row++;
		    jgen.writeStartObject();

		    for (int i = 0; i < columnNames.length; i++) {

		        jgen.writeFieldName(niceNames[i]);
		        try {
		        	writeOneCol(rs, jgen, provider, columnNames, columnTypes, colNamesToJsTypes, i);
		        } catch(SQLException e) {
		        	// error throw on one column when it can't read that one.
		        	// e.g. H2 "SELECT DASHBAORD" was failing as it had a CLOB but couldn't read it.
			        jgen.writeNull();
		        }
		    }

		    jgen.writeEndObject();
		}
		if(singleRowOnly && row != 1) {
			throw new IllegalStateException("MUST be only one row");
		}
		if(!singleRowOnly) {
			jgen.writeEndArray();
		}
		return colNamesToJsTypes;
	}
	
	private void writeOneCol(ResultSet rs, JsonGenerator jgen, SerializerProvider provider, String[] columnNames,
			int[] columnTypes, Map<String, String> colNamesToJsTypes, int i)
			throws SQLException, IOException, SerialException {
		boolean b;
		long l;
		double d;
		switch (columnTypes[i]) {

		case Types.INTEGER:
		    l = rs.getInt(i + 1);
		    if (rs.wasNull()) {
		        jgen.writeNull();
		    } else {
		        jgen.writeNumber(l);
		    }
		    break;

		case Types.BIGINT:
		    l = rs.getLong(i + 1);
		    if (rs.wasNull()) {
		        jgen.writeNull();
		    } else {
		        jgen.writeNumber(l);
		    }
		    break;

		case Types.DECIMAL:
		case Types.NUMERIC:
		    jgen.writeNumber(rs.getBigDecimal(i + 1));
		    break;

		case Types.FLOAT:
		case Types.REAL:
		case Types.DOUBLE:
		    d = rs.getDouble(i + 1);
		    if (rs.wasNull()) {
		        jgen.writeNull();
		    } else {
		        jgen.writeNumber(d);
		    }
		    break;
		    
		case Types.NVARCHAR:
		case Types.VARCHAR:
		case Types.LONGNVARCHAR:
		case Types.LONGVARCHAR:
		    ws(jgen, rs.getString(i + 1));
		    break;

		case Types.BOOLEAN:
		case Types.BIT:
		    b = rs.getBoolean(i + 1);
		    if (rs.wasNull()) {
		        jgen.writeNull();
		    } else {
		        jgen.writeBoolean(b);
		    }
		    break;

		case Types.BINARY:
		case Types.VARBINARY:
		case Types.LONGVARBINARY:
			jgen.writeBinary(rs.getBytes(i + 1));
		    break;

		case Types.TINYINT:
		case Types.SMALLINT:
		    l = rs.getShort(i + 1);
		    if (rs.wasNull()) {
		        jgen.writeNull();
		    } else {
		        jgen.writeNumber(l);
		    }
		    break;

		case Types.DATE:
		case Types.TIMESTAMP:
		case Types.TIME:     
		case Types.TIME_WITH_TIMEZONE:     
		case Types.TIMESTAMP_WITH_TIMEZONE:     
			Object o = rs.getObject(i+1);
		    if (rs.wasNull()) {
		        jgen.writeNull();
		    } else {
				long epoch = toEpochMillis(o);
				jgen.writeNumber(epoch);
		    }
			int ct = columnTypes[i];
			String typ = ct == Types.TIME || ct == Types.TIME_WITH_TIMEZONE ? "Time" : 
					(ct == Types.DATE ? "DateOnly" : "Date");
			colNamesToJsTypes.putIfAbsent(columnNames[i], typ);
		    break;

		case Types.BLOB:
		    Blob blob = rs.getBlob(i);
		    provider.defaultSerializeValue(blob.getBinaryStream(), jgen);
		    blob.free();
		    break;

		case Types.CLOB:
		    Clob clob = rs.getClob(i);
		    provider.defaultSerializeValue(clob.getCharacterStream(), jgen);
		    clob.free();
		    break;

		case Types.ARRAY:
			Object oo = rs.getObject(i+1); // notice this is getObject. getArray doesn't work for H2
			if(oo instanceof SerialArray) {
				SerialArray sa = (SerialArray)oo;
				oo = sa.getArray();
			}
			boolean isNumArray = false;
			try {
				isNumArray = writeArray(jgen, oo);
			} catch(IOException ioe) {
	    		LOG.warning("Unrecognised " + columnNames[i] + " of type " + columnTypes[i] + " with value: " + oo + " " + ioe);
				jgen.writeNull(); // MUST write something to attach to field name.
			}
			if(isNumArray) {
				colNamesToJsTypes.putIfAbsent(columnNames[i], "numarray");
			} else { 
				colNamesToJsTypes.putIfAbsent(columnNames[i], "string");
			}
		    break;

		case Types.STRUCT:
		    throw new RuntimeException("ResultSetSerializer not yet implemented for SQL type STRUCT");

		case Types.DISTINCT:
		    throw new RuntimeException("ResultSetSerializer not yet implemented for SQL type DISTINCT");

		case Types.REF:
		    throw new RuntimeException("ResultSetSerializer not yet implemented for SQL type REF");

		case Types.JAVA_OBJECT:
		default:
			Object obj = rs.getObject(i + 1);

			// https://github.com/duckdb/duckdb/issues/9585  
			// DuckDB returns type=2000 generic java object for all pivots. Need this to force conversion.
			if(obj instanceof Number) {
	        	colNamesToJsTypes.putIfAbsent(columnNames[i], "number");
			    d = rs.getDouble(i + 1);
			    if (rs.wasNull()) {
			        jgen.writeNull();
			    } else {
			        jgen.writeNumber(d);
			    }
			    break;
			} else if(obj instanceof Timestamp) {
				// TODO losing accuracy here
		    	jgen.writeNumber(((Timestamp)  obj).getTime());
		    	colNamesToJsTypes.putIfAbsent(columnNames[i], "Date");
			} else {
				// DuckDB sends type=2000 for OffsetDateTime / OffsetTime
				long epoch2 = toEpochMillis(obj);
				if(epoch2 != Long.MIN_VALUE) {
					jgen.writeNumber(epoch2);
					String typ2 = obj instanceof OffsetTime ? "Time" : "Date";
					colNamesToJsTypes.putIfAbsent(columnNames[i], typ2);
				} else {
			    	try {
			        	colNamesToJsTypes.putIfAbsent(columnNames[i], "string");
			        	if(provider != null) {
			        		provider.defaultSerializeValue(obj, jgen);
			        	} else {
			            	ws(jgen, obj == null ? "" : obj.toString());
			        	}
			    	} catch(IOException ioe) {
			    		LOG.warning("Unrecognised " + columnNames[i] + " of type " + columnTypes[i] + " with value: " + obj);
			    	}
				}
			}
		    break;
		}
	}

	/**
	 * @throws IOException when it can't find a good type to write for this array. You MUST catch this and write a field to make it work.
	 */
	private boolean writeArray(JsonGenerator jgen, Object oo)
			throws IOException {
		Object o;
		boolean isNumArray = false;
		// Hand convert some, as spark lines require proper numbers.
		// But fall back to kdb render to handle complex types.
		if(oo.getClass().isArray()) {
			// Object[] required for H2 as it returns Object[] { Integer, Integer}
			boolean isH2array = false;
			if((oo instanceof Object[]) && ((Object[])oo).length > 0) {
				Object obj = ((Object[])oo)[0];
				isH2array = obj instanceof Integer || obj instanceof Long || obj instanceof Float || obj instanceof Double;
			}
			if(isH2array || oo instanceof int[] || oo instanceof long[] || oo instanceof double[] || oo instanceof float[]
					|| oo instanceof Integer[] || oo instanceof Long[] || oo instanceof Double[] || oo instanceof Float[]) { 
				isNumArray = true;
				jgen.writeStartArray();
				int n = Array.getLength(oo);
				for(int mi=0;mi<n;mi++) {
					try {
						o = Array.get(oo, mi);
						if(c.qn(o)) { // Using kdb specific null check
							jgen.writeNull(); 
						} else {
							jgen.writeObject(o);	
						}
						
					} catch(IllegalStateException ioe) {
						throw new IOException("2Unrecognised column type value");
					}
				}
				jgen.writeEndArray();
			} else {
				String s = KdbHelper.asText(oo);
				if(s != null && !s.equals("") && !s.equals("::")) {
					jgen.writeString(s);
				} else {
					throw new IOException("3Unrecognised column type value");
				}
			}
		} else if(oo instanceof String) {
			jgen.writeString((String)oo);
		} else {
			throw new IOException("2Unrecognised column type value");
		}
		return isNumArray;
	}
	
	private static long toEpochMillis(Object o) {
		long epoch = Long.MIN_VALUE;
		if(o instanceof java.sql.Date) {
			epoch = ((java.sql.Date)o).getTime();
		} else if(o instanceof java.util.Date) {
			epoch = ((java.util.Date)o).getTime();
		} else if(o instanceof LocalDate) {
			epoch = ((LocalDate) o).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli();
		} else if(o instanceof LocalTime) {
			epoch = toEpochSecond((LocalTime) o);
		} else if(o instanceof LocalDateTime) {
			epoch = ((LocalDateTime)o).toInstant(ZoneOffset.UTC).toEpochMilli();
		} else if(o instanceof Instant) {
			epoch = ((Instant)o).toEpochMilli();
		// These offset types are returned by DuckDB but the sql type is Object=2000 
		} else if(o instanceof OffsetTime) {
			epoch = ((OffsetTime)o).getLong(ChronoField.MILLI_OF_DAY);
		} else if(o instanceof OffsetDateTime) {
			epoch = ((OffsetDateTime)o).toInstant().toEpochMilli();
		}
		return epoch;
	}
}