
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
 *  Commercially licensed only. 
 *  contact licensing@timestored.com 
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

package com.timestored.babeldb;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Joiner;
import com.timestored.connections.JdbcTypes;

public class PivotProvider {

	public static final String KDB_PULSEAGG = "{[tbl; groupby; pivot; sel]\r\n"
			+ "    \r\n"
			+ "    piv:{[t;k;p;v]\r\n"
			+ "        / controls new columns names\r\n"
			+ "        f:{[v;P]`${-1 _ raze \"_\" sv x} each string raze P[;0],'/:v,/:\\:P[;1]};\r\n"
			+ "         v:(),v; k:(),k; p:(),p; / make sure args are lists\r\n"
			+ "         G:group flip k!(t:.Q.v t)k;\r\n"
			+ "         F:group flip p!t p;\r\n"
			+ "         sortAlpha:{(c iasc `$string c:cols x) xcols x};\r\n" // "         sortAlpha:{(c iasc `$reverse each string c:cols x) xcols x};\r\n"
			+ "         key[G]!sortAlpha flip(C:f[v]P:flip value flip key F)!raze\r\n"
			+ "          {[i;j;k;x;y]\r\n"
			+ "           a:count[x]#x 0N;\r\n"
			+ "           a[y]:x y;\r\n"
			+ "           b:count[x]#0b;\r\n"
			+ "           b[y]:1b;\r\n"
			+ "           c:a i;\r\n"
			+ "           c[k]:first'[a[j]@'where'[b j]];\r\n"
			+ "           c}[I[;0];I J;J:where 1<>count'[I:value G]]/:\\:[t v;value F]};\r\n"
			+ "\r\n"
			+ "    g:groupby,pivot;\r\n"
			+ "    if[(0<count pivot) and 0=count groupby;'mustSpecifyGroupbyToAllowPivot];\r\n"
			+ "    s:@[;4] parse \"select \",sel,\" from t\";\r\n"
			+ "    t:$[0=count g;select from tbl where i<1000; ?[tbl;();g!g;s]];\r\n"
			+ "    () xkey $[count pivot; piv[() xkey t;groupby;pivot;$[count s;key s;cols[t] except g]]; t]}";
	

	public static ResultSet postProcess(JdbcTypes jdbcTypes, ResultSet rs, List<String> byCols, List<String> pivotCols) throws SQLException {
		boolean pivotAlready = jdbcTypes.equals(JdbcTypes.KDB) || jdbcTypes.equals(JdbcTypes.DUCKDB);
		if(pivotAlready || pivotCols.isEmpty()) {
			return rs;
		}
		return new PivotResultSet(rs, byCols, pivotCols);
	}
	

	public static String pivotSQL(JdbcTypes jdbcTypes, List<String> groupbylist, List<String> pivotlist, String sel, String translation) {
		boolean isKDB = jdbcTypes.isKDB();
		if(isKDB) {
			String ssel = sel.replace(":count *", ":count i");
			String groupby =  DBHelper.toKdbStringList(groupbylist);
			if(pivotlist.isEmpty()) {
				if(groupbylist.isEmpty()) {
					return translation;
				} else {
					return "select " + ssel + " by " + Joiner.on(',').join(groupbylist) + " from " + translation;
				}
			} else {
				String pivot =  DBHelper.toKdbStringList(pivotlist);
				return "{pulseagg:" + KDB_PULSEAGG + ";pulseagg[x;`$" + groupby + ";`$" + pivot + "; \"" + ssel + "\"]}[{" + translation + "}[]]";
			}
		} else {
			return pivotStandardSQL(jdbcTypes, groupbylist, pivotlist, sel, translation);
		}
	}

	private static String pivotStandardSQL(JdbcTypes jdbcTypes, List<String> groupbylist, List<String> pivotlist, String sel, String translation) {

//		if[(0<count pivot) and 0=count groupby;'mustSpecifyGroupbyToAllowPivot];
//		s:@[;4] parse \"select \",sel,\" from t;
//		t:$[0=count g;select from tbl where i<1000; ?[tbl;();g!g;s]];
//		$[count pivot; piv[() xkey t;groupby;pivot;$[count s;key s;cols[t] except g]]; t]}
		if(pivotlist.size() > 0 && groupbylist.size() == 0) {
			throw new RuntimeException("mustSpecifyGroupbyToAllowPivot");
		}
		if(groupbylist.size()>0 && (sel == null || sel.trim().length() == 0)) {
			throw new RuntimeException("mustSpecifyAggregatesForGroupBy");
		}
		List<String> g = new ArrayList<>(); 
		g.addAll(groupbylist); 
		g.addAll(pivotlist);
		if(translation.trim().endsWith(";")) {
			translation = translation.substring(0, translation.lastIndexOf(";"));
		}
		String t = translation.contains(" ") ? "(" + translation + ")" : translation;
		if(g.size() == 0) {
			translation = "SELECT * FROM " + t + " LIMIT 1000";
		} else {
			String gby = toColumnCSV(jdbcTypes, g);
			String plist = toColumnCSV(jdbcTypes, pivotlist);
			String sclauses = Arrays.asList(sel.split(",", -1)).stream().map(s -> toSqlSel(jdbcTypes, s)).collect(Collectors.joining(","));
			String selec = gby + ", " + sclauses;
			// Note ORDER BY essential as PivotResultSet relies on ordering.
			if(jdbcTypes.equals(JdbcTypes.DUCKDB) && pivotlist.size() > 0) {
				String grpby = toColumnCSV(jdbcTypes, groupbylist);
				translation = "PIVOT " + t + " ON " + plist + " USING " + sclauses + " GROUP BY " + grpby + " ORDER BY " + grpby;	
			} else {
				translation = "SELECT " + selec + " FROM " + t + " GROUP BY " + gby + " ORDER BY " + gby;	
			}
		}
		return translation;
	}

	private static String e(JdbcTypes jdbcTypes, String columnName) {
		if(jdbcTypes.equals(JdbcTypes.H2)) {
			return columnName.equals("*") ? "*" : "\"" + columnName.replace("\"", "\\\"") + "\"";
		}
		return columnName.equals("*") ? "*" : columnName.contains(" ") ? "\"" + columnName.replace("\"", "\\\"") + "\"" : columnName;
	}
	
	private static String toColumnCSV(JdbcTypes jdbcTypes,  List<String> sclause) {
		// H2 insists on being quoted so always quote it
		return sclause.stream().map(s ->  e(jdbcTypes, s)).collect(Collectors.joining(","));
	}
	
	private static String toSqlSel(JdbcTypes jdbcTypes, String sclause) {
		String[] assign = sclause.split(":");
		int p = assign[1].indexOf(" ");
		String op = assign[1].substring(0, p).trim().toLowerCase();
		if(op.equals("var")) { op = "VAR_POP"; }
		if(op.equals("svar")) { op = "VAR_SAMP"; }
		if(op.equals("dev")) { op = "STDDEV_POP"; }
		if(op.equals("sdev")) { op = "STDDEV_SAMP"; }
		
		String cname = assign[1].substring(p+1).trim();
		return op + "(" + e(jdbcTypes, cname) + ") as " + e(jdbcTypes, assign[0]);
	}
}
