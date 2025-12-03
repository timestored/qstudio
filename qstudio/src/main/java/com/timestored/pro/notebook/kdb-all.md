# Welcome to qStudio - SqlNotebooks

If you want help there are guides located at http://www.timestored.com/qstudio/help/
Any feature requests etc feel free to raise on [github](https://github.com/timeseries/qstudio/issues).

# Table

Table display can be configured using column names. See help->charts for details on format.

```sql type="grid" server="localhost:5000"
update percbar_SD_DATABAR:percent_SD_PERCENT0 ,bid_SD_FG:((`$("#FF6666";"#66FF66";""))!`$("#222";"#222";"")) bid_SD_BG from  
	 ([] time:.z.t-til 50; 
		 status:50?`partial`filled; 
		 instrument:50?`GBPUSD`USDNZD`USDCAD`CHFJPY`EURUSD;
		 symbol_SD_TAG:50?`UBS`C`MS`HSBC`NOMURA`DB;
		 price_SD_CURUSD:50?100.0;
		 bid:50?20.0;
		 bid_SD_BG:50?`$("#FF6666";"";"";"";"";"";"";"";"";"";"";"";"";"#66FF66");
		 bid_SD_CODE:50?("0.xXXx";"0.XXx";"0.xxXX");
		 percent_SD_PERCENT0:50?1.0 )
```

# Time-Series

```sql type="timeseries" server="localhost:5000"
{  walk:{ [seed;n]
	 r:{{ abs ((1664525*x)+1013904223) mod 4294967296}\[y-1;x]};
	 prds (100+((r[seed;n]) mod 11)-5)%100};
	 c:{x mod `long$00:20:00.0t}x;   st:x-c;   cn:`long$c%1000;
	 ([] time:.z.d+st+1000*til cn; gold:walk[100;cn]; bitcoin:walk[2;cn])  }[.z.t]
```

## Candlestick Chart


- The table should contain columns labelled open/high/low/close/volume.
- The table must atleast contain high/low or open/close to allow it to be drawn.


```sql type="candle" server="localhost:5000"
{  r:{{ abs ((1664525*x)+1013904223) mod 4294967296}\[y-1;x]};
	walk:{ [r;seed;n] prds (100+((r[seed;n]) mod 11)-5)%100}[r;;];
	c:{x mod `long$00:05:00.0t}x;   st:x-c;   cn:100+`long$c%1000;
	t:([] time:`second$.z.d+st+1000*til cn; open:walk[9;cn]; close:walk[105;cn]);
	-100 sublist update low:?[open > close;close;open]-(r[11;cn] mod 11)*0.02,high:?[open < close;close;open]+(r[44;cn] mod 11)*0.02,volume:(r[44;cn] mod 110) from t}[.z.t]
```

# Bar

- The first string columns are used as category labels.
- Whatever numeric columns appear after the strings represents a separate series in the chart.

```sql type="bar" server="localhost:5000"
([] Company:`Microsoft`Oracle`Paypal`Monero`FXC`Braint`MS`UBS; 
	  PnL:(0.8+rand[0.2])*31847.0 13239.0 127938.0 81308.0 63047.0 13010.0 152518.0 166629.0;
	  Revenue:(0.9+rand[0.1])*15080.0 11300.0 34444.0 3114.0 2228.0 88.9 1113.0 41196.0 ; 
	  Negatives:(0.95+rand[0.05])*48300.0 8400.0 34700.0 38100.0 36500.0 413.0 1788.0 11732.0 )
```

## Sankey

 - Assuming string columns named S1,S2,S3 with a numeric column of value V.
 - Each row represents one flow from the top level S1 to the leaf node S3. S1->S2->S3->V
 - The first numeric column reprents the size of the flow between nodes.
 - Sizes are back-propagated to the top level.
 - Null can be used to represent either gaps or allow assigning value to a node that is neither an inflow nor outflow.


```sql type="sankey" server="localhost:5000"
([] OrderOrigin:`Internal`GUI`Web`Platform`Internal`GUI`Web`Platform`Internal`GUI`Web`Platform;
	 Exchange:`ICE`ICE`ICE`NYSE`NYSE`NYSE`LDN`LDN`LDN`CBE`CBE`CBE;
	 State:12?`New`Partial`Filled`Filled`Filled`;
	 v:12?20)
```


# Bar Horizontal

- The first string columns are used as category labels.
- Whatever numeric columns appear after the strings represents a separate series in the chart.

```sql type="bar_horizontal" server="localhost:5000"
([] Company:`Microsoft`Oracle`Paypal`Monero`FXC`Braint`MS`UBS; 
	  PnL:(0.8+rand[0.2])*31847.0 13239.0 127938.0 81308.0 63047.0 13010.0 152518.0 166629.0;
	  Revenue:(0.9+rand[0.1])*15080.0 11300.0 34444.0 3114.0 2228.0 88.9 1113.0 41196.0 ; 
	  Negatives:(0.95+rand[0.05])*48300.0 8400.0 34700.0 38100.0 36500.0 413.0 1788.0 11732.0 )
