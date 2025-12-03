# Welcome to Pulse Notebooks

Pulse SQL Notebooks are a code-driven method for building data applications with SQL. This includes reports, analysis, monitoring and embedded dashboards.
Notebooks allow someone that knows SQL and a little markdown to create beatiful fully interactive data applications.

For help getting started see: http://www.timestored.com/qstudio/help/sqlnotebook

Any feature requests etc please raise on [github](https://github.com/timeseries/qstudio/issues).

---------------------------------------------------------

# Pages and Headers

 - Each .md file within the QStudio File Tree -> pages folder becomes one page on the left menu.
 - Each header tag `#` in markdown becomes an item on the right hand-side allowing navigation within the page.
 - If you look at this current file duckdb-examples.md within QStudio, you can see the markdown code.
 - If you edit the code in duckdb-examples.md and Save. Press **[Control] + [S]**. The page will automatically update to save your changes.
 - If you add a .md file to the pages folder, a new menu item will automatically appear.

# SQL Code blocks

SQL Code blocks are defined within triple backticks ```sql, on the same line you must configure which server to use for the query.
The server name must be one of the servers available within QStudio.

# Table

```sql type="grid" server="QDUCKDB"
query_data('NAME, QUANTITY, MID
	OKTA, 100, 249.30
	TWLO, 10, 593.4
	TTD, 50, 560.3
	DOCU, 500, 244.0
	NFLX, 233, 676.9
	AMZN, 432, 108.7
	TSLA, 270, 690.7
	AAPL, 322, 112.3
	BABA, 287, 266.1
	JPM, 127, 223.2
	JNJ, 123, 87.8
	FB, 312, 308.7
	MSFT, 165, 379.1')
```

## Select Table Example

````
```sql type="grid" server="QDUCKDB"
SELECT * FROM READ_PARQUET('https://www.timestored.com/data/sample/pq/quotes.parquet');
```
````

```sql type="grid" server="QDUCKDB"
SELECT * FROM READ_PARQUET('https://www.timestored.com/data/sample/pq/quotes.parquet');
```

# Timeseries Chart

The above SQL block specified ``type='grid'`` so rendered a table. 
If we instead specify ``type="timeseries"``, it renders a timeseries chart.

````
```sql type="timeseries" server="QDUCKDB"
SELECT * FROM READ_PARQUET('https://www.timestored.com/data/sample/pq/gold_vs_bitcoin.parquet');
```
````

```sql type="timeseries" server="QDUCKDB"
SELECT * FROM READ_PARQUET('https://www.timestored.com/data/sample/pq/gold_vs_bitcoin.parquet');
```

Charts can have their height set using ``height="150px"``

````
```sql type="timeseries" server="QDUCKDB" height="150px"
SELECT time,gold FROM READ_PARQUET('https://www.timestored.com/data/sample/pq/gold_vs_bitcoin.parquet');
```
````

```sql type="timeseries" server="QDUCKDB" height="150px"
SELECT time,gold FROM READ_PARQUET('https://www.timestored.com/data/sample/pq/gold_vs_bitcoin.parquet');
```

# Bar

- The first string columns are used as category labels.
- Whatever numeric columns appear after the strings represents a separate series in the chart.


Legend and tooltips can be turned off by setting ``legend={false}``:

```sql type="bar" server="QDUCKDB" legend={false} tooltip={false}
query_data('NAME, QUANTITY, MID
	OKTA, 100, 249.30
	TWLO, 10, 593.4
	TTD, 50, 560.3
	DOCU, 500, 244.0
	NFLX, 233, 676.9
	AMZN, 432, 108.7
	TSLA, 270, 690.7
	AAPL, 322, 112.3
	BABA, 287, 266.1
	JPM, 127, 223.2
	JNJ, 123, 87.8
	FB, 312, 308.7
	MSFT, 165, 379.1')
```


# Bar Horizontal

The charts are based on the wonderful [echarts](https://github.com/apache/echarts) library and accept an overrideJson which can contain any echarts configuration.
This json config adds a title and a button to download the image:

```sql type="bar_horizontal" server="QDUCKDB"  legend={false}  overrideJson={{title: { left: 'center', text: 'Hello Title' }, toolbox: { feature: { saveAsImage: {}} }}}
query_data('NAME, QUANTITY, MID
	OKTA, 100, 249.30
	TWLO, 10, 593.4
	TTD, 50, 560.3
	DOCU, 500, 244.0
	NFLX, 233, 676.9
	AMZN, 432, 108.7
	TSLA, 270, 690.7
	AAPL, 322, 112.3
	BABA, 287, 266.1
	JPM, 127, 223.2
	JNJ, 123, 87.8
	FB, 312, 308.7
	MSFT, 165, 379.1')
```

Possible types include: 
``grid``, ``timeseries``, ``area``, ``line``, ``bar``, ``stack``, ``bar_horizontal``, ``stack_horizontal``, ``pie``, ``scatter``, 
``bubble``, ``candle``, ``depthmap``, ``radar``, ``treemap``, ``heatmap``, ``calendar``, ``boxplot``, ``sunburst``, ``tree``, ``metrics``, ``sankey``.
Examples of some are shown below and the website contains [documentation](https://www.timestored.com/sqlnotebook/docs/) 
on them all including advance configuration of tables and timeseries charts.


# Candle


```sql type="candle" server="QDUCKDB"
SELECT * FROM READ_PARQUET('https://www.timestored.com/data/sample/pq/candle.parquet');
```


# Stack

- The first string columns are used as category labels.
- Whatever numeric columns appear after the strings represents a separate series in the chart.

```sql type="stack" server="QDUCKDB"
query_data('NAME, QUANTITY, MID
	OKTA, 100, 249.30
	TWLO, 10, 593.4
	TTD, 50, 560.3
	DOCU, 500, 244.0
	NFLX, 233, 676.9
	AMZN, 432, 108.7
	TSLA, 270, 690.7
	AAPL, 322, 112.3
	BABA, 287, 266.1
	JPM, 127, 223.2
	JNJ, 123, 87.8
	FB, 312, 308.7
	MSFT, 165, 379.1')
```

# Bar Horizontal

```sql type="stack_horizontal" server="QDUCKDB"
query_data('NAME, QUANTITY, MID
	OKTA, 100, 249.30
	TWLO, 10, 593.4
	TTD, 50, 560.3
	DOCU, 500, 244.0
	NFLX, 233, 676.9
	AMZN, 432, 108.7
	TSLA, 270, 690.7
	AAPL, 322, 112.3
	BABA, 287, 266.1
	JPM, 127, 223.2
	JNJ, 123, 87.8
	FB, 312, 308.7
	MSFT, 165, 379.1')
```

# Line

- The first string columns are used as category labels.
- Whatever numeric columns appear after the strings represents a separate series in the chart.

```sql type="line" server="QDUCKDB"
query_data('NAME, QUANTITY, MID
	OKTA, 100, 249.30
	TWLO, 10, 593.4
	TTD, 50, 560.3
	DOCU, 500, 244.0
	NFLX, 233, 676.9
	AMZN, 432, 108.7
	TSLA, 270, 690.7
	AAPL, 322, 112.3
	BABA, 287, 266.1
	JPM, 127, 223.2
	JNJ, 123, 87.8
	FB, 312, 308.7
	MSFT, 165, 379.1')
```


# Area

- The first string columns are used as category labels.
- Whatever numeric columns appear after the strings represents a separate series in the chart.
    
```sql type="area" server="QDUCKDB"
query_data('NAME, QUANTITY, MID
	OKTA, 100, 249.30
	TWLO, 10, 593.4
	TTD, 50, 560.3
	DOCU, 500, 244.0
	NFLX, 233, 676.9
	AMZN, 432, 108.7
	TSLA, 270, 690.7
	AAPL, 322, 112.3
	BABA, 287, 266.1
	JPM, 127, 223.2
	JNJ, 123, 87.8
	FB, 312, 308.7
	MSFT, 165, 379.1')
```

# Pie

 - Each numeric column represents one pie chart. The title of each pie chart will be the column title.
 - The segments of the pie chart use the string columns as a title where possible. If there are no string columns, row numbers are used.

```sql type="pie" server="QDUCKDB"
query_data('NAME	QUANTITY
 GOOG	654
 DOCU	500
 AMZN	432
 AAPL	322
 FB	312
 BABA	287
 TSLA	270')
```


## Sankey

 - Assuming string columns named S1,S2,S3 with a numeric column of value V.
 - Each row represents one flow from the top level S1 to the leaf node S3. S1->S2->S3->V
 - The first numeric column reprents the size of the flow between nodes.
 - Sizes are back-propagated to the top level.
 - Null can be used to represent either gaps or allow assigning value to a node that is neither an inflow nor outflow.


```sql type="sankey" server="QDUCKDB"
query_data('OrderOrigin	Exchange	State	v
Internal	ICE	Partial	2
GUI	ICE		6
Web	ICE		8
Platform	NYSE	Partial	19
Internal	NYSE	Filled	16
GUI	NYSE		17
Web	LDN	New	10
Platform	LDN		11
Internal	LDN	Partial	5
GUI	CBE	New	13
Web	CBE	New	16
Platform	CBE	Filled	17
')
```

## Boxplot

 - Each numerical column in the table becomes one boxplot item in the chart.
 - The min/max/median/Q1/Q3 are calculated from the raw data.
 - This is inefficient as a lot more data is being passed than needed but useful for toggling an existing data set view quickly.

```sql type="boxplot" server="QDUCKDB"
QUERY_DATA('a,b,c
1,2,3
3,4,7
3,3,10')
```

## Metrics

 - Two or more numeric columns are required.
 - The values in the first column are used for the X-axis.
 - The values in following columns are used for the Y-axis. Each column is displayed with a separate color.


```sql type="metrics" server="QDUCKDB"
QUERY_DATA('a,b,c
1,2,3')
```




# QDuckDB 

 - [QDuckDB](https://www.timestored.com/qstudio/help/qduckdb) has the full power of DuckDB but with BabelDB placed ontop.
 - [BabelDB](https://www.timestored.com/pulse/help/babeldb) adds the ability to join data from different data sources and to save any table result locally
 - Some examples below mostly use ``query_data`` to hardcode CSV tables as part of the query.
