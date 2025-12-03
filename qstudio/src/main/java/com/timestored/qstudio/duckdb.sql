-- Welcome to QStudio,
--
-- Below you will find example queries to generate each chart type:
-- - Press [Control]+[E] to run the highlighted text . 
--    Then on the chart tab/panel select "Time Series" chart type to draw the first example.
--
-- If you want help there are guides located at http://www.timestored.com/qstudio/help/
-- Any feature requests etc feel free to contact us at:  contact@timestored.com

-- Example of _SD_ SmartDisplay column formatting tags in DuckDB
SELECT
        id,
        TIMESTAMP '2025-01-01 00:00:00' + CAST(random()*600 AS INTEGER) * INTERVAL '1 second' AS time,
        list_element(list_value('partial','filled','waiting','done','cancelled'),1+CAST(random()*5 AS INTEGER)) AS status,
        list_element(list_value('GBPUSD','USDNZD','USDCAD','CHFJPY','EURUSD'),1+CAST(random()*5 AS INTEGER)) AS instrument,
        list_element(list_value('UBS','C','MS','HSBC','NOMURA','DB'),1+CAST(random()*6 AS INTEGER)) AS symbol_SD_TAG,
        90+random()*20 AS price_SD_CURUSD,
        list_element(list_value(NULL,':caret-down:red',':chevron-up:green',':arrow-up',':arrow-down',':error'),1+CAST(random()*6 AS INTEGER)) AS icon_SD_ICON,
        10+random()*10 AS bid,
        list_element(list_value('#FF6666',NULL,NULL,NULL,NULL,'#66FF66'),1+CAST(random()*6 AS INTEGER)) AS bid_SD_BG,
        list_element(list_value('0.xXXx','0.XXx','0.xxXX'),1+CAST(random()*3 AS INTEGER)) AS bid_SD_CODE,
        random() AS percent_SD_PERCENT0,
        'http://timestored.com' AS link,
        list_value(random(),random(),random(),random(),random()) AS sparkline_SD_SPARKLINE,
        list_value(abs(random()),abs(random()),abs(random()),abs(random()),abs(random())) AS sparkbar_SD_SPARKBAR
    FROM range(10) t(id);

-- Time Series 1
WITH b AS (SELECT DATE '2013-01-01'+INTERVAL '1 day'*i dt,i FROM generate_series(0,20)t(i))
SELECT dt,COS(i) cosineWave,SIN(0.6*i) sineWave FROM b;

-- Time Series 2
WITH b AS (SELECT TIME '10:00'+INTERVAL '1 minute'*i AS time,i FROM generate_series(0,98)t(i))
SELECT  time, 0.4*i-(i%8) AS Position, 100*SIN(0.015*i) AS Cost FROM b;



-- Multi-series Monthly
WITH b AS (
SELECT 
    DATE '2000-01-01' + INTERVAL '1 month' * i AS Month,
    (ARRAY[30,40,45,55,58,63,55,65,78,80,75,90])[i+1] AS Costs,
    (ARRAY[10,12,14,18,26,42,74,90,110,130,155,167])[i+1] AS Sales
FROM generate_series(0,11) t(i))
SELECT * FROM b;



-- Multi-Series Table
SELECT * FROM (
VALUES
('NorthAmerica','US',313847,15080,48300,77.14),
('Asia','China',1343239,11300,8400,72.22),
('Asia','Japan',127938,4444,34700,80.93),
('Europe','Germany',81308,3114,38100,78.42),
('Europe','UK',63047,2228,36500,78.16),
('Africa','Zimbabwe',13010,9.9,413,39.01),
('Asia','Bangladesh',152518,113,1788,61.33),
('Africa','Nigeria',166629,196,732,51.01),
('Asia','Vietnam',87840,104,3359,70.05)
)t(Continent,Country,Population,GDP,GDPperCapita,LifeExpectancy);

-- Bubble/Scatter
WITH b AS (
SELECT * FROM (
VALUES
('NorthAmerica','US',313847,15080,48300,77.14),
('Asia','China',1343239,11300,8400,72.22),
('Asia','Japan',127938,4444,34700,80.93),
('Europe','Germany',81308,3114,38100,78.42),
('Europe','UK',63047,2228,36500,78.16),
('Africa','Zimbabwe',13010,9.9,413,39.01),
('Asia','Bangladesh',152518,113,1788,61.33),
('Africa','Nigeria',166629,196,732,51.01),
('Asia','Vietnam',87840,104,3359,70.05)
)t(cn,ct,pop,gdp,gpc,life))
SELECT *,gpc/20 "GDPperCapita" FROM b;

-- Candlestick
WITH b AS (
SELECT 
    TIME '09:00' + INTERVAL '10 minutes' * i AS t,
    (55 + 2*i) + 30 AS high,
    (55 + 2*i) - 20 AS low,
    60 + i AS "open",
    (55 + 2*i) AS "close",
    (ARRAY[3,9,6])[(i % 3) + 1] AS volume
FROM generate_series(0,21) t(i))
SELECT * FROM b;


-- Heatmap (same dataset)
SELECT * FROM (
VALUES
('NorthAmerica','US',313847,15080,48300,77.14),
('Asia','China',1343239,11300,8400,72.22),
('Asia','Japan',127938,4444,34700,80.93),
('Europe','Germany',81308,3114,38100,78.42),
('Europe','UK',63047,2228,36500,78.16),
('Africa','Zimbabwe',13010,9.9,413,39.01),
('Asia','Bangladesh',152518,113,1788,61.33),
('Africa','Nigeria',166629,196,732,51.01),
('Asia','Vietnam',87840,104,3359,70.05)
)t(cont,country,pop,gdp,gpc,life);

-- Histogram
WITH b AS (
SELECT COS(0.0015*i) AS Returns,COS(0.002*i) AS  Losses
FROM generate_series(0,499)t(i))
SELECT * FROM b;

-- Pie Single
SELECT * FROM (
VALUES
('US',15080),('China',11300),('Japan',4444),
('Germany',3114),('UK',2228),('Zimbabwe',9.9),
('Bangladesh',113),('Nigeria',196),('Vietnam',104)
)t(country,gdp);

-- Pie Many
SELECT * FROM (
VALUES
('NorthAmerica','US',313847,15080,48300,77.14),
('Asia','China',1343239,11300,8400,72.22),
('Asia','Japan',127938,4444,34700,80.93),
('Europe','Germany',81308,3114,38100,78.42),
('Europe','UK',63047,2228,36500,78.16),
('Africa','Zimbabwe',13010,9.9,413,39.01),
('Asia','Bangladesh',152518,113,1788,61.33),
('Africa','Nigeria',166629,196,732,51.01),
('Asia','Vietnam',87840,104,3359,70.05)
)t(cont,country,pop,gdp,gpc,life);
