/*
 * Copyright (c) 1998-2017 Kx Systems Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package kx;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.UUID;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.timestored.qstudio.kdb.KdbHelper;

import kx.c.Dict;
import kx.c.Flip;


/**
 * Connector class for interfacing with a kdb+ process. This class is essentially a serializer/deserializer of java types
 * to/from the kdb+ ipc wire format, enabling remote method invocation in kdb+ via tcp/ip.
 * <p>
 * Further information can be found at <a href="https://code.kx.com/q/interfaces/java-client-for-q/">https://code.kx.com/q/interfaces/java-client-for-q/</a>
 * <p>
 * To begin with, a connection may be established to a listening kdb+ process via the constructor
 * 
 * <code>c connection=new c("localhost",5000);</code>
 * </p>
 * <p>
 *  There are then 3 methods available for interacting with the connection:
 *   <ol>
 *     <li>Sending a sync message using k() <br>
 *       <code>Object result=connection.k("functionName",args);</code>
 * 
 *     <li>Sending an async message using ks()<br>
 *       <code>connection.ks("functionName",args); </code>
 *     
 *     <li>Awaiting an incoming async message using k()<br>
 *       <code>Object object=connection.k();</code>. <br> 
 *       When the connection is no longer required, it may be closed via connection.close();
 *   </ol>
 */
public class c implements AutoCloseable {
  /**
   * Encoding specifies the character encoding to use when [de]-serializing strings.
   */
  private static String encoding="ISO-8859-1";
  /**
   *  {@code sync}  tracks how many response messages the remote is expecting
   */
  protected int sync=0;
  /**
   * Representation of a null for a time atom within kdb
   */
  public static final LocalTime LOCAL_TIME_NULL = LocalTime.ofNanoOfDay(1);
  /**
   * Sets character encoding for serialising/deserialising strings.
   * 
   * @param encoding The name of a supported
   *                 <a href="../lang/package-summary.html#charenc">
   *                 character encoding</a>
   * @throws UnsupportedEncodingException
   *                If the named encoding is not supported
   */
  public static void setEncoding(String encoding) throws UnsupportedEncodingException{
    c.encoding=encoding;
  }
  /**
   * {@code s} is the socket used to communicate with the remote kdb+ process.
   */
  public Socket s;
  /**
   * {@code i} is the {@code DataInputStream} of the socket used to read data from the remote kdb+ process.
   */
  protected DataInputStream inStream;
  /**
   * {@code o} is the outputStream of the socket used to write data to the remote kdb+ process.
   */
  protected OutputStream outStream;
  /**
   * {@code b} is the buffer used to store the incoming message bytes from the remote prior to de-serialization
   */
  protected byte[] rBuff;
  /**
   * {@code j} is the current position of the de-serializer within the read buffer b
   */
  protected int rBuffPos;
  /**
   * {@code wBuff} is the buffer used to store the outgoing message bytes when serializing an object
   */
  byte[] wBuff;
  /**
   * {@code wBuffPos} is the current position the serializer within the write buffer wBuff
   */
  int wBuffPos;
  /**
  * {@code ipcVersion} indicates the ipc version to encode with
  */
  int ipcVersion;
  /**
   * Marks whether the message being deserialized was encoded little or big endian.
   */
  protected boolean isLittleEndian;
  /**
   * Indicates whether the current connection is to a local interface. Tested when considering whether to compress an outgoing message.
   */
  boolean isLoopback;
  /**
   * Indicates whether messages should be candidates for compressing before sending (given uncompressed serialized data also has a length 
   * greater than 2000 bytes and connection is not localhost)
   */
  boolean zip;
public String host;
public int port;


private static final char[][] FUNC_1;
private static final char[][] FUNC_2;
private static final String[] ADVERBS = new String[] { "'", "/", "\\", "':", "/:", "\\:" };

static {
	final String[] monadics ={null,"+:","-:","*:","%:","&:","|:","^:","=:","<:",
		">:","$:",",:","#:","_:","~:","!:","?:","@:",".:","0::","1::","2::","avg",
		"last","sum","prd","min","max","exit","getenv","abs","sqrt","log","exp",
		"sin","asin","cos","acos","tan","atan", "enlist", "var", "dev", "hopen"};
	FUNC_1 = toChars(monadics);

	final String[] dyads ={":","+","-","*","%","&","|","^","=","<",">","$",
		",","#","_","~","!","?","@",".","0:","1:","2:","in","within","like","bin",
		"ss","insert","wsum","wavg","div","xexp","setenv", "binr", "cov", "cor"};
	FUNC_2 = toChars(dyads);
}

private static char[][] toChars(final String[] monadics) {
	char[][] a = new char[monadics.length][];
	for(int i=0; i<a.length; i++) {
		a[i] = monadics[i]==null ? null : monadics[i].toCharArray();
	}
	return a;
}

  /**
   * Sets whether or not to consider compression on outgoing messages (given uncompressed serialized data also has a length 
   * greater than 2000 bytes and connection is not localhost)
   * @param b true if to use a compression. Default is false.
   * @see <a href="https://code.kx.com/q/ref/ipc/#compression">IPC compression</a>
   */
  public void zip(boolean b){
    zip=b;
  }
  /**
   * Prepare socket for kdb+ ipc comms
   * @param x socket to setup
   * @throws IOException an I/O error occurs.
   */
  void io(Socket x) throws IOException{
    s=x;
    s.setTcpNoDelay(true);
    InetAddress addr=s.getInetAddress();
    isLoopback=addr.isAnyLocalAddress()||addr.isLoopbackAddress();
    inStream=new DataInputStream(s.getInputStream());
    outStream=s.getOutputStream();
    s.setKeepAlive(true);
  }

  /** 
   * Closes the current connection to the remote process. 
   * @throws IOException if an I/O error occurs when closing this socket.
   */
  public void close() throws IOException{
    if(null!=s){
      s.close();
      s=null;
    }
    if(null!=inStream){
      inStream.close();
      inStream=null;
    }
    if(null!=outStream){
      outStream.close();
      outStream=null;
    }
  }

  /** When acting as a server for client connections, {@code IAuthenticate} describes an interface to 
   * use in order to authenticate incoming connections based on the KDB+ handshake.
   * */
  public interface IAuthenticate{
    /**
     * Checks authentication string provided to allow/reject connection. 
     * @see <a href="https://code.kx.com/q/ref/dotz/#zpw-validate-user">.z.pw</a>
     * @param s String containing username:password for authentication
     * @return true if credentials accepted. 
     */
    public boolean authenticate(String s);
  }

  /**
   * Initializes a new {@link c} instance by acting as a server, blocking
   * till a client connects and authenticates using the KDB+ protocol. This object
   * should be used for a single client connection. A new instance should be created 
   * for each new client connection.
   * @param s {@link ServerSocket} to accept connections on using kdb+ IPC protocol.
   * @param a {@link IAuthenticate} instance to authenticate incoming connections. 
   *          Accepts all incoming connections if {@code null}.
   * @throws IOException if access is denied or an I/O error occurs.
   * 
   */
  public c(Socket s,IAuthenticate a) throws IOException{
    io(s);
    rBuff=new byte[99];
    int bytesRead=inStream.read(rBuff);
    if(a!=null&&!a.authenticate(new String(rBuff,0,bytesRead>1?bytesRead-2:0))){
      close();
      throw new IOException("access");
    }
    ipcVersion=bytesRead>1?rBuff[bytesRead-2]:0;
    rBuff[0]=(byte)(ipcVersion<'\3'?ipcVersion:'\3');
    outStream.write(rBuff,0,1);
  }

