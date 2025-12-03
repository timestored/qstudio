# Welcome to Pulse Notebooks

Pulse SQL Notebooks are a code-driven method for building data applications with SQL. This includes reports, analysis, monitoring and embedded dashboards.
Notebooks allow someone that knows SQL and a little markdown to create beatiful fully interactive data applications.

For help getting started see: http://www.timestored.com/qstudio/help/sqlnotebook

Any feature requests etc please raise on [github](https://github.com/timeseries/qstudio/issues).

---------------------------------------------------------

# Pages and Headers

 - Each .md file within the QStudio File Tree -> pages folder becomes one page on the left menu.
 - Each header tag `#` in markdown becomes an item on the right hand-side allowing navigation within the page.
 - If you look at this current file kdb-simple-examples.md within QStudio, you can see the markdown code.
 - If you edit the code in kdb-simple-examples.md and Save. Press **[Control] + [S]**. The page will automatically update to save your changes.
 - If you add a .md file to the pages folder, a new menu item will automatically appear.

# SQL Code blocks

SQL Code blocks are defined within triple backticks ```sql, on the same line you must configure which server to use for the query.
The server name must be one of the servers available within QStudio.


```sql server="localhost:5000" type='grid'

 select from ([] time:.z.t-til 50; status:50?`partial`filled; 
		 instrument:50?`GBPUSD`USDNZD`USDCAD`CHFJPY`EURUSD;
		 symbol_SD_TAG:50?`UBS`C`MS`HSBC`NOMURA`DB;
		 bid:50?20.0;
		 percent_SD_PERCENT0:50?1.2 )

```


## Example Time Series Charts

The above SQL block specified ``type='grid'`` so rendered a table. 
If we instead specify ``type="timeseries"``, it renders a timeseries chart.

````
```sql type="timeseries" server="localhost:5000"
([] dt:2013.01.01+til 21; cosineWave:cos a; sineWave:sin a:0.6*til 21)
```
````

```sql type="timeseries" server="localhost:5000"
([] dt:2013.01.01+til 21; cosineWave:cos a; sineWave:sin a:0.6*til 21)
```

Charts can have their height set using ``height="150px"``

````
```sql type="timeseries" server="localhost:5000" height="150px" 
([] time:10:00t+60000*til 99; Position:0.4*a-mod[a;8] -a:100*sin 0.015*til 99)
```
````

```sql type="timeseries" server="localhost:5000" height="150px" 
([] time:10:00t+60000*til 99; Position:0.4*a-mod[a;8] -a:100*sin 0.015*til 99)
```

## Bar / Stack

Legend and tooltips can be turned off by setting ``legend={false}``:

```sql type="bar" server="localhost:5000" legend={false} tooltip={false}

([Month:2000.01m + til 12]  
	 Costs:30.0 40.0 45.0 55.0 58.0 63.0 55.0 65.0 78.0 80.0 75.0 90.0 ; 
	 Sales:10.0 12.0 14.0 18.0 26.0 42.0 74.0 90.0 110.0 130.0 155.0 167.0 )
```


### Bar Horizontal

The charts are based on the wonderful [echarts](https://github.com/apache/echarts) library and accept an overrideJson which can contain any echarts configuration.
This json config adds a title and a button to download the image:

````
```sql type="bar_horizontal" server="localhost:5000" legend={false}  overrideJson={{title: { left: 'center', text: 'Hello Title' }, toolbox: { feature: { saveAsImage: {}} }}}
([Month:2000.01m + til 12]  
	 Costs:30.0 40.0 45.0 55.0 58.0 63.0 55.0 65.0 78.0 80.0 75.0 90.0 ; 
	 Sales:10.0 12.0 14.0 18.0 26.0 42.0 74.0 90.0 110.0 130.0 155.0 167.0 )
```
````

```sql type="bar_horizontal" server="localhost:5000" legend={false}  overrideJson={{title: { left: 'center', text: 'Hello Title' }, toolbox: { feature: { saveAsImage: {}} }}}
([Month:2000.01m + til 12]  
	 Costs:30.0 40.0 45.0 55.0 58.0 63.0 55.0 65.0 78.0 80.0 75.0 90.0 ; 
	 Sales:10.0 12.0 14.0 18.0 26.0 42.0 74.0 90.0 110.0 130.0 155.0 167.0 )
```

Possible types include: 
``grid``, ``timeseries``, ``area``, ``line``, ``bar``, ``stack``, ``bar_horizontal``, ``stack_horizontal``, ``pie``, ``scatter``, 
``bubble``, ``candle``, ``depthmap``, ``radar``, ``treemap``, ``heatmap``, ``calendar``, ``boxplot``, ``sunburst``, ``tree``, ``metrics``, ``sankey``.
Examples of some are shown below and the website contains [documentation](https://www.timestored.com/sqlnotebook/docs/) 
on them all including advance configuration of tables and timeseries charts.

### Stack

```sql type="stack" server="localhost:5000"
([Month:2000.01m + til 12]  
	 Costs:30.0 40.0 45.0 55.0 58.0 63.0 55.0 65.0 78.0 80.0 75.0 90.0 ; 
	 Sales:10.0 12.0 14.0 18.0 26.0 42.0 74.0 90.0 110.0 130.0 155.0 167.0 )
```

### Stack Horizontal

```sql type="stack_horizontal" server="localhost:5000"
([Month:2000.01m + til 12]  
	 Costs:30.0 40.0 45.0 55.0 58.0 63.0 55.0 65.0 78.0 80.0 75.0 90.0 ; 
	 Sales:10.0 12.0 14.0 18.0 26.0 42.0 74.0 90.0 110.0 130.0 155.0 167.0 )
```

### Multiple Bar Series

```sql type="bar" server="localhost:5000"
([] Continent:`NorthAmerica`Asia`Asia`Europe`Europe`Africa`Asia`Africa`Asia;
	 Country:`US`China`japan`Germany`UK`Zimbabwe`Bangladesh`Nigeria`Vietnam; 
	 Population:313847.0 1343239.0 127938.0 81308.0 63047.0 13010.0 152518.0 166629.0 87840.0 ;
	 GDP:15080.0 11300.0 4444.0 3114.0 2228.0 9.9 113.0 196.0 104.0 ; 
	GDPperCapita:48300.0 8400.0 34700.0 38100.0 36500.0 413.0 1788.0 732.0 3359.0 ;  
	LifeExpectancy:77.14 72.22 80.93 78.42 78.16 39.01 61.33 51.01 70.05 )
```

## Line     / Area Chart

### Line

```sql type="line" server="localhost:5000"
([Month:2000.01m + til 12]  
	 Costs:30.0 40.0 45.0 55.0 58.0 63.0 55.0 65.0 78.0 80.0 75.0 90.0 ; 
	 Sales:10.0 12.0 14.0 18.0 26.0 42.0 74.0 90.0 110.0 130.0 155.0 167.0 )
```

### Area

```sql type="area" server="localhost:5000"
([Month:2000.01m + til 12]  
	 Costs:30.0 40.0 45.0 55.0 58.0 63.0 55.0 65.0 78.0 80.0 75.0 90.0 ; 
	 Sales:10.0 12.0 14.0 18.0 26.0 42.0 74.0 90.0 110.0 130.0 155.0 167.0 )
```


## Scatter 

```sql type="scatter" server="localhost:5000"
update GDPperCapita%20 from ([] Continent:`NorthAmerica`Asia`Asia`Europe`Europe`Africa`Asia`Africa`Asia;
	 Country:`US`China`japan`Germany`UK`Zimbabwe`Bangladesh`Nigeria`Vietnam; 
	 Population:313847.0 1343239.0 127938.0 81308.0 63047.0 13010.0 152518.0 166629.0 87840.0 ;
	 GDP:15080.0 11300.0 4444.0 3114.0 2228.0 9.9 113.0 196.0 104.0 ; 
	GDPperCapita:48300.0 8400.0 34700.0 38100.0 36500.0 413.0 1788.0 732.0 3359.0 ;  
	LifeExpectancy:77.14 72.22 80.93 78.42 78.16 39.01 61.33 51.01 70.05 )
```

## Bubble Chart

The first numeric column is x-axis, 2nd is y-axis, 3rd is bubble size. Strings are used as labels. 

```sql type="bubble" server="localhost:5000"
update exports:(0.1+9?0.1)*GDP, exportsPerCapita:(0.4+9?0.1)*GDPperCapita from 
	  ([] Country:`US`France`japan`Germany`UK`Zimbabwe`Bangladesh`Nigeria`Vietnam; 
	  Population:(0.9+9?0.2)*313847.0 213847.0 127938.0 81308.0 63047.0 13010.0 152518.0 166629.0 87840.0 ;
	  GDP:(0.9+9?0.2)*15080.0 3333. 4444.0 3114.0 2228.0 9.9 113.0 196.0 104.0 ; 
	  GDPperCapita:(0.9+9?0.2)*0.001*48300.0 37000 34700.0 38100.0 36500.0 413.0 1788.0 732.0 3359.0)
```

## Candlestick Chart

```sql type="candle" server="localhost:5000"
([] t:09:00t+600000*til 22; high:c+30; low:c-20; open:60+til 22; close:c:55+2*til 22; volume:22#3 9 6)
```

## PieChart
```sql type="pie" server="localhost:5000"
([] Country:`US`China`japan`Germany`UK`Zimbabwe`Bangladesh`Nigeria`Vietnam; 
	 GDP:15080.0 11300.0 4444.0 3114.0 2228.0 9.9 113.0 196.0 104.0 )
```

### Many Pies

```sql type="pie" server="localhost:5000"
([] Continent:`NorthAmerica`Asia`Asia`Europe`Europe`Africa`Asia`Africa`Asia;
	 Country:`US`China`japan`Germany`UK`Zimbabwe`Bangladesh`Nigeria`Vietnam; 
	 Population:313847.0 1343239.0 127938.0 81308.0 63047.0 13010.0 152518.0 166629.0 87840.0 ;
	 GDP:15080.0 11300.0 4444.0 3114.0 2228.0 9.9 113.0 196.0 104.0 ; 
	GDPperCapita:48300.0 8400.0 34700.0 38100.0 36500.0 413.0 1788.0 732.0 3359.0 ;  
	LifeExpectancy:77.14 72.22 80.93 78.42 78.16 39.01 61.33 51.01 70.05 )
```


