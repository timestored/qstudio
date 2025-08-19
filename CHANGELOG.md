# Changelog

**qStudio is a free SQL GUI** that lets you browse tables,
run SQL scripts, and chart and export the results.
qStudio runs on Windows, macOS and Linux, and works with
every popular database including mysql, postgresql, mssql, kdb…
For more info see:
[timestored.com/qstudio](https://timestored.com/qstudio)

This file lists the changes that have occurred in the project:
QStudio has been continuously developed and improved since first released in 2013.
We hope to make this the best IDE for analytical databases there can be
If you find a bug or feel there's a feature you would like added raise an issue on github or email us: tech@timestored.com


___________________________________________________________________

Changelog:

2025-08-19 - 4.12   - Show negative numbers in red. TRUE/FALSE for booleans. User configurable.
					- QDoc 2.0 with modern mkdocs theme.

2025-05-12 - 4.11   - Upgrade ClickHouse driver to 0.8.6.
					- Upgrade DuckDB to 1.3.0.0.
	 
2025-05-12 - 4.09   - Improve Autocomplete suggestions
					- Add namespace listing panel.
					- Improve QDoc to add file level details.
     
2025-04-22 - 4.08   - Bugfix: Candlestick charts were wrong as receiving wrong timezone from kdb driver. Timezone now set to UTC.
					- Bugfix: "Save as" to .sql was shrinking text. Now fixed.
					- Bugfix: Show red X close on every document tab.					

2025-04-06 - 4.07   - Add UI Scaling and Font size as separate options to help with 4K monitors
					- Bugfix: Kdb+ Queries with errors could cause 30 second freezes.
					- Bugfix: Switch back to MonoSpaced font. Variable width fonts cause wrong caret position.
					- Improved high resolution QStudio icon.
					- Mac: Bugfix: Prefences dialog fixed and allows choosing font.
					- Mac: Fixed Menu shoing about/preferences/exit.
					- Mac: Allow Command+Option+E to run current query.
					- 4K handling improved sizing of dialogs.
					- Bugfix: Improved duckdb init when folder is empty.

2025-03-13 - 4.06   - Add ability to transpose rows.
					- DuckDB 1.2.1. Improve display of DuckDB arrays.
					- Add comma separator for thousands option.

2025-02-23 - 4.05   - Upgrade kdb+ c/jdbc.java to allow SSL TLS. 		
					- Add preference to allow hiding all tooltips.	
					- Double-click on kdb+ table with dictionary/table/list nested in cell will pop it out. 	
					
2025-01-23 - 4.04   - Show column info (avg/min/max) when column header is hovered.
					- Remove watched expressions entirely.
					- Improved UI threading for tree/chart display.
					
2024-12-23 - 4.03   - Upgrade H2 database to version 2.2.224.
					- Upgrade DuckDB 1.1.3, MySQL 9.1.0, postgresql 42.7.4.
					- Default to font sans serif to fix chinese language support.

2024-12-10 - 4.02   - Bugfix: enable buttons on first server add. SQL Notebooks add ```sql type='table' and nesting.

2024-11-18 - 4.01   - SQL Notebooks official launch version. 

2024-11-12 - 3.86   - Improved Welcome and code examples. Improved display of kdb+ lists as table. bugfix: chartpanel size.

2024-11-08 - 3.85   - Add update reminder. Remove expression watcher, splash screen. Faster startup.

2024-11-01 - 3.84   - Improved auto-complete of multiple languages. Move all kdb+ code to one area.

2024-10-25 - 3.83   - Remove Legacy SQLDashboards. Update notebooks. Merge to one java project.

2024-10-21 - 3.82   - (SqlNotebooks) - Add Page select dropdown menu when on small screen. Add basic markdown/select-table autocomplete.

2024-10-18 - 3.81   - (SqlNotebooks) - Add light/dark mode. Improved quote handling. Add top menu. Add demos.

2024-10-14 - 3.80   - (SqlNotebooks) - Allow users to create SQL notebooks based on markdown and HTML.
					- File tree panel significantly improved. Now opens text files in qStudio, binary files e.g. jpeg using OS.
					- 10x faster to open OS clicked files in existing qStudio.
					- Bugfix: Closing doc out of sync bug. 

2024-09-14 - 3.10   - Bugfix: Fix custom server border color.

2024-09-13 - 3.07   - Upgrade DuckDB to v1.1.0. Add server search to toolbar. 

## 3.06 - 2024-06-05 
- Add Redshift support.
- DuckDB 1.0.
- Restore window size on restart.
- Drag and drop of files into qStudio
- UI and User Experience improvements
- Improved logging of environment information
- Improve connection to SQLite on macOS

## 3.05 - 2023-06-03 
- copy-paste bugfix.

## 3.04 - 2023-05-30 
- File->Open handles sqlite/duckdb better. 
- DolphinDB = v3 driver.

## 3.03 - 2023-05-30

- Improve DolphinDB support.
- Add auto-complete, documentation etc. 
- PRQL Mac bugfix.

## 3.02 - 2024-05-28   
- Bugfixes and UI improvements. 
- Improve DolphinDB support. 
- Add PRQL Compilation Support.

## 3.01 - 2024-05-24 
- AI - Generate SQL queries, ask for error help or explanations via OpenAI assistant.
- Pivot - Perform excel like pivots within qStudio and have it generate the query for you.
- BabelDB - Query any database and store it to a local duckdb instance.  
- SQL - support significantly improved. Documentation added, highlighting improved, added code formatter. 
- SQL - Added Ctrl+Q run current query support. Lower/uppercase commands.  
- Parquet File viewer support.
- Generate command line charts using sqlchart command 
- Default theme changed to dark mode.  
- UI Niceties - Added icons for charts / database types. Safer chart rendering. 
- UI Niceties - Document tabs now allow mouse scrolling, added file options to menu. Fixed bugs. 
- Remove - Legacy java licensing code.
					- DuckDB - Improved rendering of the various SQL types.

*... many previous releases...*

## 2.0 - 2023-02-24 - Version 2.0

## 1.20 - 2013-01-24 - FIRST RELEASE
