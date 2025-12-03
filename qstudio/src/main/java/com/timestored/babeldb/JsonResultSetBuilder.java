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

import java.lang.reflect.Array;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.sql.rowset.CachedRowSet;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;

import lombok.extern.java.Log;
import net.thisptr.jackson.jq.BuiltinFunctionLoader;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Versions;

@Log
public class JsonResultSetBuilder {

	public static CachedRowSet fromJSON(String json) throws JsonMappingException, JsonProcessingException {
		return fromJSON(json, "");
	}

	public static JsonMapper getObjectMapper() {
		return  JsonMapper.builder()
				.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
				.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
		    	.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
		    	.build();
	}
	
	public static SimpleResultSet fromJSON(String json, String path) throws JsonMappingException, JsonProcessingException {
		ObjectMapper objectMapper =getObjectMapper();
		JsonNode n = objectMapper.readTree(json);
		try {
	    	if(!path.isEmpty()) {
//	    		jsonRead = ((JSONArray) JsonPath.read(json, path)).toJSONString();
	    		ArrayNode an = objectMapper.createArrayNode();
	    		JsonQuery q = JsonQuery.compile(path, Versions.JQ_1_6);
	    		Scope sc = Scope.newEmptyScope();
	    		BuiltinFunctionLoader.getInstance().loadFunctions(Versions.JQ_1_6, sc);
	    		q.apply(sc, n, an::add);
	    		n = an.size() == 1 ? an.get(0) : an;
	    		if(n instanceof NullNode) {
	    			throw new IllegalStateException(json);
	    		}
	    	}

			ArrayNode an = findArrayNode(n);
			if(an != null) {
				return fromJSON(an);
			}
			if(n instanceof ObjectNode) {
				return fromSingleObjectNode((ObjectNode)n);
			} else if(n instanceof ValueNode) {
				// single value - make it an array of one then convert
				an = objectMapper.createArrayNode();
				an.add(n);
				return fromJSON(an);
			}
			// What's left?
		} catch(JsonProcessingException | ClassCastException e) {
			if(n != null) {
				return new SimpleResultSet(new String[] { "jsonError" }, new Object[] { new String[] { json, n.toPrettyString(), e.toString() } });
			} else {
				return new SimpleResultSet(new String[] { "jsonError" }, new Object[] { new String[] { json, e.toString() } });
			}
		}
		return new SimpleResultSet(new String[] { "InvalidJson" }, new Object[] { new String[] { json } });
	}
	
