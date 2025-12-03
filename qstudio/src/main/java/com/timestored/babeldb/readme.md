![BabelDB](babeldb-flow.png)

# BabelJDBC

## _Query multiple databases within one query_

**BabelJDBC** - Is a java SQL driver that allows querying 20+ databases with queries that join between servers.

As well as 30+ databases, BabelDB allows **querying REST/JSON/CSV data sources directly**.

## Examples

Add a mysql / postgresql connection and query them:
Notice the SQL queries are wrapped in parentheses (), Quotes also work but parentheses allow SQL highlighting to work better in most editors.

``` sql
put_connection('serverA','mysql','localhost',3306,'root','password');
put_connection('pgserv','postgresql','localhost',5432,'postgres','password');

SELECT * FROM query_db('serverA',(select * from users)) Users
    INNER JOIN query_db('pgserv',(select count(*) AS c GROUP BY userid from inventory)) inv
	ON Users.id = inv.CustomerID;
```

Query a REST/CSV data source:
	
``` sql
select * from query_web('https://api3.binance.com/api/v3/ticker/price');
query_web('https://api.github.com/search/repositories?q=more+useful+keyboard');

query_web('https://www.iso20022.org/sites/default/files/ISO10383_MIC/ISO10383_MIC.csv')

CREATE VIEW IF NOT EXISTS binance.ticker_24hr AS query_web('https://api1.binance.com/api/v3/ticker/24hr');
SELECT * FROM binance.ticker_24hr;

CREATE VIEW IF NOT EXISTS binance.ticker_price AS 
    query_web('https://api2.binance.com/api/v3/ticker/bookTicker','',
	    {symbol: 'VARCHAR', bidPrice: 'DOUBLE', bidQty: 'DOUBLE', askPrice: 'DOUBLE', askQty: 'DOUBLE'});
SELECT * FROM binance.ticker_price;
```

## query_web JSON Docs

``query_web`` takes the following arguments:

| Arg | Description |
|-----|-----------|
| HTTP | HTTP endpoint with data |
| JQ Path (Optional) | [JQ Path Expression](https://jqlang.github.io/jq/manual/) |
| ColumnTypes (Optional) |  A json struct that specifies the key names and value types contained within the JSON file <br>e.g. ``{key1: 'INTEGER', key2: 'VARCHAR'}`` - If not specified these will be guessed. |

## Why do this?

[TimeStored](https://www.timestored.com/) make tools for data analysts and SQL users:
 - [**qStudio**](https://www.timestored.com/qstudio/) is an SQL IDE for data analysts
 - [**PulseUI**](https://www.timestored.com/pulse/) is a low-code tool for developing real-time internal applications fast.

BabelJDBC allows those tools to combine queries from any data source in a clean way without clicking and dragging UI elements.
We are particularly proud that it just is SQL with special functions added in. 

## Hows does it work?

``query_`` queries cause temporary tables to be created and the function call is replaced with that table name. Simplified example: 
``select date,txt from query_web('https://api3.binance.com/api/v3/ticker/price');`` becomes:

``` sql
CREATE TEMPORARY TABLE temp.price( d DATE, col INTEGER, txt VARCHAR);
INSERT INTO TABLE temp.price VALUES( ......);
INSERT INTO TABLE temp.price VALUES( ......);

select date,txt from temp.price;
```

## Limitations / TODO

 - Babel only supports limited simple column types. Would be nice to add arrays etc.
 - Babel uses H2 version 1.4.200 as SQL engine, Pluggable option would allow user to choose.
 

## Why Babel?

From The Hitchhiker's Guide to the Galaxy , The **Babel fish** is a small, bright yellow fish, which can be placed in someone's ear in order for them to be able to hear any language translated into their first language. Ford Prefect puts one in Arthur Dent's ear at the beginning of the story so that he can hear the Vogon speech. 

BabelJDBC allows combining queries between any JDBC compatible database.

![BabelDB](babelfish.png)

Huge credit and thanks goes to:
  - [Charles Skelton](https://github.com/CharlesSkelton) for the initial idea and name.
  - [H2 Database](http://www.h2database.com/) for supplying the core engine that performs the SQL.