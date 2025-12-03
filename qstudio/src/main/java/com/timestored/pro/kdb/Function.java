package com.timestored.pro.kdb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.ImageIcon;

import com.timestored.qdoc.DocSource;
import com.timestored.qdoc.DocumentedEntity;
import com.timestored.qdoc.BuiltinDocumentedEntities;
import com.timestored.theme.Theme;

/**
 * Represents a KDB function and provides static accessors to listing of known functions.
 */
class Function extends BuiltinDocumentedEntities implements DocumentedEntity,DocSource {
	
	
	private static final List<Function> knownFunctions;
	private final String url;
	
	static {
		List<Function> kf = new ArrayList<Function>();

		kf.add(new Function("and","Return the minimum value of two arguments. Commonly used as a logical and.","r:A and B","1010~1100 and 1010"));
		kf.add(new Function("asc","Accepts a single argument and if it's a list, returns it sorted in ascending order.","asc L","1 2 3~asc 2 1 3"));
		kf.add(new Function("attr","Accepts one argument and returns a symbol detailing what attribute is applied to it.","attr L","`s~attr `s#3 4 5"));
		kf.add(new Function("avg","Returns the running mean average of a list of numbers. Any null elements are ignored.","avgs L","1 1.5 2~avgs 1 2 3"));
		kf.add(new Function("binr","Gives the index of the first element in x which is >=y. Introduced in kdb+3.0 2012.07.26","r:x bin y","q)0 2 4 6 8 10 bin 5\r\n2"));
		kf.add(new Function("cor","Returns the correlation of its arguments as a floating point number between -1f and 1f. Perfectly correlated data results in a 1 or -1. When one variable increases as the other increases the correlation is positive; when one decreases as the other increases it is negative. Completely uncorrelated arguments return 0f. Arguments must be of the same length. ","X cor Y","q)29 10 54 cor 1 3 9 \r\n0.7727746 \r\nq)10 29 54 cor 1 3 9 \r\n0.9795734 \r\nq)1 3 9 cor neg 1 3 9 \r\n-1f"));
		kf.add(new Function("cov","Returns the covariance of its arguments as a floating point number. ","X cov Y","q)2 3 5 7 cov 3 3 5 9 \r\n4.5 \r\nq)2 3 5 7 cov 4 3 0 2 \r\n-1.8125 \r\nq)select price cov size by sym from trade"));
		kf.add(new Function("cut","Splits its right argument according to its left. ","X cut Y","q)4 cut til 10 \r\n0 1 2 3 \r\n4 5 6 7 \r\n8 9"));
		kf.add(new Function("deltas","returns differences between consecutive pairs","deltas A","q)deltas 8 7 6 5 4 8 9 \r\n8 -1 -1 -1 -1 4 1"));
		kf.add(new Function("dev","Computes the standard deviation of a list (as the square root of the variance). ","dev X","q)dev 8 7 6 5 4 8 9 \r\n1.665986"));
		kf.add(new Function("do","Perform a sequence of expressions a fixed number of times","do[number; expr1; expr2]","q)do[3; show `a] \r\n`a \r\n`a \r\n`a"));
		kf.add(new Function("exec","Evaluates a parse tree.","eval tree","q)eval parse \"show `a\"\r\n`a"));
		kf.add(new Function("exit","Terminate the q process with the given exit status. 0 is success.","exit val","exit 0"));
		kf.add(new Function("first","Returns the first element of its argument","first L","2~first 2 4 9 8"));
		kf.add(new Function("get","get reads or memory maps a q data file. A type error is thrown if the file is not a q data file. ","get X","t:get`:tradeTab"));
		kf.add(new Function("hclose","Closes a file or process handle. ","hclose h","hclose h"));
		kf.add(new Function("hopen","Opens a file or process handle. ","h:hopen `:host:port[:user:password]","hopen h"));
		kf.add(new Function("if","If control statement. The test expression is evaluated and the result compared with zero. If not zero, the remaining expressions are evaluated in order. The test expression should return an atom. ","if[test;exp1;...;expn] ","if[a>10;a:20;r:`aa]"));
		kf.add(new Function("load","Loads binary data from the filesystem. ","load x","q)t:([]x: 1 2 3; y: 10 20 30) \r\nq)save`t             / save to a binary file (same as `:t set t) \r\n`:t \r\nq)delete t from `.   / delete t \r\n`. \r\nq)t                  / not found \r\n't \r\n \r\nq)load`t             / load from a binary file (same as t:get `:t) \r\n`t \r\nq)t \r\nx y \r\n---- \r\n1 10 \r\n2 20 \r\n3 30"));
		kf.add(new Function("lsq","Least Squares. The lsq verb is a matrix divide. x and y must be floating point matrices with the same number of columns. The number of rows of y must be less than or equal to the number of columns, and the rows of y must be linearly independent. ","w:x lsq y","q)a:1f+3 4#til 12 \r\nq)b:4 4#2 7 -2 5 5 3 6 1 -2 5 2 7 5 0 3 4f \r\nq)a lsq b \r\n-0.1233333 0.16      0.4766667 0.28 \r\n0.07666667 0.6933333 0.6766667 0.5466667 \r\n0.2766667  1.226667  0.8766667 0.8133333"));
		kf.add(new Function("mavg","The mavg verb returns the N-item moving average of its numeric right argument, with any nulls after the first element replaced by zero. The first N items of the result arethe averages of the terms so far, and thereafter the result is the moving average. The result is floating point.\r\n\r\nN.B. Infinities (0w) are incompatible with this function. ","r:N mavg L","q)2 mavg 1 2 3 5 7 10 \r\n1 1.5 2.5 4 6 8.5 \r\nq)5 mavg 1 2 3 5 7 10 \r\n1 1.5 2 2.75 3.6 5.4 \r\nq)5 mavg 0N 2 0N 5 7 0N    / nulls after the first are replaced by 0 \r\n0n 2 2 3.5 4.666667 4.666667"));
		kf.add(new Function("md5","Computes the MD5 (Message-Digest algorithm 5) of a string. MD5 is a widely used, Internet standard (RFC 1321), hash function that computes a 128-bit hash, that is commonly used to check the integrity of files. It is not recommended for serious cryptographic protection, for which strong hashes should be used. ","md5 aString","q)md5 \"this is a not so secret message\" \r\n0x6cf192c1938b79012c323fa30e62787e "));
		kf.add(new Function("mins","Returns the minimums of the prefixes of its argument. ","mins L","q)mins 2 5 7 1 3 \r\n2 2 2 1 1 \r\nq)mins 0N 5 0N 1 3         / initial nulls return infinity \r\n0W 5 5 1 1"));
		kf.add(new Function("mmin","Returns the N-item moving minimum of its numeric right argument, with nulls treated as the minimum value. The first N items of the result are the minimums of the terms so far, and thereafter the result is the moving minimum. ","N mmin L","q)3 mmin 0N -3 -2 1 -0W 0 \r\n0N 0N 0N -3 -0W -0W \r\nq)3 mmin 0N -3 -2 1 0N -0W    / null is the minimum value \r\n0N 0N 0N -3 0N 0N"));
		kf.add(new Function("next","Returns the next element for each item in its argument list. For the last element, it returns a null value of the list if simple (a homogeneous list), and an empty list ()if non-simple (a mixed list). ","r:next L","q)next 2 3 5 7 11\r\n3 5 7 11 0N"));
		kf.add(new Function("prds","Returns the cumulative products of the items in its argument. ","r:prds L","q)prds 7                     / atom is returned unchanged \r\n7 \r\nq)prds 2 3 5 7 / cumulative products of list \r\n2 6 30 210 \r\nq)prds 2 3 0N 7              / 0N is treated as 1 \r\n2 6 6 42"));
		kf.add(new Function("prev","Returns the previous element for each item in its argument list. For the first element, it returns a null value of the list if simple (a homogeneous list), and an empty list () if non-simple (a mixed list). ","r:prev L","q)prev 2 3 5 7 11\r\n0N 2 3 5 7"));
		kf.add(new Function("prior","Apply the function f between each item of y and its predecessor. ","f prior y","q)(-)prior 2 3 4\r\n2 1 1"));
		kf.add(new Function("read0","Reads a text file, returning a list of lines. ","r:read0 `:file","q)read0 `:test.txt\r\n(\"hello\";\"goodbye\")"));
		kf.add(new Function("read1","Reads a file as a list of bytes.","r:read1 `:file","q)read1 `:test.txt \r\n0x68656c6c6f0a676f6f64"));
		kf.add(new Function("rsave","The rsave function saves a table splayed to a directory of the same name. The argument is the table name as a symbol. The table must be fully enumerated and not keyed. ","  ","  "));
		kf.add(new Function("sums","Returns the cumulative sums of the items in its argument. If the argument is an atom, it is returned unchanged. Otherwise, if the argument is a numeric list, it returns the cumulative sum as a list of the same length. A null is treated as 0. \r\nIf the argument is a non-numeric list, a type error is signaled. ","r:sums X","q)sums 7                        / cumulative sum atom (returned unchanged) \r\n7 \r\nq)sums 2 3 5 7                  / cumulative sum list \r\n2 5 10 17 \r\nq)sums 2 3 0N 7                 / 0N is treated as 0 \r\n2 5 5 12"));
		kf.add(new Function("while","While control structure. The test expression is evaluated and the result compared with zero. If not zero, the remaining expressions are evaluated in order. This is repeated as long as test is not zero. The test expression should return an atom. ","while[test;exp1;...;expn]","q)r:1 1 \r\nq)x:10 \r\nq)while[x-:1;r,:sum -2#r] \r\nq)r \r\n1 1 2 3 5 8 13 21 34 55 89"));
		kf.add(new Function("wj1","Window join is a generalization of asof join, and is available from kdb+ 2.6. asof join takes a snapshot of the current state, while window join aggregates all values of specified columns within intervals. ","wj[w;c;t;(q;(f0;c0);(f1;c1))]","q)t:([]sym:3#`ibm;time:10:01:01 10:01:04 10:01:08;price:100 101 105) \r\nq)t \r\nsym time     price \r\n------------------ \r\nibm 10:01:01 100 \r\nibm 10:01:04 101 \r\nibm 10:01:08 105 \r\n \r\nq)q:([]sym:9#`ibm;time:10:01:01+til 9;ask:101 103 103 104 104 107 108 107 108;bid:98 99 102 103 103 104 106 106 107) \r\nq)q \r\nsym time    ask bid \r\n-------------------- \r\nibm 10:01:01 101 98 \r\nibm 10:01:02 103 99 \r\nibm 10:01:03 103 102 \r\nibm 10:01:04 104 103 \r\nibm 10:01:05 104 103 \r\nibm 10:01:06 107 104 \r\nibm 10:01:07 108 106 \r\nibm 10:01:08 107 106 \r\nibm 10:01:09 108 107 \r\n \r\nq)f:`sym`time \r\nq)w:-2 1+\\:t.time \r\n \r\nq)wj[w;f;t;(q;(max;`ask);(min;`bid))] \r\nsym time     price ask bid \r\n-------------------------- \r\nibm 10:01:01 100   103 98 \r\nibm 10:01:04 101   104 99 \r\nibm 10:01:08 105   108 104"));
		kf.add(new Function("xdesc","Sorts a table in descending order of given columns. The behaviour is just like xasc except that the sort is descending and the `s# attribute is not set. The sort is stable, i.e. it preserves order amongst equals. ","q)r:cols xdesc table","q)\\l sp.q \r\nq)s \r\ns | name  status city \r\n--| ------------------- \r\ns1| smith 20     london \r\ns2| jones 10     paris \r\ns3| blake 30     paris \r\ns4| clark 20     london \r\ns5| adams 30     athens \r\nq)`city xdesc s                 / sort descending by city \r\ns | name  status city \r\n--| ------------------- \r\ns2| jones 10     paris \r\ns3| blake 30     paris \r\ns1| smith 20     london \r\ns4| clark 20     london \r\ns5| adams 30     athens"));
		kf.add(new Function("xlog","Computes the base-x logarithm of y. Integer arguments are first converted to float, and the result is type float. Null is returned if the right argument is negative, and negative infinity if the right argument is 0. ","q)r:X xlog Y","q)2 xlog 8 \r\n3f \r\nq)2 xlog 0.125 \r\n-3f \r\nq)1.5 xlog 0 0.125 1 3 0n \r\n-0w -5.128534 0 2.709511 0n"));

		
		kf.add(new Function("0:","Prepare. The dyadic prepare text function takes a separator character as its first argument and a table or a list of columns as its second. The result is a list of character strings containing text representations of the rows of the second argument separated by the first.","","show csv 0: ([]a:1 2 3;b:`x`y`z)", "http://code.kx.com/wiki/Reference/ZeroColon"));
		kf.add(new Function("1","Write to standard output","","1 \"String vector here\"","http://code.kx.com/wiki/Reference/One"));
		kf.add(new Function("1:","The 1: dyadic function is used to read fixed length data from a file or byte sequence. The leftargument is a list of types and their widths to read, and the right argument is the file handle or byte sequence.","","(\"ich\";4 1 2)1:0x00000000410000FF00000042FFFF", "http://code.kx.com/wiki/Reference/oneColon"));
		kf.add(new Function("2","write to standard error","","2 \"String vector here\"", "http://code.kx.com/wiki/Reference/Two"));
		kf.add(new Function("2:","The 2: function is a dyadic function used to dynamically load C functions into Kdb+. Its left argument is a symbol representing the name of the dynamic library from which to load the function. Its right argument is a list of a symbol which is the function name and an integer which is the number of arguments the function to be loaded takes.","","","http://code.kx.com/wiki/Reference/TwoColon"));
		kf.add(new Function("abs","Returns the absolute value of a number. i.e. the positive value","abs X","abs -2 -1 0 1"));
		kf.add(new Function("acos","Accepts a single argument in radians and returns the arc cosine.","acos[radians]","acos 3.141593"));
		kf.add(new Function("aj","Asof Join -  Joins tables based on nearest time.","aj[c1...cn;t1;t2]","aj[`sym`time; trade; quote]"));
		kf.add(new Function("all","Function all returns a boolean atom 1b if all values in its argument are non-zero, and otherwise 0b.It applies to all data types except symbol, first converting the type to boolean if necessary. ","r:all A","all 1 2 3=1 2 4"));
		kf.add(new Function("any","Return 1b if there are any non-zero values in it's argument, otherwise return 0b.","any X","any 1010111001b"));
		kf.add(new Function("asin","Accepts a single argument in radians and returns the arc sine.","sin[radians]","sin 3.141593"));
		kf.add(new Function("atan","Accepts a single argument in radians and returns the arc tan.","atan[radians]","atan 3.141593"));

		kf.add(new Function("bin","bin gives the index of the last element in x which is <=y. The result is -1 for y less than the first element of x.It uses a binary search algorithm, which is generally more efficient on large data than the linear search algorithm used by ?.The items of the left argument should be sorted non-descending although q does not enforce it. The right argument can be either an atom or simple list of the same type as the left argument. ","r:x bin y","0 2 4 6 8 10 bin 5  "));
		kf.add(new Function("ceiling","When passed floating point values, return the smallest integer greater than or equal to those values.","ceiling X","ceiling 1.2 3.4 5.8"));
		kf.add(new Function("cols","Return a symbol list of column names for a given table, or a list of keys for a dictionary.","cols[table]","cols trade"));
		kf.add(new Function("cos","Accepts a single argument in radians and returns the cosine.","cos[radians]","cos 3.141593"));
		kf.add(new Function("count","Returns the number of items in a list, for atoms 1 is returned","",""));
		kf.add(new Function("cross","Returns the cross product (i.e. all possible combinations) of its arguments.","R:X cross Y",""));
		kf.add(new Function("delete","two Formats. Either delete rows or delete columns from table.","delete col from t. delete row from t where filter.","delete from t where price<10"));
		kf.add(new Function("differ","The uniform function differ returns a boolean list indicating whether consecutive pairs differ. It applies to all data types. (The first item of the result is always 1b)","r:differ A",""));
		kf.add(new Function("each","Takes a function on its left, and creates a new function that applies to each item of its argument.","","count each (1 2;`p`o`i)"));
		kf.add(new Function("ej","Equi Join - Same as ij but allows specifying the column names.","ej[c;t1;t2]","ej[sym; trade; quote]","http://www.timestored.com/kdb-guides/qsql-inner-left-joins#equi-join"));
		kf.add(new Function("enlist","Take a single argument and return it wrapped it in a list.","enlist X","enlist 1"));
		kf.add(new Function("eval","The eval function is the dual to parse and can be used to evaluate a parse tree as returned by that function (as well as manually constructed parse trees). ","","eval parse \"2+3\""));
		kf.add(new Function("except","Returns all elements of its left argument that are not in its right argument. ","x except y","1 3 2 4 except 1 2"));
		kf.add(new Function("exp","This function computes e to the power of x, where e is the base of the natural logarithms. Null is returned if the argument is null.","R:exp 1",""));
		kf.add(new Function("fby","This verb is typically used with select, and obviates the need for many common correlated subqueries. It aggregates data in a similar way to by and computes a function on the result.","(aggr;data) fby group",""));
		kf.add(new Function("fills","Uniform function that is used to forward fill a list containing nulls.","r:fills A","fills (0N 2 3 0N 0N 7 0N)"));
		kf.add(new Function("fkeys","The function fkeys takes a table as an argument and returns a dictionary that maps foreign key columns to their tables.","",""));
		kf.add(new Function("flip","transposes its argument, which may be a list of lists, a dictionary or a table.","","flip (`a`b`c; 1 2 3)"));
		kf.add(new Function("floor","When passed floating point values, return the greatest integer less than or equal to those values.","floor X","floor 1.2 3.4 5.8"));
		kf.add(new Function("getenv","Returns the value of the given environment variable.","","getenv `PATH"));
		kf.add(new Function("group","Groups the distinct elements of its argument, and returns a dictionary whose keys are the distinct elements, and whose values are the indices where the distinct elements occur. The order of the keys is the order inwhich they are encountered in the argument.","D:group L","group \"mississippi\""));
		kf.add(new Function("gtime","The gtime function returns the UTC datetime/timestamp for a given datetime/timestamp. Recallthat the UTC and local datetime/timestamps are available as .z.z/.z.p and .z.Z/.z.P respectively.","",""));
		kf.add(new Function("hcount","Gets the size in bytes of a file as a long integer.","","hcount`:c:/q/test.txt"));
		kf.add(new Function("hdel","Delete a file.","hdel fh","hdel `:readme.txt"));
		kf.add(new Function("hsym","Converts its symbol argument into a file name, or valid hostname, ipaddress","","hsym`10.43.23.197"));
		kf.add(new Function("iasc","Uniform function that returns the indices needed to sort the list argument.","r:iasc L","iasc(2 1 3 4 2 1 2)"));
		kf.add(new Function("idesc","Uniform function that returns the indices needed to sort the list argument.","r:idesc L","idesc (2 1 3 4 2 1 2)"));
		kf.add(new Function("ij","Inner Join - Where matches occur between t and kt on primary key columns, update or add that column. Non-matches are not returned in the result.","t ij kt","([] a:1 2; b:3 4) ij ([a:2 3]; c:`p`o)","http://www.timestored.com/kdb-guides/qsql-inner-left-joins#inner-join"));
		kf.add(new Function("in","Returns a boolean indicating which items of x are in y. Result is same size as x","x in y","1 45 in 10 5 11 1"));
		kf.add(new Function("insert","Insert appends records to a table.","`table insert records","`x insert (`s`t;40 50)"));
		kf.add(new Function("inter","Returns all elements common to both arguments","x inter y","1 3 2 4 inter 1 2 6 7"));
		kf.add(new Function("inv","inv computes the inverse of a non-singular floating point matrix.","r:inv x","inv (3 3#2 4 8 35 6 0 7 1f)"));
		kf.add(new Function("key","Given a dictionary, it returns the keys","r:key D","key (`q`w`e!(1 2;3 4;5 6))"));
		kf.add(new Function("keys","Monadic function which takes a table as argument and returns a symbol list of the primary keycolumns of its argument and in the case of no keys it returns an empty symbol list. Can pass the table by reference or byvalue","keys T","keys ([s:`q`w`e]g:1 2 3;h:4 5 6)"));
		kf.add(new Function("last","Returns the item at the end of the list or table","last X","7~last 2 3 4 7"));
		kf.add(new Function("like","Perform simple pattern matching of strings.","like[text; pattern]","like[(\"kim\";\"kyle\";\"Jim\"); \"k*\"]"));
		kf.add(new Function("lj","Left Join - for each row in t, return a result row, where matches are performed by finding the first key in kt that matches. Non-matches have any new columns filled with null.","t lj kt","([] a:1 2; b:3 4) lj ([a:2 3]; c:`p`o)","http://www.timestored.com/kdb-guides/qsql-inner-left-joins#left-join"));
		kf.add(new Function("log","Returns the natural logarithm of it's argument","log X","log 0 1 2.71828"));
		kf.add(new Function("lower","Monadic that converts strings and symbols to lower case","lower[text]","`small~lower `SMALL"));
		kf.add(new Function("ltime","The ltime function returns the local datetime/timestamp for a given UTC datetime/timestamp. Recall that the UTC and local datetime/timestamps are available as .z.z/.z.p and .z.Z/.z.P respectively.","",""));
		kf.add(new Function("ltrim","Monadic that removes leading whitespace from strings.","ltrim[text]","\"abc\"~ltrim \" abc\""));
		kf.add(new Function("max","The max function returns the maximum of its argument. If the argument is an atom, it is returned unchanged. ","max a","max 1"));
		kf.add(new Function("maxs","The maxs function returns the maximums of the prefixes of its argument. If the argument is anatom, it is returned unchanged. ","maxs a","maxs 1"));
		kf.add(new Function("mcount","The mcount verb returns the N-item moving count of the non-null items of its numeric right argument. The first N items of the result are the counts so far, and thereafter the result is the moving count.","r:N mcount L","3 mcount 0N 1 2 3 0N 5"));
		kf.add(new Function("mdev","The mdev verb returns the N-item moving deviation of its numeric right argument, with any nulls after the first element replaced by zero. The first N items of the result are the deviations of the terms so far, and thereafter the result is the moving deviation. The result is floating point.","r:N mdev L","2 mdev 1 2 3 5 7 10"));
		kf.add(new Function("med","Computes the median of a numeric list.","r:med L","med 10 34 23 123 5 56"));
		kf.add(new Function("meta","The meta function returns the meta data of its table argument, passed by value or reference.","K:meta T","meta ([s:`q`w`e]g:1 2 3;h:4 5 6)"));
		kf.add(new Function("min","Returns the minimum value within a list","min X","3~min 100 10 3 22"));
		kf.add(new Function("mmax","The mmax verb returns the N-item moving maximum of its numeric right argument, with nulls after the first replaced by the preceding maximum. The first N items of the result are the maximums of the terms so far, and thereafter the result is the moving maximum.","r:N mmax L","3 mmax 2 7 1 3 5 2 8"));
		kf.add(new Function("mmu","mmu computes the matrix multiplication of floating point matrices. The arguments must be floating point and must conform in the usual way, i.e. the columns of x must equal the rows of y.","r:x mmu y","(2 4#2 4 8 3 5 6 0 7f) mmu (4 3#`float$til 12)"));
		kf.add(new Function("mod","x mod y returns the remainder of y%x. Applies to numeric types, and gives type error on sym, char and temporal types.","r:L mod N","-3 -2 -1 0 1 2 3 4 mod 3"));
		kf.add(new Function("msum","The msum verb returns the N-item moving sum of its numeric right argument, with nulls replaced by zero. The first N items of the result are the sums of the terms so far, and thereafter the result is the moving sum.","r:N msum L","3 msum 1 2 3 5 7 11"));
		kf.add(new Function("neg","The function neg negates its argument, e.g. neg 3 is -3. Applies to all data types except sym and char. Applies item-wise to lists, dict values and table columns. ","r:neg X","neg -1 0 1 2"));
		kf.add(new Function("not","The logical not function returns a boolean result 0b when the argument is not equal to zero, and 1b otherwise. Applies to all data types except sym. Applies item-wise to lists, dict values and table columns. ","r:notX","not -1 0 1 2"));
		kf.add(new Function("null","The function null returns 1b if its argument is null.Applies to all data types. Applies item-wise to lists, dict values and table columns. ","r:null X","null 0 0n 0w 1 0n"));
		kf.add(new Function("or","The verb or returns the maximum of its arguments. It applies to all data types except symbol.","r:L or Y","-2 0 3 7 or 0 1 3 4"));
		kf.add(new Function("over","The over adverb takes a function of two arguments on its left, and creates a new atomic function that applies to successive items of its list argument. The result is the result of the last application of the function.","r:f over L","{x+2*y} over 2 3 5 7"));
		kf.add(new Function("parse","parse is a monadic function that takes a string and parses it as a kdb+ expression, returning the parse tree. To execute a parsed expression, use the eval function. ","","parse \"{x+42} each til 10\"", "http://www.timestored.com/kdb-guides/functional-queries-dynamic-sql#parse"));
		kf.add(new Function("peach","The parallel each adverb is used for parallel execution of a function over data. In order toexecute in parallel, q must be started with multiple slaves (-s). ","","{sum exp x?1.0}peach 2#1000000 "));
		kf.add(new Function("pj","Plus Join - Same principle as lj, but existing values are added to where column names match.","t pj kt","","http://www.timestored.com/kdb-guides/qsql-inner-left-joins#plus-join"));
		kf.add(new Function("prd","Aggregation function, also called multiply over, applies to all numeric data types. It returnsa type error with symbol, character and temporal types. prd always returns an atom and in the case of application to an atom returns the argument.","ra:prd L","prd 2 4 5 6"));
		kf.add(new Function("rand","If X is an atom 0, it returns a random value of the same type in the range of that type:","r:rand 0","rand each 3#0h"));
		kf.add(new Function("rank","The uniform function rank takes a list argument, and returns an integer list of the same length. Each value is the position where the corresponding list element would occur in the sorted list. This is the same as calling iasc twice on the list.","r:rank L","rank 2 7 3 2 5"));
		kf.add(new Function("ratios","The uniform function ratios returns the ratio of consecutive pairs. It applies to all numeric data types.","r:ratios L","ratios 1 2 4 6 7 10"));
		kf.add(new Function("raze","The raze function joins items of its argument, and collapses one level of nesting. To collapse all levels, use over i.e. raze/[x].  An atom argument is returned as a one-element list.","","raze (1 2;3 4 5)"));
		kf.add(new Function("read0","The read0 function reads a text file, returning a list of lines.Lines are assumed delimited by either LF or CRLF, and the delimiters are removed. ","","read0`:test.txt"));
		kf.add(new Function("read1","The read1 function reads a file as a list of bytes.","","read1`:test.txt   "));
		kf.add(new Function("reciprocal","Returns the reciprocal of its argument. The argument is first cast to float, and the result is float.","r:reciprocal X","reciprocal 0 0w 0n 3 10"));
		kf.add(new Function("reverse","Uniform function that reverses the items of its argument. On dictionaries, reverses the keys; and on tables, reverses the columns","","reverse 1 2 3 4"));
		kf.add(new Function("rload","The rload function loads a splayed table. This can also be done, as officially documented, using the get function. ","",""));
		kf.add(new Function("rotate","The uniform verb rotate takes an integer left argument and a list or table right argument. This rotates L by N positions to the left for positive N, and to the right for negative N. ","r:N rotate L","2 rotate 2 3 5 7 11"));
		kf.add(new Function("rtrim","Monadic that removes trailing whitespace from strings.","rtrim[text]","\"abc\"~rtrim \"abc \""));
		kf.add(new Function("save","The save function saves data to the filesystem.","","t:([]x: 1 2 3; y: 10 20 30);save `t"));
		kf.add(new Function("scan","The scan adverb takes a function of two arguments on its left, and creates a new uniform function that applies to successive items of its list argument. The result is a list of the same length.","r:f scan L","{x+2*y} scan 2 3 5 7"));
		kf.add(new Function("select","Select rows from a table.","select columns by groups from table where filters","select max price by date,sym from trade where sym in `AA`C"));
		kf.add(new Function("set","Dyadic functional form of assignment often used when saving objects to disk. set is used mainly to write data to disk and in this case the left argument is a file path, i.e. a symbol atom beginning with a :","","`:c:/q/testTradeTable set trade"));
		kf.add(new Function("setenv","Dyadic function which changes or adds an environment variable.","","`name setenv value"));
		kf.add(new Function("show","Monadic function used to pretty-print data to the console.","","show 10#enlist til 10"));
		kf.add(new Function("signum","The function signum returns -1, 0 or 1 if the argument is negative, zero or positive respectively. Applies item-wise to lists, dictionaries and tables, and to all data types except symbol.","r:signum X","signum -20 1 3"));
		kf.add(new Function("sin","Accepts a single argument in radians and returns the sine.","sin[radians]","sin 3.141593"));
		kf.add(new Function("sqrt","Returns the square root of it's argument","sqrt X","sqrt 0 1 4 9"));
		kf.add(new Function("ss","The function ss finds positions of a substring within a string. It also supports some pattern matching capabilities of the function like: ","r:HayStack ss needle","\"toronto ontario\" ss \"ont\""));
		kf.add(new Function("ssr","The function ssr does search and replace on a string.","r:ssr[haystack; needle;  replacement]","ssr[\"toronto ontario\"; \"ont\"; \"XX\"]"));
		kf.add(new Function("string","The function string converts each atom in its argument to a character string. It applies toall data types.","r:string X","string ([]a:1 2 3;b:`ibm`goog`aapl)"));
		kf.add(new Function("sublist","The verb sublist returns a sublist of its right argument, as specified by its left argument. The result contains only as many items as are available in the right argument. If X is a single integer, it returns X items from the beginning of Y if positive, or from the end of Y if negative.If X is an integer pair, it returns X 1 items from Y, starting at item X 0. ","r:X sublist Y","3 sublist 2 3 5 7 11"));
		kf.add(new Function("sum","Returns the sum total of a list","sum X","14~sum 2 4 8"));
		kf.add(new Function("sv","scalar from vector- dyadic function that performs different functions on different data types.","",""));
		kf.add(new Function("system","Monadic function which executes system commands i.e. OS commands.","","system\"pwd\""));
		kf.add(new Function("tables","Monadic function which returns a list of the tables in the specified namespace, this list is sorted.","","tables`."));
		kf.add(new Function("tan","Accepts a single argument in radians and returns the tan.","tan[radians]","tan 3.141593"));
		kf.add(new Function("til","takes positive integer n and returns list of numbers from 0 to n-1","","til 9"));
		kf.add(new Function("trim","Monadic that removes leading and trailing whitespace from strings.","trim[text]","\"abc\"~trim \" abc \""));
		kf.add(new Function("type","This monadic function returns the type of its argument as a short integer. Negatives numbers are for atoms, positive numbers are for lists, and zero is a general K list.","",""));
		kf.add(new Function("uj","Union Join - Combine all columns from both tables. Where possible common columns append or new columns created and filled with nulls.","t1 uj t2","([] a:1 2; b:3 4) uj ([] a:2 3; c:`p`o)","http://www.timestored.com/kdb-guides/qsql-inner-left-joins#union-join"));
		kf.add(new Function("ungroup","The ungroup function monadic function ungroups a table.","",""));
		kf.add(new Function("union","Dyadic function which returns the union of its arguments, i.e. returns the distinct elementsof the combined lists respecting the order of the union.","R:X union Y","1 2 3 union 2 4 6 8"));
		kf.add(new Function("update","Modify the table to update existing values or to create a new column.","update col by c2 from t where filter","update price:price+10 from t where sym=`A"));
		kf.add(new Function("upper","Monadic that converts strings and symbols to upper case","lower[text]","`BIG~upper `big"));
		kf.add(new Function("upsert","Functional form of inserting into a table using the , primitive. It is called upsert because when applied to keyed tables it performs an insert if the key value is not there and otherwise performs an update.","r: T upsert newEntries","([s:`q`w`e]r:1 2 3;u:5 6 7) upsert (`q;100;500)"));
		kf.add(new Function("value","This is the same verb as get but is typically used for different things.","",""));
		kf.add(new Function("var","Aggregation function which applies to a list of numerical types and returns the variance of the list. Again for the usual reason it works on the temporal types.","r:var X","var 10 343 232 55"));
		kf.add(new Function("view","Monadic function which returns the expression defining the dependency passed as its symbol argument.","",""));
		kf.add(new Function("views","Monadic function which returns a list of the currently defined views in the root directory, this list is sorted and has the `s attribute set.","",""));
		kf.add(new Function("vs","The dyadic vs vector from scalar function has several uses.","",""));
		kf.add(new Function("wavg","Dyadic where first arg is weights, second is values, returns the weighted average.","X wavg Y","7=1 1 2 wavg 5 5 9"));
		kf.add(new Function("where","Where the argument is a boolean list, this returns the indices of the 1's","","where 21 2 5 11 33 9>15"));
		kf.add(new Function("within","The right argument of this primitive function is always a two-item list. The result is a boolean list with the same number of items as the left argument. The result indicates whether or not each item of the left argument is within the bounds defined by the right argument.","","1 3 10 6 4 within 2 6"));
		kf.add(new Function("wj","Window join is a generalization of asof join, and is available from kdb+ 2.6. asof join takes asnapshot of the current state, while window join aggregates all values of specified columns within intervals.","",""));
		kf.add(new Function("wsum","The weighted sum aggregation function wsum produces the sum of the items of its right argument weighted by the items of its left argument. The left argument can be a scalar, or a list of equal length to the right argument. When both arguments are integer lists, they are converted to floating point. Any null elements in the arguments are excluded from the calculation.","","2 3 4 wsum 1 2 4"));
		kf.add(new Function("xasc","Dyadic function-sorts a table in ascending order of a particular column, sorting is order preserving among equals. Takes a symbol list or atom and a table as arguments and returns the original table sorted by the columns as specified in the first argument.","R:C xasc T","t:`sym xasc trade "));
		kf.add(new Function("xbar","Interval bars are prominent in aggregation queries. For example, to roll-up prices and sizes in 10 minute bars:","","select last price, sum size by 10 xbar time.minute from trade"));
		kf.add(new Function("xcol","Dyadic function - rename columns in a table. Takes a symbol list of column names and a table as arguments, and returns the table with the new column names. The number of column names must be less than or equal to the number of columns in the table. The table must be passed by value.","R:C xcol T","`A`S`D`F xcol trade"));
		kf.add(new Function("xcols","Dyadic function - reorder columns in a table. Takes a symbol list of column names and a table as arguments and returns the table with the named columns moved to the front. The column names given must belong to the table. The table must have no primary keys and is passed by value.","R:C xcols T","xcols[reverse cols trade;trade]"));
		kf.add(new Function("xexp","This is the dyadic power function.","r:xexp[X;Y]","2 xexp 3"));
		kf.add(new Function("xgroup","The xgroup function dyadic function groups its right argument by the left argument (which is a foreign key).","",""));
		kf.add(new Function("xkey","Dyadic function-sets a primary in a table. Takes a symbol list or atom and a table as arguments and returns the original table with a primary key corresponding to the column(s) specified in the first argument.","R:Cxkey T","`r xkey ([s:`q`w`e]r:1 2 3;u:5 6 7)"));
		kf.add(new Function("xprev","Uniform dyadic function, returns the n previous element to each item in its argument list.","r:N xprev A","2 xprev 2 3 4 5 6 7 8"));
		kf.add(new Function("xrank","Uniform dyadic function which allocates values to buckets based on value. This is commonly used to place items in N equal buckets.","",""));
		knownFunctions = Collections.unmodifiableList(kf);
	}

	private Function(String name, String description, String syntax, String eg) {
		super(name, description, syntax, eg);
		this.url = null;
	}

	private Function(String name, String description, String syntax, String eg, String url) {
		super(name, description, syntax, eg);
		this.url = url;
	}
	
	public String getLink() {
		if(url != null) {
			return url;
		} 
		return "http://code.kx.com/wiki/Reference/" + docname;
	}
	
	public static List<Function> getKnownfunctions() {
		return knownFunctions;
	}

	@Override public ImageIcon getIcon() {
		return Theme.CIcon.FUNCTION_ELEMENT.get16();
	}

	@Override public List<? extends DocumentedEntity> getDocs() {
		return knownFunctions;
	}

	@Override public String getSource() {
		return ".q";
	}

	
}