  /** 
   * Initializes a new {@link c} instance by acting as a server, and blocks while waiting 
   * for a new client connection. A new instance should be created for each new client connection.
   * @param s {@link ServerSocket} to accept connections on using kdb+ IPC protocol.
   * @throws IOException an I/O error occurs.
   */
  public c(Socket s) throws IOException{
    this(s,null);
  }
  /**
   * Initializes a new {@link c} instance and connects to KDB+ over TCP.
   * @param host Host of remote q process
   * @param port Port of remote q process
   * @param usernamepassword Username and password as "username:password" for remote authorization
   * @throws KException if access denied
   * @throws IOException if an I/O error occurs.
   */
  public c(String host,int port,String usernamepassword) throws KException,IOException{
    this(host,port,usernamepassword,false);
  }

  /**
   * Initializes a new {@link c} instance and connects to KDB+ over TCP with optional TLS support for encryption.
   * @param host Host of remote q process
   * @param port Port of remote q process
   * @param usernamepassword Username and password as "username:password" for remote authorization
   * @param useTLS whether to use TLS to encrypt the connection
   * @throws KException if access denied
   * @throws IOException if an I/O error occurs.
   */
  public c(String host,int port,String usernamepassword,boolean useTLS) throws KException,IOException{
	this.host = host;
	this.port = port;
    wBuff=new byte[2+ns(usernamepassword)];
    s=new Socket(host,port);
    if(useTLS){
      try{
        s=((SSLSocketFactory)SSLSocketFactory.getDefault()).createSocket(s,host,port,true);
        ((SSLSocket)s).startHandshake();
      }
      catch(Exception e){
        s.close();
        throw e;
      }
    }
    io(s);
    wBuffPos=0;
    w(usernamepassword+"\3");
    outStream.write(wBuff);
    if(1!=inStream.read(wBuff,0,1)){
      close();
      throw new KException("access");
    }
    ipcVersion=Math.min(wBuff[0],3);
  }
  /**
   * Initializes a new {@link c} instance and connects to KDB+ over TCP, using {@code user.name} system property for username and password criteria.
   * The {@code user.name} system property should be set to a value in the "username:password" format for remote authorization
   * 
   * @param host Host of remote q process
   * @param port Port of remote q process
   * @throws KException if access denied
   * @throws IOException if an I/O error occurs.
   */
  public c(String host,int port) throws KException,IOException{
    this(host,port,System.getProperty("user.name"));
  }
  /** Initializes a new {@link c} instance for the purposes of serialization only, no connection is instantiated
   * to/from a KDB+ process */
  public c(){
    ipcVersion='\3';
    isLoopback=false;
    inStream=new DataInputStream(new InputStream(){
      @Override 
      public int read()throws IOException{
        throw new UnsupportedOperationException("nyi");
      }});
    outStream=new OutputStream(){
      @Override 
      public void write(int b)throws IOException{
        throw new UnsupportedOperationException("nyi");
      }};
  }

  /** {@code Month} represents kdb+ month type, which is the number of months since Jan 2000. */
  public static class Month implements Comparable<Month>{
    /** Number of months since Jan 2000 */
    public int i;
    /**
     * Create a KDB+ representation of 'month' type from the q language 
     * (a month value is the count of months since the beginning of the millennium. 
     * Post-milieu is positive and pre is negative)
     * @param x Number of months from millennium
     */
    public Month(int x){
      i=x;
    }
    @Override
	public String toString() {
		int m = i + 24000, y = m / 12;
		return i == ni ? "" : i == Integer.MAX_VALUE ? "0W" : i == (ni+1) ? "-0W" : 
				i2(y / 100) + i2(y % 100) + "."
				+ i2(1 + m % 12);
	}
    @Override
    public boolean equals(final Object o){
      return ((o instanceof Month) && (((Month)o).i==i));
    }
    @Override
    public int hashCode(){
      return i;
    }
    @Override
    public int compareTo(Month m){
      return i-m.i;
    }
  }

  /** {@code Minute} represents kdb+ minute type, which is a time represented as the number of minutes from midnight. */
  public static class Minute implements Comparable<Minute>{
    /** Number of minutes since midnight. */
    public int i;
    /**
     * Create a KDB+ representation of 'minute' type from the q language 
     * (point in time represented in minutes since midnight)
     * @param x Number of minutes since midnight
     */
    public Minute(int x){
      i=x;
    }
    @Override
	public String toString() {
		return i == ni ? "" : i == Integer.MAX_VALUE ? "0W" :
				i == (ni+1) ? "-0W" : i2(i / 60) + ":" + i2(i % 60);
	}
    @Override
    public boolean equals(final Object o){
      return ( (o instanceof Minute)&&(((Minute)o).i==i) );
    }
    @Override
    public int hashCode(){
      return i;
    }
    @Override
    public int compareTo(Minute m){
      return i-m.i;
    }
  }

  /** {@code Second} represents kdb+ second type, which is a point in time represented in seconds since midnight. */
  public static class Second implements Comparable<Second>{
    /** Number of seconds since midnight. */
    public int i;
    /**
     * Create a KDB+ representation of 'second' type from the q language 
     * (point in time represented in seconds since midnight)
     * @param x Number of seconds since midnight
     */
    public Second(int x){
      i=x;
    }
    @Override
	public String toString() {
		return i == ni ? "" : i == Integer.MAX_VALUE ? "0W" :
			i == (ni+1) ? "-0W" :
			new Minute(i / 60).toString() + ':' + i2(i % 60);
	}
    @Override
    public boolean equals(final Object o){
      return ( (o instanceof Second)&&(((Second)o).i==i) );
    }
    @Override
    public int hashCode(){
      return i;
    }
    @Override
    public int compareTo(Second s){
      return i-s.i;
    }
  }

  /** {@code Timespan} represents kdb+ timestamp type, which is a point in time represented in nanoseconds since midnight. */
  public static class Timespan implements Comparable<Timespan>{
    /** Number of nanoseconds since midnight. */
    public long j;
    /**
     * Create a KDB+ representation of 'timespan' type from the q language 
     * (point in time represented in nanoseconds since midnight)
     * @param x Number of nanoseconds since midnight
     */
    public Timespan(long x){
      j=x;
    }
    /** Constructs {@code Timespan} using current time since midnight and default timezone. */
    public Timespan(){
      this(TimeZone.getTimeZone("UTC"));
    }
    /** 
     * Constructs {@code Timespan} using current time since midnight and default timezone. 
     * @param tz {@code TimeZone} to use for deriving midnight.
     */
    public Timespan(TimeZone tz){
      Calendar c=Calendar.getInstance(tz);
      long now=c.getTimeInMillis();
      c.set(Calendar.HOUR_OF_DAY,0);
      c.set(Calendar.MINUTE,0);
      c.set(Calendar.SECOND,0);
      c.set(Calendar.MILLISECOND,0);
      j=(now-c.getTimeInMillis())*1000000L;
    }
    @Override
	public String toString() {
		if (j == nj) {
			return "";
		} else if(j == Long.MAX_VALUE) {
			return "0W";
		} else if(j == (nj+1)) {
			return "-0W";
		}
		String s = j < 0 ? "-" : "";
		long jj = j < 0 ? -j : j;
		int d = ((int) (jj / 86400000000000L));
		if (d != 0)
			s += d + "D";
		return s + i2((int) ((jj % 86400000000000L) / 3600000000000L))
				+ ":" + i2((int) ((jj % 3600000000000L) / 60000000000L))
				+ ":" + i2((int) ((jj % 60000000000L) / 1000000000L)) + "."
				+ i9((int) (jj % 1000000000L));
	}
    @Override
    public int compareTo(Timespan t){
      if (j>t.j)
        return 1;
      return j<t.j?-1:0;
    }
    @Override
    public boolean equals(final Object o){
      return ( (o instanceof Timespan) && (((Timespan)o).j==j) );
    }
    @Override
    public int hashCode(){
      return (int)(j^(j>>>32));
    }
  }
  /**
   * {@code Dict} represents the kdb+ dictionary type, which is a mapping from a key list to a value list.
   * The two lists must have the same count.
   * An introduction can be found at <a href="https://code.kx.com/q4m3/5_Dictionaries/">https://code.kx.com/q4m3/5_Dictionaries/</a>
   */
  public static class Dict{
    /** Dict keys */
    public Object x;
    /** Dict values */
    public Object y;
    /**
     * Create a representation of the KDB+ dictionary type, which is a 
     * mapping between keys and values
     * @param keys Keys to store. Should be an array type when using multiple values.
     * @param vals Values to store. Index of each value should match the corresponding associated key.
    *  Should be an array type when using multiple values.
     */
    public Dict(Object keys,Object vals){
      x=keys;
      y=vals;
    }
    