	private static Stream<Entry<String, JsonNode>> toStream(ObjectNode n) {
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(n.fields(), Spliterator.ORDERED), false);
	}
	private static Stream<JsonNode> toStream(ArrayNode n) {
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(n.elements(), Spliterator.ORDERED), false);
	}

	/** No array was found, flip it as a dict **/
	private static SimpleResultSet fromSingleObjectNode(ObjectNode n) {
		List<String> keys = new ArrayList<>();
		List<String> vals = new ArrayList<>();
		toStream(n).forEach(e -> {
			keys.add(e.getKey());
			vals.add((e.getValue().toPrettyString()));
		});
		Object[] cols = new Object[] { keys.toArray(new String[] {}), vals.toArray(new String[] {}) };
		return new SimpleResultSet(new String[] { "keys","vals" }, cols);
		
	}

	private static ArrayNode findArrayNode(JsonNode n) {
		if (n instanceof ArrayNode) {
			return (ArrayNode) n;
		}
		if (n.isObject()) {
			ObjectNode objectNode = (ObjectNode) n;
			Iterator<Map.Entry<String, JsonNode>> iter = objectNode.fields();
			while (iter.hasNext()) {
				Map.Entry<String, JsonNode> entry = iter.next();
				if (entry.getValue().isArray()) {
					return (ArrayNode) entry.getValue();
				}
			}
		}
		return null;
	}

	private static SimpleResultSet fromJSON(ArrayNode arrayNode) throws JsonMappingException, JsonProcessingException {
		int sz = arrayNode.size();
		if(sz == 0) {
			return new SimpleResultSet(new String[] { "empty" });
		}
		if(arrayNode.get(0).isValueNode()) { // [1,2,3]
			return fromArrayOfvalueNodes(arrayNode);
		} else if(arrayNode.get(0).isObject()) { // [{a:1,b:2},{a:1,b:2}]
			return fromArrayOfObjectNodes(arrayNode);
		}  else if(arrayNode.get(0).isArray()) { // [[1,"one"],[2,"two"]
			return fromArrayOfObjectNodes(arrayNode);
		} // array of array nodes?
		return new SimpleResultSet(new String[] { "a" }, new Object[] { new String[] { "v1", "v2" } });
	}

	public static SqlTypeMerger SQL_TYPE_MERGER = new SqlTypeMerger();
	static class SqlTypeMerger implements BiFunction<Integer, Integer, Integer> {
		@Override public Integer apply(Integer a, Integer b) {
			if(a.equals(java.sql.Types.DOUBLE) && b.equals(java.sql.Types.BIGINT)) {
				return java.sql.Types.DOUBLE;
			} else if(a.equals(java.sql.Types.BIGINT) && b.equals(java.sql.Types.DOUBLE)) {
				return java.sql.Types.DOUBLE;
			} else if(a.equals(java.sql.Types.NULL)) {
				return b;
			} else if(b.equals(java.sql.Types.NULL)) {
				return a;
			}
			return a.equals(b) ? a :  java.sql.Types.VARCHAR;
		}
		
		static void replaceNulls(Map<String,Integer> cTypes) {
			// Null was lowest precendence when we were not sure of type. If still not sure use varchar.
			cTypes.forEach((s,i) -> {
				if(i == java.sql.Types.NULL) {
					cTypes.put(s, java.sql.Types.VARCHAR);
				}
			});
		}
	}
	
	private static SimpleResultSet fromArrayOfObjectNodes(ArrayNode arrayNode) {
		int sz = arrayNode.size();
		Set<String> keys = new LinkedHashSet<>(); // linked = important to remember order
		Map<String,Integer> cTypes = new HashMap<>();
		toStream(arrayNode).forEach(jsn -> {
			if(jsn instanceof ObjectNode) {
				toStream(((ObjectNode)jsn)).forEach(e ->{
					String k = e.getKey();
					keys.add(k);
					cTypes.merge(k, toSQLtype(e.getValue()), new SqlTypeMerger());
				});
			} else {
				ArrayNode an = (ArrayNode)jsn;
				for(int i=0; i<an.size(); i++) {
					String k = "c"+i;
					keys.add(k);
					cTypes.merge(k, toSQLtype(an.get(i)), new SqlTypeMerger());
				}
			}
		});
		// Null was lowest precendence when we were not sure of type. If still not sure use varchar.
		SqlTypeMerger.replaceNulls(cTypes);
		
		String[] colNames = keys.toArray(new String[] {});
		List<String> cNames = Arrays.asList(colNames);
		
		Object[] colVals = SimpleResultSet.getArray(cNames.stream().map(cTypes::get).collect(Collectors.toList()), sz);
		int r = 0;
		Iterator<JsonNode> it = arrayNode.iterator();
		while(it.hasNext()) {
			JsonNode jsn = it.next();
			final int row = r;
			if(jsn instanceof ObjectNode) {
				toStream((ObjectNode)jsn).forEach(e -> { 
					int c = cNames.indexOf(e.getKey());
					Object val = getValue(e.getValue(), cTypes.get(e.getKey()));
					try {
						arraySetUnlessNull(colVals[c], row, val);
					} catch(IllegalArgumentException iae) {
						if(val != null && colVals[c] != null) {
							log.fine("Tried to place object of type " + val.getClass().getSimpleName() + " into " + colVals[c].getClass().getSimpleName());
						}
					}
				});
			} else {
				ArrayNode an = (ArrayNode)jsn;
				for(int i=0; i<an.size(); i++) {
					String k = "c"+i;
					int c = cNames.indexOf(k);
					Object val = getValue(an.get(i), cTypes.get(k));
					arraySetUnlessNull(colVals[c], row,  val);
				}
			}
			r++;
		}
		return new SimpleResultSet(colNames, colVals);
	}

	public static void arraySetUnlessNull(Object array, int row, Object val) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
		if(val != null || (array instanceof Object[])) {
			Array.set(array, row,  val);
		}
	}
	
	private static Object getValue(JsonNode on, int sqlType) {
		switch(sqlType) {
			case java.sql.Types.DOUBLE: return on.isNull() ? Double.NaN : on.asDouble();
			case java.sql.Types.INTEGER: return on.isNull() ? null : on.asInt();
			case java.sql.Types.BIGINT: return on.isNull() ? null : on.asLong();
			case java.sql.Types.BOOLEAN: return on.isNull() ? null : on.asBoolean();
			default:
		} 
		if(on.isNull()) {
			return null;
		}
		// Use JSON type info but then use exact same logic as babel CSV
		// Notice we use toString here rather than asTExt() in toSQLtype
		// This is to get the nested data e.g. [1,2] {foo:bar}
		return BabelDBJdbcDriver.getValue(unquote(on.toString()),sqlType);
	}

	public static String unquote(String value) {
		if (value.startsWith("\"") && (value.endsWith("\""))) {
			// Replace all \' slash quote with " quote
			return value.substring(1, value.length() - 1).replaceAll("\\\"", "\"");
		}
		return value;
	}
	
	private static Integer toSQLtype(JsonNode jsonNode) {
		if(jsonNode == null || jsonNode.isNull()) {
			return java.sql.Types.NULL;
		}
		JsonNodeType nodeType = jsonNode.getNodeType();
		switch(nodeType) {
			case NUMBER:
				return jsonNode.asText().contains(".") ? java.sql.Types.DOUBLE : java.sql.Types.BIGINT;
			case BOOLEAN: return java.sql.Types.BOOLEAN;
			case NULL:
			case OBJECT:
			case STRING:
			case POJO:
			case ARRAY:
			case BINARY:
			case MISSING:
			default:
		}// Use JSON type info but then use exact same logic as babel CSV
		return BabelDBJdbcDriver.toSQLtype(jsonNode.asText());
	}

	private static SimpleResultSet fromArrayOfvalueNodes(ArrayNode arrayNode) {
		int sz = arrayNode.size();
		Object valArray = null;
		ValueNode vn = (ValueNode)arrayNode.get(0);
		String t = vn.getNodeType().name(); 
		Iterator<JsonNode> it = arrayNode.iterator();
		int i = 0;
		switch(vn.getNodeType()) {
			case ARRAY:
			case BINARY:
			case BOOLEAN:
			case MISSING:
			case NULL:
			case OBJECT:
			case STRING:
			case POJO:
				String[] v = new String[sz];
				valArray = v;
				while(it.hasNext()) {
					v[i++] = it.next().toString();
				}
				break;
			case NUMBER:
				double[] d = new double[sz];
				valArray = d;
				while(it.hasNext()) {
					d[i++] = it.next().asDouble();
				}
				break;
		}
		return new SimpleResultSet(new String[] { t }, new Object[] { valArray });
	}
}