```

# Stack

- The first string columns are used as category labels.
- Whatever numeric columns appear after the strings represents a separate series in the chart.

```sql type="stack" server="localhost:5000"
([] Company:`Microsoft`Oracle`Paypal`Monero`FXC`Braint`MS`UBS; 
	  PnL:(0.8+rand[0.2])*31847.0 13239.0 127938.0 81308.0 63047.0 13010.0 152518.0 166629.0;
	  Revenue:(0.9+rand[0.1])*15080.0 11300.0 34444.0 3114.0 2228.0 88.9 1113.0 41196.0 ; 
	  Negatives:(0.95+rand[0.05])*48300.0 8400.0 34700.0 38100.0 36500.0 413.0 1788.0 11732.0 )
```

# Bar Horizontal

```sql type="stack_horizontal" server="localhost:5000"
([] Company:`Microsoft`Oracle`Paypal`Monero`FXC`Braint`MS`UBS; 
	  PnL:(0.8+rand[0.2])*31847.0 13239.0 127938.0 81308.0 63047.0 13010.0 152518.0 166629.0;
	  Revenue:(0.9+rand[0.1])*15080.0 11300.0 34444.0 3114.0 2228.0 88.9 1113.0 41196.0 ; 
	  Negatives:(0.95+rand[0.05])*48300.0 8400.0 34700.0 38100.0 36500.0 413.0 1788.0 11732.0 )
```

# Line

- The first string columns are used as category labels.
- Whatever numeric columns appear after the strings represents a separate series in the chart.

```sql type="line" server="localhost:5000"
([] Company:`Microsoft`Oracle`Paypal`Monero`FXC`Braint`MS`UBS; 
	  PnL:(0.8+rand[0.2])*31847.0 13239.0 127938.0 81308.0 63047.0 13010.0 152518.0 166629.0;
	  Revenue:(0.9+rand[0.1])*15080.0 11300.0 34444.0 3114.0 2228.0 88.9 1113.0 41196.0 ; 
	  Negatives:(0.95+rand[0.05])*48300.0 8400.0 34700.0 38100.0 36500.0 413.0 1788.0 11732.0 )
```


# Area

- The first string columns are used as category labels.
- Whatever numeric columns appear after the strings represents a separate series in the chart.
    
```sql type="area" server="localhost:5000"
([] Company:`Microsoft`Oracle`Paypal`Monero`FXC`Braint`MS`UBS; 
	  PnL:(0.8+rand[0.2])*31847.0 13239.0 127938.0 81308.0 63047.0 13010.0 152518.0 166629.0;
	  Revenue:(0.9+rand[0.1])*15080.0 11300.0 34444.0 3114.0 2228.0 88.9 1113.0 41196.0 ; 
	  Negatives:(0.95+rand[0.05])*48300.0 8400.0 34700.0 38100.0 36500.0 413.0 1788.0 11732.0 )
```

# Pie

 - Each numeric column represents one pie chart. The title of each pie chart will be the column title.
 - The segments of the pie chart use the string columns as a title where possible. If there are no string columns, row numbers are used.

```sql type="pie" server="localhost:5000"
([] Company:`Microsoft`Oracle`Paypal`Monero`FXC`Braint`MS`UBS; 
	  PnL:(0.8+rand[0.2])*31847.0 13239.0 127938.0 81308.0 63047.0 13010.0 152518.0 166629.0;
	  Revenue:(0.9+rand[0.1])*15080.0 11300.0 34444.0 3114.0 2228.0 88.9 1113.0 41196.0 ; 
	  Negatives:(0.95+rand[0.05])*48300.0 8400.0 34700.0 38100.0 36500.0 413.0 1788.0 11732.0 )
```

# Hierarchical

## Tree

 - Starting from the left each string column is taken as one nesting level
 - The first numerical column will be taken as size.

```sql type="tree" server="localhost:5000"
([] Continent:`NA`Asia`Asia`Europe`Asia`Europe`Europe`SA`Europe`NA`Europe`Asia`Australia`Europe`NA;
	  TradingBloc:`US`China`Japan`EU`India`UK`EU`Brazil`EU`US`Russia`SouthKorea`Australia`EU`US; 
	  Country:`US`China`Japan`Germany`India`UK`France`Brazil`Italy`Canada`Russia`SouthKorea`Australia`Spain`Mexico; 
	  GDP:19.485 12.238 4.872 3.693 2.651 2.638 2.583 2.054 1.944 1.647 1.578  1.531 1.323 1.314 1.151 )
```

## Sunburst

 - Starting from the left each string column is taken as one nesting level
 - The first numerical column will be taken as size.


```sql type="sunburst" server="localhost:5000"
([] Continent:`NA`Asia`Asia`Europe`Asia`Europe`Europe`SA`Europe`NA`Europe`Asia`Australia`Europe`NA;
	  TradingBloc:`US`China`Japan`EU`India`UK`EU`Brazil`EU`US`Russia`SouthKorea`Australia`EU`US; 
	  Country:`US`China`Japan`Germany`India`UK`France`Brazil`Italy`Canada`Russia`SouthKorea`Australia`Spain`Mexico; 
	  GDP:19.485 12.238 4.872 3.693 2.651 2.638 2.583 2.054 1.944 1.647 1.578  1.531 1.323 1.314 1.151 )