    @Override public String toString() {
    	if(x instanceof Flip && y instanceof Flip) {
			return "([" + Joiner.on("; ").join(((Flip)x).x) + "] " + Joiner.on("; ").join(((Flip)y).x) + ")";
    	}
    	return KdbHelper.asLine(x) + "!" + KdbHelper.asLine(y);
    }
  }
  /**
   * {@code Flip} represents a kdb+ table (an array of column names, and an array of arrays containing the column data).
   * q tables are column-oriented, in contrast to the row-oriented tables in relational databases.
   * An introduction can be found at <a href="https://code.kx.com/q4m3/8_Tables/">https://code.kx.com/q4m3/8_Tables/</a>
   */
  public static class Flip{
    /** Array of column names. */
    public String[] x;
    /** Array of arrays of the column values. */
    public Object[] y;
    /**
     * Create a Flip (KDB+ table) from the values stored in a Dict.
     * @param dict Values stored in the dict should be an array of Strings for the column names (keys), with an 
     * array of arrays for the column values
     */
    public Flip(Dict dict){
      x=(String[])dict.x;
      y=(Object[])dict.y;
    }
    /**
     * Returns the column values given the column name
     * @param s The column name
     * @return The value(s) associated with the column name which can be casted to an array of objects.
     */
    public Object at(String s){
      return y[find(x,s)];
    }
    
    @Override public String toString() {
    	return "([] " + Joiner.on("; ").join(x) + ")";
    }
  }
  
  /**
   * {@code KException} is used to indicate there was an error generated by the remote process during the processing of a sync message or if the connection failed due to access credentials. 
   * Network errors are reported as IOException.
   */
  public static class KException extends Exception{

		private final String title; 
		private final String stackMessage; 
		
		public KException(String title, String stackMessage) {
			super(title);
			this.title = Preconditions.checkNotNull(title);
			this.stackMessage = Preconditions.checkNotNull(stackMessage);
		}

		public KException(String title) {
			this(title, "");
		}

		public String getTitle() {
			return title;
		}

		public String getStackMessage() {
			StringBuilder sb = new StringBuilder(title);
			if (stackMessage.length() > 0) {
				sb.append("\r\n").append(stackMessage);
			}
			return sb.toString();
		}
	}
  
  private void compress(){
    byte i=0;
    boolean g;
    final int origSize=wBuffPos;
    int f=0;
    int h0=0;
    int h=0;
    byte[] y=wBuff;
    wBuff=new byte[y.length/2];
    int c=12;
    int d=c;
    int e=wBuff.length;
    int p=0;
    int q;
    int r;
    int s0=0;
    int s=8;
    int t=wBuffPos;
    int[] a=new int[256];
    System.arraycopy(y,0,wBuff,0,4);
    wBuff[2]=1;
    wBuffPos=8;
    w(origSize);
    for(;s<t;i*=2){
      if(0==i){
        if(d>e-17){
          wBuffPos=origSize;
          wBuff=y;
          return;
        }
        i=1;
        wBuff[c]=(byte)f;
        c=d++;
        f=0;
      }
      g=(s>t-3)||(0==(p=a[h=0xFF&(y[s]^y[s+1])]))||(0!=(y[s]^y[p]));
      if(0<s0){
        a[h0]=s0;
        s0=0;
      }
      if(g){
        h0=h;
        s0=s;
        wBuff[d++]=y[s++];
      }else{
        a[h]=s;
        f|=i;
        p+=2;
        r=s+=2;
        q=Math.min(s+255,t);
        while(y[p]==y[s]&&++s<q)
          ++p;
        wBuff[d++]=(byte)h;
        wBuff[d++]=(byte)(s-r);
      }
    }
    wBuff[c]=(byte)f;
    wBuffPos=4;
    w(d);
    wBuffPos=d;
    wBuff=Arrays.copyOf(wBuff,wBuffPos);
  }
  private void uncompress(){
    int n=0;
    int r=0;
    int f=0;
    int s=8;
    int p=s;
    short i=0;
    byte[] dst=new byte[ri()];
    int d=rBuffPos;
    int[] aa=new int[256];
    while(s<dst.length){
      if(i==0){
        f=0xff&(int)rBuff[d++];
        i=1;
      }
      if((f&i)!=0){
        r=aa[0xff&(int)rBuff[d++]];
        dst[s++]=dst[r++];
        dst[s++]=dst[r++];
        n=0xff&(int)rBuff[d++];
        for(int m=0;m<n;m++)
          dst[s+m]=dst[r+m];
      }else
        dst[s++]=rBuff[d++];
      while(p<s-1)
        aa[(0xff&(int)dst[p])^(0xff&(int)dst[p+1])]=p++;
      if((f&i)!=0)
        p=s+=n;
      i*=2;
      if(i==256)
        i=0;
    }
    rBuff=dst;
    rBuffPos=8;
  }
  /**
   * Write byte to serialization buffer and increment buffer position
   * @param x byte to write to buffer
   */
  void w(byte x){
    wBuff[wBuffPos++]=x;
  }
  /** null integer, i.e. 0Ni */
  static int ni=Integer.MIN_VALUE; 
  /** null long, i.e. 0N */
  static long nj=Long.MIN_VALUE;
  /** null float, i.e. 0Nf or 0n */
  static double nf=Double.NaN;
  /**
   * Deserialize boolean from byte buffer
   * @return Deserialized boolean
   */
  boolean rb(){
    return 1==rBuff[rBuffPos++];
  }
  /**
   * Write boolean to serialization buffer
   * @param x boolean to serialize
   */
  void w(boolean x){
    w((byte)(x?1:0));
  }
  /**
   * Deserialize char from byte buffer
   * @return Deserialized char
   */
  char rc(){
    return (char)(rBuff[rBuffPos++]&0xff);
  }
  /**
   * Write char to serialization buffer
   * @param c char to serialize
   */
  void w(char c){
    w((byte)c);
  }
  /**
   * Deserialize short from byte buffer
   * @return Deserialized short
   */
  short rh(){
    int x=rBuff[rBuffPos++];
    int y=rBuff[rBuffPos++];
    return (short)(isLittleEndian?x&0xff|y<<8:x<<8|y&0xff);
  }
  /**
   * Write short to serialization buffer in big endian format
   * @param h short to serialize
   */
  void w(short h){
    w((byte)(h>>8));
    w((byte)h);
  }
  /**
   * Deserialize int from byte buffer
   * @return Deserialized int
   */
  int ri(){
    int x=rh();
    int y=rh();
    return isLittleEndian?x&0xffff|y<<16:x<<16|y&0xffff;
  }
  /**
   * Write int to serialization buffer in big endian format
   * @param i int to serialize
   */
  void w(int i){
    w((short)(i>>16));
    w((short)i);
  }
  /**
   * Deserialize UUID from byte buffer
   * @return Deserialized UUID
   */
  UUID rg(){
    boolean oa=isLittleEndian;
    isLittleEndian=false;
    UUID g=new UUID(rj(),rj());
    isLittleEndian=oa;
    return g;
  }
  /**
   * Write uuid to serialization buffer in big endian format
   * @param uuid UUID to serialize
   */
  void w(UUID uuid){
    if(ipcVersion<3)
      throw new RuntimeException("Guid not valid pre kdb+3.0");
    w(uuid.getMostSignificantBits());
    w(uuid.getLeastSignificantBits());
  }
  /**
   * Deserialize long from byte buffer
   * @return Deserialized long
   */
  long rj(){
    int x=ri();
    int y=ri();
    return isLittleEndian?x&0xffffffffL|(long)y<<32:(long)x<<32|y&0xffffffffL;
  }
  /**
   * Write long to serialization buffer in big endian format
   * @param j long to serialize
   */
  void w(long j){
    w((int)(j>>32));
    w((int)j);
  }
  /**
   * Deserialize float from byte buffer
   * @return Deserialized float
   */
  float re(){
    return Float.intBitsToFloat(ri());
  }
  /**
   * Write float to serialization buffer in big endian format
   * @param e float to serialize
   */
  void w(float e){
    w(Float.floatToIntBits(e));
  }
  /**
   * Deserialize double from byte buffer
   * @return Deserialized double
   */
  double rf(){
    return Double.longBitsToDouble(rj());
  }
  /**
   * Write double to serialization buffer in big endian format
   * @param f double to serialize
   */
  void w(double f){
    w(Double.doubleToLongBits(f));
  }
  /**
   * Deserialize Month from byte buffer
   * @return Deserialized Month
   */
  Month rm(){
    return new Month(ri());
  }
  /**
   * Write Month to serialization buffer in big endian format
   * @param m Month to serialize
   */
  void w(Month m){
    w(m.i);
  }
  /**
   * Deserialize Minute from byte buffer
   * @return Deserialized Minute
   */
  Minute ru(){
    return new Minute(ri());
  }
  /**
   * Write Minute to serialization buffer in big endian format
   * @param u Minute to serialize
   */
  void w(Minute u){
    w(u.i);
  }
  /**
   * Deserialize Second from byte buffer
   * @return Deserialized Second
   */
  Second rv(){
    return new Second(ri());
  }
  /**
   * Write Second to serialization buffer in big endian format
   * @param v Second to serialize
   */
  void w(Second v){
    w(v.i);
  }
  /**
   * Deserialize Timespan from byte buffer
   * @return Deserialized Timespan
   */
  Timespan rn(){
    return new Timespan(rj());
  }
  /**
   * Write Timespan to serialization buffer in big endian format
   * @param n Timespan to serialize
   */
  void w(Timespan n){
    if(ipcVersion<1)
      throw new RuntimeException("Timespan not valid pre kdb+2.6");
    w(n.j);
  }

  static final int DAYS_BETWEEN_1970_2000 = 10957;
  static final long MILLS_IN_DAY = 86400000L;
  static final long MILLS_BETWEEN_1970_2000=MILLS_IN_DAY*DAYS_BETWEEN_1970_2000;
  static final long NANOS_IN_SEC=1000000000L;

  /**
   * Deserialize date from byte buffer
   * @return Deserialized date
   */
  LocalDate rd(){
    int dateAsInt=ri();
    return (dateAsInt==ni?LocalDate.MIN:LocalDate.ofEpochDay(10957L+dateAsInt));
  }
  /**
   * Write LocalDate to serialization buffer in big endian format
   * @param d Date to serialize
   */
  void w(LocalDate d){
    if (d==LocalDate.MIN){
      w(ni);
      return;
    }
    long daysSince2000=d.toEpochDay()-DAYS_BETWEEN_1970_2000;
    if (daysSince2000<Integer.MIN_VALUE||daysSince2000>Integer.MAX_VALUE)
      throw new RuntimeException("LocalDate epoch day since 2000 must be >= Integer.MIN_VALUE and <= Integer.MAX_VALUE");
    w((int)(daysSince2000));
  }
  /**
   * Deserialize time from byte buffer
   * @return Deserialized time
   */
  LocalTime rt(){
    int timeAsInt=ri();
     return (timeAsInt==ni?LOCAL_TIME_NULL:LocalDateTime.ofInstant(Instant.ofEpochMilli(timeAsInt),ZoneId.of("UTC")).toLocalTime());
  }
  private static long toEpochSecond(LocalTime t,LocalDate d,ZoneOffset o){
    long epochDay=d.toEpochDay();
    long secs=epochDay*86400+t.toSecondOfDay();
    secs-=o.getTotalSeconds();
    return secs;
  }
  /**
   * Write LocalTime to serialization buffer in big endian format
   * @param t Time to serialize
   */
  void w(LocalTime t){
     w((t==LOCAL_TIME_NULL)?ni:(int)((toEpochSecond(t,LocalDate.of(1970,1,1),ZoneOffset.UTC)*1000+t.getNano()/1000000)%MILLS_IN_DAY));
  }
  /**
   * Deserialize LocalDateTime from byte buffer
   * @return Deserialized date
   */
  LocalDateTime rz(){
    double f=rf();
    if(Double.isNaN(f))
      return LocalDateTime.MIN;
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(MILLS_BETWEEN_1970_2000+Math.round(8.64e7*f)), ZoneId.of("UTC"));
  }
  /**
   * Write Date to serialization buffer in big endian format (only millisecond support)
   * @param z Date to serialize
   */
  void w(LocalDateTime z){
    w(z==LocalDateTime.MIN?nf:(z.toInstant(ZoneOffset.UTC).toEpochMilli()-MILLS_BETWEEN_1970_2000)/8.64e7);
  }
  /**
   * Deserialize Instant from byte buffer
   * @return Deserialized timestamp
   */
  Instant rp(){
    long timeAsLong=rj();
    if(timeAsLong==nj)
      return Instant.MIN;
    long d=timeAsLong<0?(timeAsLong+1)/NANOS_IN_SEC-1:timeAsLong/NANOS_IN_SEC;
    return Instant.ofEpochMilli(MILLS_BETWEEN_1970_2000+1000*d).plusNanos((int)(timeAsLong-NANOS_IN_SEC*d));
  }
  /**
   * Write Instant to serialization buffer in big endian format
   * @param p Instant to serialize
   */
  void w(Instant p){
    if(ipcVersion<1)
      throw new RuntimeException("Instant not valid pre kdb+2.6");
    w(p==Instant.MIN?nj:1000000*(p.toEpochMilli()-MILLS_BETWEEN_1970_2000)+p.getNano()%1000000); 
  }
  /**
   * Deserialize string from byte buffer
   * @return Deserialized string using registered encoding
   * @throws UnsupportedEncodingException If there is an issue with the registed encoding
   */
  String rs() throws UnsupportedEncodingException{
    int startPos=rBuffPos;
    while(rBuff[rBuffPos++]!=0);
    return (startPos==rBuffPos-1)?"":new String(rBuff,startPos,rBuffPos-1-startPos,encoding);
  }
  /**
   * Write String to serialization buffer
   * @param s String to serialize
   * @throws UnsupportedEncodingException If there is an issue with the registed encoding
   */
  void w(String s) throws UnsupportedEncodingException{
    if(s!=null){
      int byteLen=ns(s);
      byte[] bytes=s.getBytes(encoding);
      for(int idx=0;idx<byteLen;idx++)
        w(bytes[idx]);
    }
    wBuff[wBuffPos++]=0;
  }
  /** 
   * Deserializes the contents of the incoming message buffer {@code b}. 
   * @return deserialised object
   * @throws UnsupportedEncodingException If the named charset is not supported
   */
  Object r() throws UnsupportedEncodingException{
    int i=0;
    int n;
    int t=rBuff[rBuffPos++];
    if(t<0)
      switch(t){
        case -1:
          return rb();
        case (-2):
          return rg();
        case -4:
          return rBuff[rBuffPos++];
        case -5:
          return rh();
        case -6:
          return ri();
        case -7:
          return rj();
        case -8:
          return re();
        case -9:
          return rf();
        case -10:
          return rc();
        case -11:
          return rs();
        case -12:
          return rp();
        case -13:
          return rm();
        case -14:
          return rd();
        case -15:
          return rz();
        case -16:
          return rn();
        case -17:
          return ru();
        case -18:
          return rv();
        case -19:
          return rt();
      }
    if(t>99){
      if(t==100){
        rs();
        return r();
      }

		// Ryan - custom handling to show where native functions appear
		// this may change but seems consistent between kdb versions
		if(t==102 || t==101) {
			int p = rBuff[rBuffPos];
			char[][] funcs = t==101 ? FUNC_1 : FUNC_2;
			if(p<funcs.length && p>=0) {
				rBuffPos++;
				return funcs[p];
			}
		} else if(t>=106 && t<=111) {
			String adverb = ADVERBS[t-106];
			Object def = r();
			if(def instanceof char[]) {
				return (new String((char[])def)+adverb).toCharArray();
			}
			return null;
		}      
		// for projections return Object list with the parts. Allows drilling into nested table.
		if(t==104) {
			int nn = ri();
			Object[] oo = new Object[nn];
			for (; i < nn; i++) {
				Object o = r();
					oo[i] = o;
			}
			return oo;
		}
		
		// handle empty arguments of a projection
		if(t==101 && rBuff[rBuffPos]==-1) {
			rBuffPos++;
			return "";
		}		

	  // type 127 is a sorted keyed table e.g. `s#([a:0 1 2h] num:9 8 7h)
	  if(t == 127) {
		return new Dict(r(), r());
	  }
      if(t<104)
        return rBuff[rBuffPos++]==0&&t==101?null:"func";
      if(t>105)
        r();
      else
        for(n=ri();i<n;i++)
          r();
      return "func";
    }
    if(t==99)
      return new Dict(r(),r());
    rBuffPos++;
    if(t==98)
      return new Flip((Dict)r());
    n=ri();
    switch(t){
      case 0:
        Object[] objArr=new Object[n];
        for(;i<n;i++)
          objArr[i]=r();
        return objArr;
      case 1:
        boolean[] boolArr=new boolean[n];
        for(;i<n;i++)
          boolArr[i]=rb();
        return boolArr;
      case 2:
        UUID[] uuidArr=new UUID[n];
        for(;i<n;i++)
          uuidArr[i]=rg();
        return uuidArr;
      case 4:
        byte[] byteArr=new byte[n];
        for(;i<n;i++)
          byteArr[i]=rBuff[rBuffPos++];
        return byteArr;
      case 5:
        short[] shortArr=new short[n];
        for(;i<n;i++)
          shortArr[i]=rh();
        return shortArr;
      case 6:
        int[] intArr=new int[n];
        for(;i<n;i++)
          intArr[i]=ri();
        return intArr;
      case 7:
        long[] longArr=new long[n];
        for(;i<n;i++)
          longArr[i]=rj();
        return longArr;
      case 8:
        float[] floatArr=new float[n];
        for(;i<n;i++)
          floatArr[i]=re();
        return floatArr;
      case 9:
        double[] doubleArr=new double[n];
        for(;i<n;i++)
          doubleArr[i]=rf();
        return doubleArr;
      case 10:
        char[] charArr=new String(rBuff,rBuffPos,n,encoding).toCharArray();
        rBuffPos+=n;
        return charArr;
      case 11:
        String[] stringArr=new String[n];
        for(;i<n;i++)
          stringArr[i]=rs();
        return stringArr;
      case 12:
        Instant[] timestampArr=new Instant[n];
        for(;i<n;i++)
          timestampArr[i]=rp();
        return timestampArr;
      case 13:
        Month[] monthArr=new Month[n];
        for(;i<n;i++)
          monthArr[i]=rm();
        return monthArr;
      case 14:
        LocalDate[] dateArr=new LocalDate[n];
        for(;i<n;i++)
          dateArr[i]=rd();
        return dateArr;
      case 15:
        LocalDateTime[] dateUtilArr=new LocalDateTime[n];
        for(;i<n;i++)
          dateUtilArr[i]=rz();
        return dateUtilArr;
      case 16:
        Timespan[] timespanArr=new Timespan[n];
        for(;i<n;i++)
          timespanArr[i]=rn();
        return timespanArr;
      case 17:
        Minute[] minArr=new Minute[n];
        for(;i<n;i++)
          minArr[i]=ru();
        return minArr;
      case 18:
        Second[] secArr=new Second[n];
        for(;i<n;i++)
          secArr[i]=rv();
        return secArr;
      case 19:
        LocalTime[] timeArr=new LocalTime[n];
        for(;i<n;i++)
          timeArr[i]=rt();
        return timeArr;
      default:
        // do nothing, let it return null
    }
    return null;
  }