```


## Scatter


 - Two or more numeric columns are required.
 - The values in the first column are used for the X-axis.
 - The values in following columns are used for the Y-axis. Each column is displayed with a separate color.


```sql type="scatter" server="localhost:5000"
update exports:(0.1+9?0.1)*GDP, exportsPerCapita:(0.4+9?0.1)*GDPperCapita from 
	  ([] Country:`US`France`japan`Germany`UK`Zimbabwe`Bangladesh`Nigeria`Vietnam; 
	  Population:(0.9+9?0.2)*313847.0 213847.0 127938.0 81308.0 63047.0 13010.0 152518.0 166629.0 87840.0 ;
	  GDP:(0.9+9?0.2)*15080.0 3333. 4444.0 3114.0 2228.0 9.9 113.0 196.0 104.0 ; 
	  GDPperCapita:(0.9+9?0.2)*0.001*48300.0 37000 34700.0 38100.0 36500.0 413.0 1788.0 732.0 3359.0)
```

## bubble

 - The first string columns are used as category labels.
 - There must then be 3 numeric columns which are used for x-coord, y-coord, size in that order.

```sql type="bubble" server="localhost:5000"
update exports:(0.1+9?0.1)*GDP, exportsPerCapita:(0.4+9?0.1)*GDPperCapita from 
	  ([] Country:`US`France`japan`Germany`UK`Zimbabwe`Bangladesh`Nigeria`Vietnam; 
	  Population:(0.9+9?0.2)*313847.0 213847.0 127938.0 81308.0 63047.0 13010.0 152518.0 166629.0 87840.0 ;
	  GDP:(0.9+9?0.2)*15080.0 3333. 4444.0 3114.0 2228.0 9.9 113.0 196.0 104.0 ; 
	  GDPperCapita:(0.9+9?0.2)*0.001*48300.0 37000 34700.0 38100.0 36500.0 413.0 1788.0 732.0 3359.0)
```

## Heatmap

 - Each numerical column in the table becomes one column in the chart.
 - The numerical values represent the shading within the chart.

```sql type="heatmap" server="localhost:5000"
update exports:(0.1+9?0.1)*GDP, exportsPerCapita:(0.4+9?0.1)*GDPperCapita from 
	  ([] Country:`US`France`japan`Germany`UK`Zimbabwe`Bangladesh`Nigeria`Vietnam; 
	  Population:(0.9+9?0.2)*313847.0 213847.0 127938.0 81308.0 63047.0 13010.0 152518.0 166629.0 87840.0 ;
	  GDP:(0.9+9?0.2)*15080.0 3333. 4444.0 3114.0 2228.0 9.9 113.0 196.0 104.0 ; 
	  GDPperCapita:(0.9+9?0.2)*0.001*48300.0 37000 34700.0 38100.0 36500.0 413.0 1788.0 732.0 3359.0)
```

## Radar

 - A radar chart requires 3 or more numeric columns to render sensibly.
 - Each numeric column represents one spoke in the radar. The column titles are used as spoke titles.
 - Each row in the data represents one circle withing the radar.

```sql type="radar" server="localhost:5000"
([] portfolio:`threadneedle`diamonte; agri:100 10; realEstate:100 10; tech:0 80; growthPotential:50 100; finance:60 20) 
```

## Calendar

 - The table should contain a date and atleast one numeric column.
 - The first numeric column will be used as the value for that date.
 - Dates should not be repeated. If they are the value selected is not guaranteed.


```sql type="calendar" server="localhost:5000"
([] dt:2023.12.31 - til 730; v:(asc 730?50)+(730?50)+730#90 80 72 83 40 2 3)
```
 
## Boxplot

 - Each numerical column in the table becomes one boxplot item in the chart.
 - The min/max/median/Q1/Q3 are calculated from the raw data.
 - This is inefficient as a lot more data is being passed than needed but useful for toggling an existing data set view quickly.

```sql type="boxplot" server="localhost:5000"
([] gold:10?10; silver:til 10; crude:desc til 10; slick:13-til 10; copper:10?3; iron:10?8; diamond:4+10?8; rubber:6+10?10; lead:8+10?12)
```

## Metrics

 - Two or more numeric columns are required.
 - The values in the first column are used for the X-axis.
 - The values in following columns are used for the Y-axis. Each column is displayed with a separate color.


```sql type="metrics" server="localhost:5000"
([] gold:10?10; silver:til 10; crude:desc til 10; slick:13-til 10)
```