//object.getClass().isArray()   t(int[]) is .5 isarray is .1 lookup .05
  /**
   * Gets the numeric type of the supplied object used in kdb+ (distict supported data types in KDB+ can be identified by a numeric).&nbsp;
   * See data type reference <a href="https://code.kx.com/q/basics/datatypes/">https://code.kx.com/q/basics/datatypes/</a>.
   * For example, an object of type java.lang.Integer provides a numeric type of -6.
   * @param x Object to get the numeric type of
   * @return kdb+ type number for an object
   */
  public static int t(final Object x){
    if (x instanceof Boolean)
      return -1;
    if (x instanceof UUID)
      return -2;
    if (x instanceof Byte)
      return -4;
    if (x instanceof Short)
      return -5;
    if (x instanceof Integer)
      return -6;
    if (x instanceof Long)
      return -7;
    if (x instanceof Float)
      return -8;
    if (x instanceof Double)
      return -9;
    if (x instanceof Character)
      return -10;
    if (x instanceof String)
      return -11;
    if (x instanceof LocalDate)
      return -14;
    if (x instanceof LocalTime)
      return -19;
    if (x instanceof Instant)
      return -12;
    if (x instanceof LocalDateTime)
      return -15;
    if (x instanceof Timespan)
      return -16;
    if (x instanceof Month)
      return -13;
    if (x instanceof Minute)
      return -17;
    if (x instanceof Second)
      return -18;
    if (x instanceof boolean[])
      return 1;
    if (x instanceof UUID[])
      return 2;
    if (x instanceof byte[])
      return 4;
    if (x instanceof short[])
      return 5;
    if (x instanceof int[])
      return 6;
    if (x instanceof long[])
      return 7;
    if (x instanceof float[])
      return 8;
    if (x instanceof double[])
      return 9;
    if (x instanceof char[])
      return 10;
    if (x instanceof String[])
      return 11;
    if (x instanceof LocalDate[])
      return 14;
    if (x instanceof LocalTime[])
      return 19;
    if (x instanceof Instant[])
      return 12;
    if (x instanceof LocalDateTime[])
      return 15;
    if (x instanceof Timespan[])
      return 16;
    if (x instanceof Month[])
      return 13;
    if (x instanceof Minute[])
      return 17;
    if (x instanceof Second[])
      return 18;
    if (x instanceof Flip)
      return 98;
    if (x instanceof Dict)
      return 99;
    return 0;
  }
  /**
   * "number of bytes from type." A helper for nx, to assist in calculating the number of bytes required to serialize a
   * particular type.
   */
  static int[] nt={0,1,16,0,1,2,4,8,4,8,1,0,8,4,4,8,8,4,4,4};
  /**
   * A helper function for nx, calculates the number of bytes which would be required to serialize the supplied string.
   * @param s String to be serialized
   * @return number of bytes required to serialise a string
   * @throws UnsupportedEncodingException  If the named charset is not supported
   */
  static int ns(String s) throws UnsupportedEncodingException{
    int i;
    if(s==null)
      return 0;
    if(-1<(i=s.indexOf('\000')))
      s=s.substring(0,i);
    return s.getBytes(encoding).length;
  }
  /**
   * A helper function used by nx which returns the number of elements in the supplied object
   * (for example: the number of keys in a Dict, the number of rows in a Flip, 
   * the length of the array if its an array type)
   * @param x Object to be serialized
   * @return number of elements in an object.
   * @throws UnsupportedEncodingException  If the named charset is not supported
   */
  public static int n(final Object x) throws UnsupportedEncodingException{
    if (x instanceof Dict)
      return n(((Dict)x).x);
    if (x instanceof Flip)
      return n(((Flip)x).y[0]);
    return x instanceof char[]?new String((char[])x).getBytes(encoding).length:Array.getLength(x);
  }
  /**
   * Calculates the number of bytes which would be required to serialize the supplied object.
   * @param x Object to be serialized
   * @return number of bytes required to serialise an object.
   * @throws UnsupportedEncodingException  If the named charset is not supported
   */
  public int nx(Object x) throws UnsupportedEncodingException{
    int type=t(x);
    if(type==99)
      return 1+nx(((Dict)x).x)+nx(((Dict)x).y);
    if(type==98)
      return 3+nx(((Flip)x).x)+nx(((Flip)x).y);
    if(type<0)
      return type==-11?2+ns((String)x):1+nt[-type];
    int numBytes=6;
    int numElements=n(x);
    if(type==0||type==11)
      for(int idx=0;idx<numElements;++idx)
        numBytes+=type==0?nx(((Object[])x)[idx]):1+ns(((String[])x)[idx]);
    else
      numBytes+=numElements*nt[type];
    return numBytes;
  }
  /**
   * Serialize object in big endian format
   * @param x Object to serialize
   * @throws UnsupportedEncodingException If the named charset (encoding) is not supported
   */
  void w(Object x) throws UnsupportedEncodingException{
    int i=0;
    int n;
    int type=t(x);
    w((byte)type);
    if(type<0)
      switch(type){
        case -1:
          w(((Boolean)x).booleanValue());
          return;
        case -2:
          w((UUID)x);
          return;
        case -4:
          w(((Byte)x).byteValue());
          return;
        case -5:
          w(((Short)x).shortValue());
          return;
        case -6:
          w(((Integer)x).intValue());
          return;
        case -7:
          w(((Long)x).longValue());
          return;
        case -8:
          w(((Float)x).floatValue());
          return;
        case -9:
          w(((Double)x).doubleValue());
          return;
        case -10:
          w(((Character)x).charValue());
          return;
        case -11:
          w((String)x);
          return;
        case -12:
          w((Instant)x);
          return;
        case -13:
          w((Month)x);
          return;
        case -14:
          w((LocalDate)x);
          return;
        case -15:
          w((LocalDateTime)x);
          return;
        case -16:
          w((Timespan)x);
          return;
        case -17:
          w((Minute)x);
          return;
        case -18:
          w((Second)x);
          return;
        case -19:
          w((LocalTime)x);
          return;
      }
    if(type==99){
      Dict r=(Dict)x;
      w(r.x);
      w(r.y);
      return;
    }
    wBuff[wBuffPos++]=0;
    if(type==98){
      Flip r=(Flip)x;
      wBuff[wBuffPos++]=99;
      w(r.x);
      w(r.y);
      return;
    }
    n=n(x);
    w(n);
    if(type==10){
      byte[] b=new String((char[])x).getBytes(encoding);
      while(i<b.length)
        w(b[i++]);
    }else
      for(;i<n;++i)
        if(type==0)
          w(((Object[])x)[i]);
        else if(type==1)
          w(((boolean[])x)[i]);
        else if(type==2)
          w(((UUID[])x)[i]);
        else if(type==4)
          w(((byte[])x)[i]);
        else if(type==5)
          w(((short[])x)[i]);
        else if(type==6)
          w(((int[])x)[i]);
        else if(type==7)
          w(((long[])x)[i]);
        else if(type==8)
          w(((float[])x)[i]);
        else if(type==9)
          w(((double[])x)[i]);
        else if(type==11)
          w(((String[])x)[i]);
        else if(type==12)
          w(((Instant[])x)[i]);
        else if(type==13)
          w(((Month[])x)[i]);
        else if(type==14)
          w(((LocalDate[])x)[i]);
        else if(type==15)
          w(((LocalDateTime[])x)[i]);
        else if(type==16)
          w(((Timespan[])x)[i]);
        else if(type==17)
          w(((Minute[])x)[i]);
        else if(type==18)
          w(((Second[])x)[i]);
        else
          w(((LocalTime[])x)[i]);
  }

  /**
   * Serialises {@code x} object as {@code byte[]} array.
   * @param msgType type of the ipc message (0 – async, 1 – sync, 2 – response)
   * @param x object to serialise
   * @param zip true if to attempt compress serialised output (given uncompressed serialized data also has a length 
   * greater than 2000 bytes and connection is not localhost)
   * @return {@code wBuff} containing serialised representation
   * 
   * @throws IOException should not throw
   */
  public byte[] serialize(int msgType,Object x,boolean zip)throws IOException{
    int length=8+nx(x);
    synchronized(outStream){
      wBuff=new byte[length];
      wBuff[0]=0;
      wBuff[1]=(byte)msgType;
      wBuffPos=4;
      w(length);
      w(x);
      if(zip&&wBuffPos>2000&&!isLoopback)
        compress();
      return wBuff;
    }
  }

  /**
   * Deserialises {@code buffer} q ipc as an object
   * @param buffer byte[] to deserialise object from 
   * @return deserialised object
   * @throws KException if buffer contains kdb+ error object.
   * @throws UnsupportedEncodingException  If the named charset is not supported
   */
  public Object deserialize(byte[]buffer)throws KException, UnsupportedEncodingException{
    synchronized(inStream){
      rBuff=buffer;
      isLittleEndian=rBuff[0]==1;  // endianness of the msg 
      boolean compressed=rBuff[2]==1;
      rBuffPos=8;      
      if(compressed)
        uncompress();
      if(rBuff[8]==-128){
        rBuffPos=9;
        throw new KException(rs());
      }
      return r(); // deserialize the message
    }    
  }

  /**
   * Serialize and write the data to the registered connection
   * @param msgType The message type to use within the message (0 – async, 1 – sync, 2 – response)
   * @param x The contents of the message
   * @throws IOException due to an issue serializing/sending the provided data
   */
  protected void w(int msgType,Object x) throws IOException{
    synchronized(outStream){
      byte[] buffer=serialize(msgType,x,zip);
      outStream.write(buffer,0,buffer.length);
    }
  }
  /**
   * Sends a response message to the remote kdb+ process. This should be called only during processing of an incoming sync message.
   * @param obj Object to send to the remote
   * @throws IOException if not expecting any response
   */
  public void kr(Object obj) throws IOException{
    if(sync==0)
      throw new IOException("Unexpected response msg");
    sync--;
    w(2,obj);
  }
  /**
   * Sends an error as a response message to the remote kdb+ process. This should be called only during processing of an incoming sync message.
   * @param text The error message text
   * @throws IOException unexpected error message
   */
  public void ke(String text) throws IOException{
    if(sync==0)
      throw new IOException("Unexpected error msg");
    sync--;
    int n=2+ns(text)+8;
    synchronized(outStream){
      wBuff=new byte[n];
      wBuff[0]=0;
      wBuff[1]=2;
      wBuffPos=4;
      w(n);
      w((byte)-128);
      w(text);
      outStream.write(wBuff);
    }
  }
  /**
   * Sends an async message to the remote kdb+ process. This blocks until the serialized data has been written to the
   * socket. On return, there is no guarantee that this msg has already been processed by the remote process.
   * @param expr The expression to send
   * @throws IOException if an I/O error occurs.
   */
  public void ks(String expr) throws IOException{
    w(0,expr.toCharArray());
  }
  /**
   * Sends an async message to the remote kdb+ process. This blocks until the serialized data has been written to the
   * socket. On return, there is no guarantee that this msg has already been processed by the remote process.
   * @param obj The object to send
   * @throws IOException if an I/O error occurs.
   */
  public void ks(Object obj) throws IOException{
    w(0,obj);
  }
  /**
   * Sends an async message to the remote kdb+ process. This blocks until the serialized data has been written to the
   * socket. On return, there is no guarantee that this msg has already been processed by the remote process. Use this to
   * invoke a function in kdb+ which takes a single argument and does not return a value. e.g. to invoke f[x] use
   * ks("f",x); to invoke a lambda, use ks("{x}",x);
   * @param s The name of the function, or a lambda itself
   * @param x The argument to the function named in s
   * @throws IOException if an I/O error occurs.
   */
  public void ks(String s,Object x) throws IOException{
    Object[] a={s.toCharArray(),x};
    w(0,a);
  }
  /**
   * Sends an async message to the remote kdb+ process. This blocks until the serialized data has been written to the
   * socket. On return, there is no guarantee that this msg has already been processed by the remote process. Use this to
   * invoke a function in kdb+ which takes 2 arguments and does not return a value. e.g. to invoke f[x;y] use ks("f",x,y);
   * to invoke a lambda, use ks("{x+y}",x,y);
   * @param s The name of the function, or a lambda itself
   * @param x The first argument to the function named in s
   * @param y The second argument to the function named in s
   * @throws IOException if an I/O error occurs.
   */
  public void ks(String s,Object x,Object y) throws IOException{
    Object[] a={s.toCharArray(),x,y};
    w(0,a);
  }
  /**
   * Sends an async message to the remote kdb+ process. This blocks until the serialized data has been written to the
   * socket. On return, there is no guarantee that this msg has already been processed by the remote process. Use this to
   * invoke a function in kdb+ which takes 3 arguments and does not return a value. e.g. to invoke f[x;y;z] use
   * ks("f",x,y,z); to invoke a lambda, use ks("{x+y+z}",x,y,z);
   * @param s The name of the function, or a lambda itself
   * @param x The first argument to the function named in s
   * @param y The second argument to the function named in s
   * @param z The third argument to the function named in s
   * @throws IOException if an I/O error occurs.
   */
  public void ks(String s,Object x,Object y,Object z) throws IOException{
    Object[] a={s.toCharArray(),x,y,z};
    w(0,a);
  }
  /**
   * Reads an incoming message from the remote kdb+ process. This blocks until a single message has been received and
   * deserialized. This is called automatically during a sync request via k(String s,..). It can be called explicitly when
   * subscribing to a publisher.
   * @return an Object array of {messageType,deserialised object}
   * @throws KException if response contains an error
   * @throws IOException if an I/O error occurs.
   * @throws UnsupportedEncodingException If the named charset is not supported
   */
  public Object[] readMsg() throws KException,IOException,UnsupportedEncodingException{
    synchronized(inStream){
      rBuff=new byte[8];
      inStream.readFully(rBuff); // read the msg header
      isLittleEndian=rBuff[0]==1;  // endianness of the msg
      if(rBuff[1]==1) // msg types are 0 - async, 1 - sync, 2 - response
        sync++;   // an incoming sync message means the remote will expect a response message
      rBuffPos=4;
      rBuff=Arrays.copyOf(rBuff,ri());
      inStream.readFully(rBuff,8,rBuff.length-8); // read the incoming message in full
      return new Object[]{rBuff[1],deserialize(rBuff)};
    }
  }
  /**
   * Reads an incoming message from the remote kdb+ process. This blocks until a single message has been received and
   * deserialized. This is called automatically during a sync request via k(String s,..). It can be called explicitly when
   * subscribing to a publisher.
   * @return the deserialised object
   * @throws KException if response contains an error
   * @throws IOException if an I/O error occurs.
   * @throws UnsupportedEncodingException If the named charset is not supported
   */
  public Object k() throws KException,IOException,UnsupportedEncodingException{
    return readMsg()[1];
  }
  /**
   * MsgHandler interface for processing async or sync messages during a sync request whilst awaiting a response message
   * which contains a default implementation
   */
  public interface MsgHandler{
    /**
     * The default implementation discards async messages, responds to sync messages with an error, 
     * otherwise the remote will continue to wait for a response
     * @param c The c object that received the message
     * @param msgType The type of the message received (0 – async, 1 – sync, 2 – response)
     * @param msg The message contents
     * @throws IOException Thrown when message type is unexpected (i.e isnt a sync or async message)
     */
    default void processMsg(c c,byte msgType,Object msg)throws IOException{
      switch(msgType){
        case 0:System.err.println("discarded unexpected incoming async msg!");break; // implicitly discard incoming async messages
        case 1:c.ke("unable to process sync requests");break; // signal to remote that we're unable to process these by default
        default:throw new IOException("Invalid message type received: "+msgType);
      }
    }
  }
  /**
   * {@code msgHandler} is used for handling incoming async and sync messages whilst awaiting a response message to a sync request
   */
  private MsgHandler msgHandler=null;
  /**
   * Stores the handler in an instance variable
   * @param handler The handler to store
   */
  public void setMsgHandler(MsgHandler handler){
    msgHandler=handler;
  }
  /**
   * Returns the current msg handler
   * @return the current msg handler
   */
  public MsgHandler getMsgHandler(){
    return msgHandler;
  }
  /**
   * {@code collectResponseAsync} is used to indicate whether k() should leave the reading of the associated response message to the caller
   * via readMsg();
   */
  private boolean collectResponseAsync;
  /**
   * Stores the boolean in an instance variable
   * @param b The boolean to store
   */
  public void setCollectResponseAsync(boolean b){collectResponseAsync=b;}

  /**
   * Sends a sync message to the remote kdb+ process. This blocks until the message has been sent in full, and, if a MsgHandler
   * is set, will process any queued, incoming async or sync message in order to reach the response message.
   * If the caller has already indicated via {@code setCollectResponseAsync} that the response message will be read async, later, then return
   * without trying to read any messages at this point; the caller can collect(read) the response message by calling readMsg();
   * @param x The object to send
   * @return deserialised response to request {@code x}
   * @throws KException if request evaluation resulted in an error
   * @throws IOException if an I/O error occurs.
   */
  public synchronized Object k(Object x) throws KException,IOException{
    w(1,x);
    if(collectResponseAsync)
      return null;
    while(true){
      Object[]msg=readMsg();
      if(msgHandler==null||(byte)msg[0]==(byte)2) // if there's no handler or the msg is a response msg, return it
        return msg[1];
      msgHandler.processMsg(this,(byte)msg[0],msg[1]); // process async and sync requests
    }
  }
  /**
   * Sends a sync message to the remote kdb+ process. This blocks until the message has been sent in full, and a message
   * is received from the remote; typically the received message would be the corresponding response message.
   * @param expr The expression to send
   * @return deserialised response to request {@code x}
   * @throws KException if request evaluation resulted in an error
   * @throws IOException if an I/O error occurs.
   */
  public Object k(String expr) throws KException,IOException{
    return k(expr.toCharArray());
  }
  /**
   * Sends a sync message to the remote kdb+ process. This blocks until the message has been sent in full, and a message
   * is received from the remote; typically the received message would be the corresponding response message. Use this to
   * invoke a function in kdb+ which takes a single argument and returns a value. e.g. to invoke f[x] use k("f",x); to
   * invoke a lambda, use k("{x}",x);
   * @param s The name of the function, or a lambda itself
   * @param x The argument to the function named in s   
   * @return deserialised response to request {@code s} with params {@code x}
   * @throws KException if request evaluation resulted in an error
   * @throws IOException if an I/O error occurs.
   */
  public Object k(String s,Object x) throws KException,IOException{
    Object[] a={s.toCharArray(),x};
    return k(a);
  }
  /**
   * Sends a sync message to the remote kdb+ process. This blocks until the message has been sent in full, and a message
   * is received from the remote; typically the received message would be the corresponding response message. Use this to
   * invoke a function in kdb+ which takes arguments and returns a value. e.g. to invoke f[x;y] use k("f",x,y); to invoke
   * a lambda, use k("{x+y}",x,y);
   * @param s The name of the function, or a lambda itself
   * @param x The first argument to the function named in s
   * @param y The second argument to the function named in s
   * @return deserialised response to the request
   * @throws KException if request evaluation resulted in an error
   * @throws IOException if an I/O error occurs.
   */
  public Object k(String s,Object x,Object y) throws KException,IOException{
    Object[] a={s.toCharArray(),x,y};
    return k(a);
  }
  /**
   * Sends a sync message to the remote kdb+ process. This blocks until the message has been sent in full, and a message
   * is received from the remote; typically the received message would be the corresponding response message. Use this to
   * invoke a function in kdb+ which takes 3 arguments and returns a value. e.g. to invoke f[x;y;z] use k("f",x,y,z); to
   * invoke a lambda, use k("{x+y+z}",x,y,z);
   * @param s The name of the function, or a lambda itself
   * @param x The first argument to the function named in s
   * @param y The second argument to the function named in s
   * @param z The third argument to the function named in s
   * @return deserialised response to the request
   * @throws KException if request evaluation resulted in an error
   * @throws IOException if an I/O error occurs.
   */
  public Object k(String s,Object x,Object y,Object z) throws KException,IOException{
    Object[] a={s.toCharArray(),x,y,z};
    return k(a);
  }
  /** 
   * Array containing the null object representation for corresponing kdb+ type number (0-19).&nbsp;
   * See data type reference <a href="https://code.kx.com/q/basics/datatypes/">https://code.kx.com/q/basics/datatypes/</a> 
   * For example {@code "".equals(NULL[11])} 
   */
  public static final Object[] NULL={null,Boolean.valueOf(false),new UUID(0,0),null,Byte.valueOf((byte)0),Short.valueOf(Short.MIN_VALUE),Integer.valueOf(ni),Long.valueOf(nj),Float.valueOf((float)nf),Double.valueOf(nf),Character.valueOf(' '),"",
    Instant.MIN,new Month(ni),LocalDate.MIN,LocalDateTime.MIN,new Timespan(nj),new Minute(ni),new Second(ni),LOCAL_TIME_NULL
  };
  /**
   * Gets a null object for the type indicated by the character.&nbsp;
   * See data type reference <a href="https://code.kx.com/q/basics/datatypes/">https://code.kx.com/q/basics/datatypes/</a> 
   * @param c The shorthand character for the type
   * @return instance of null object of specified kdb+ type.
   */
  public static Object NULL(char c){
    return NULL[" bg xhijefcspmdznuvt".indexOf(c)];
  }
  /**
   * Tests whether an object represents a KDB+ null for its type, for example 
   * qn(NULL('j')) should return true
   * @param x The object to be tested for null
   * @return true if {@code x} is kdb+ null, false otherwise
   */
  public static boolean qn(Object x){
    int t=-t(x);
    return (t==2||t>4)&&x.equals(NULL[t]);
  }
  /**
   * Gets the object at an index of a given array, if its a valid type used by the KDB+ interface.
   * @param x The array to index
   * @param i The offset to index at
   * @return object at index, or null if the object value represents
   * a KDB+ null value for its type 
   */
  public static Object at(Object x,int i){
    x=Array.get(x,i);
    return qn(x)?null:x;
  }
  /**
   * Sets the object at an index of an array.
   * @param x The array to index
   * @param i The offset to index at
   * @param y The object to set at index i. null can be used if you wish
   * to set the KDB+ null representation of the type (e.g. null would populate
   * an empty string if x was an array of Strings)
   */
  public static void set(Object x,int i,Object y){
    Array.set(x,i,null==y?NULL[t(x)]:y);
  } 
  /**
   * Finds index of string in an array
   * @param x String array to search
   * @param y The String to locate in the array
   * @return The index at which the String resides
   */
  static int find(String[] x,String y){
    int i=0;
    while(i<x.length&&!x[i].equals(y))
      ++i;
    return i;
  }
  /**
   * Removes the key from a keyed table. 
   * <p>
   * A keyed table(a.k.a. Flip) is a dictionary where both key and value are tables
   * themselves. For ease of processing, this method, td, table from dictionary, can be used to remove the key.
   * </p>
   * @param tbl A table or keyed table.
   * @return A simple table
   * @throws UnsupportedEncodingException If the named charset is not supported
   */
  public static Flip td(Object tbl) throws UnsupportedEncodingException{
    if(tbl instanceof Flip)
      return (Flip)tbl;
    Dict d=(Dict)tbl;
    Flip a=(Flip)d.x;
    Flip b=(Flip)d.y;
    int m=n(a.x);
    int n=n(b.x);
    String[] x=new String[m+n];
    System.arraycopy(a.x,0,x,0,m);
    System.arraycopy(b.x,0,x,m,n);
    Object[] y=new Object[m+n];
    System.arraycopy(a.y,0,y,0,m);
    System.arraycopy(b.y,0,y,m,n);
    return new Flip(new Dict(x,y));
  }
  /**
   * Creates a string from int with left padding of 0s, if less than 2 digits
   * @param i Integer to convert to string
   * @return String representation of int with zero padding
   */
  static String i2(int i){
    return new DecimalFormat("00").format(i);
  }
  /**
   * Creates a string from int with left padding of 0s, if less than 9 digits
   * @param i Integer to convert to string
   * @return String representation of int with zero padding
   */
  static String i9(int i){
    return new DecimalFormat("000000000").format(i);
  }
  
}