www.timestored.com/qStudio - The analytical SQL Client.

We hope to make this the best IDE for analytical databases there can be 
so if you find a bug or feel there's a feature you would like added
please raise a github issue.

- Ryan Hamilton
___________________________________________________________________

Changelog:

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

2024-03-13 - 4.06   - Add ability to transpose rows.
					  DuckDB 1.2.1. Improve display of DuckDB arrays.
					  Add comma separator for thousands option.

2024-02-23 - 4.05   - Upgrade kdb+ c/jdbc.java to allow SSL TLS. 		
					- Add preference to allow hiding all tooltips.	
					- Double-click on kdb+ table with dictionary/table/list nested in cell will pop it out. 	
					
2024-01-23 - 4.04   - Show column info (avg/min/max) when column header is hovered.
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

2024-06-05 - 3.06   - Add Redshift support.
					- DuckDB 1.0. Restore window size on restart. 
					
2024-06-03 - 3.05   - Copy-paste bugfix.

2024-05-30 - 3.04   - File->Open handles sqlite/duckdb better. DolphinDB = v3 driver.

2024-05-30 - 3.03   - Improve DolphinDB support. Add auto-complete, documentation etc. PRQL Mac bugfix.

2024-05-28 - 3.02   - Bugfixes and UI improvements. 
                    - Add PRQL Compilation Support.

2024-05-24 - 3.01   - AI - Generate SQL queries, ask for error help or explanations via OpenAI assistant.
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

2024-04-30 - 2.59   - Add ability to export table query result to xlsx file.

2024-04-10 - 2.58   - Add highlighting support for  c, cpp, java, bat, sh, html, js, json, xml, PRQL and properties files.

2024-04-01 - 2.57   - Improved Scatter Chart. Scatter now supports time-series.

2024-03-01 - 2.56   - Better error messaging for torq/deferred or unexpected responses. Improved Pie Chart.

2024-02-11 - 2.55   - Update duckdb to 0.10.0

2024-01-22 - 2.54   - Drivers: Update clickhouse to 0.6.0 and DolhpinDB to 1.30.22.5 drivers.

2023-11-01 - 2.53   - Allow right-click closing connection to servers to allow DuckDB file reuse.

2023-08-10 - 2.51   - Add support for format 2022 license. In preparation for no longer supporting old 2013qs license format.

2023-08-04 - 2.40   - Improve SQL database support. Show server tree for many databases.

2023-07-31 - 2.34   - Improve SQLite support. Allow File->Open Database.

2023-07-27 - 2.33   - Add support for 30+ databases, improve server tree support.
					- Bundle JRE11 with windows installer and zip.

2023-07-19 - 2.11   - Add Redis Support. 
					- Allow selecting kdb row by clicking on row number.

2023-06-15 - 2.10   - Update JQ. Improve flatlaf startup stability on windows by bundling dlls.

2023-06-01 - 2.09   - CSV loader UI fix. Upgrade to DuckDB 0.8.

2023-05-01 - 2.07   - MS SQL JDBC driver fix trustServerCertificate=true

2023-04-04 - 2.06   - Bugfix: Support dark theme paging tables and non-kdb tables.

2023-03-27 - 2.05   - Improved Appearance
				    - Add Flat/Intellij/Material themes including fully functioning dark theme.
				    - Change default font to Jetbrains Mono and allow rescaling of all fonts.
					
2023-02-27 - 2.04   - Minor bugfixes including showing that kdb returning null is ::

2023-02-27 - 2.02   - Add Server Tree support for standard SQL databases. Relies on information_schema.

2023-02-24 - 2.01   - Allow running queries against postgres/mysql/clickhouse/MS sql server databases.
					- DuckDB support to allow loading, creating and querying .duckdb databases
					- Remove sqlDashboards integration. sqlDashboards legacy version still available separately but Pulse is replacement.


					
2022-10-31 - 1.54   - Improved display of date/timestamps to show them in kdb format 2001.01.01 where possible.
					- Improved table copy/paste of single cells. To allow easy copying of strings with \r\n\t etc.
					- (sqlDashboards) Upgrade dependencies including: apache connection pooling, mysql/postgresql/ms-sql jdbc drivers.
					- (sqlDashboards) Improved handling of multiple result sets OR queries that don't return any table.
					- (sqlDashboards) Switch from $key to ((key)) and {key}
					
2022-06-09 - 1.53   - Certificate update.

2020-11-15 - 1.52   - Allow running on java 8/9/10/11.
					- Bugfix:DefaultDocumentEventUndoableWrapper error no longer thrown on java9. JSyntaxPane upgraded.
					- Bugfix:ExceptionInInitializerError no longer throws on startup, DockFrontend library upgraded.
					- Bugfix:UnsupportedDataTypeException was throw due to javax.bind removal in java 11+. Now bundled.
					- (sqlDashboards) Upgrade charting library to 1.0.19
					
2020-05-10 - 1.51   - jq added to bundle.

2019-12-30 - 1.50   - qDoc Add @example/@col support, decrease output verbosity, add baseHref support.

2019-08-01 - 1.49   - (sqlDashboards) SQL Array support, now displays nested numeric/date/time arrays.

2019-04-25 - 1.48   - Build using java 8. Note: This may no longer run on java 6 runtimes.
					- Allow license.txt specified in path.

2018-07-01 - 1.47   - bugfix to work in kdb 3.6 due to $[;;] parser change.
					- Copy web link to latest result button added.

2018-05-12 - 1.46   - Update to latest qunit with assertKnown and parameters.
				    - Fix step plot to auto size the axis rather than always start at zero.

2018-03-31 - 1.45   - Hidden folders/files regex fix.
					- Add Step-Plot Chart display option

2018-02-11 - 1.44   - Add Stacked Bar Chart display option
                    - Add Dot graph render display option (Inspired by Noormo)
                    - Bugfix: Mac was displaying startup error with java 9.
					- Bugfix: Ctrl+F Search in source fixed. (Thanks Alex)
					
2017-04-12 - 1.43   - Add Stack Trace reporting when user is using wrapped-queries and kdb 3.5+
                    - Bugfix: Mac "Save As" dialog was hiding the filename prompt. Fixed.
					
2017-02-05 - 1.42   - Bugfix Sending empty query would cause qStudio to get into bad state.
					- Default to chart NoRedraw when first loaded to save memory/time.
					- Preferences Improvements
						- Option to allow saving Document with windows \r\n or linux \n line endings. Settings -> Preferences... -> Misc
						- Allow specifying regex for folders that should be ignored by the "File Tree" window and Autocomplete
					- Add copy "hopen `:currentServer" command button to toolbar.
					- Ctrl+p Shortcut - Allow opening folders in explorer aswell as files.
					- Smarter Background Documents Saving (30 seconds between saves on background thread)
					- (sqlDashboards) Stop wrapping JDBC queries as we dont want kdb to use the standard SQL handler. We want to use the q) handler.
					- (sqlDashboards) Allow saving .das without username/password to allow sharing
										Prompt user on file open if cant connect to server.
					- (sqlDashboards) Bugfix: Allow resizing of windows within sqlDashboards even when "No table returned" or query contains error.
					- (sqlDashboards) If query is wrong and missing arg or something, report the reason.
					
2016-06-24 - 1.41   - Add ability to use custom Security Authentications and JDBC drivers
					  http://www.timestored.com/qstudio/help/kerberos-custom-security-authentication
					- Load .jar plugins from libs folder.
					- Remove ctrl+w shortcut as it was often getting used by mistake
					- Improved startup/shutdown logging.
                    
					
2016-02-15 - 1.40   - No need to save changes before shutdown, unsaved changes stored till reopened.
                    - Add sqlchart to system path.
					- Fix display of tables with underscore in the name.
                    - Database documenter/report enhancements
					- Improved code printing
					- FileTreePanel much more efficient at displaying large number of files.
					
2015-01-16 - 1.37	- Fix query cancelling
					- Number of fixes to help supporting 5000+ server connections:
						- Allow nested connection folders
						- Allow specifying default username/password once for all servers
						- Add critical color option
					- Fix critical Mac bug that prevented launching in some instances
					- Sort File Tree Alphabetically
					
2014-10-20 - 1.36	- Allow opening copies of charts, results and the console in a pop-out window
					- Increase size of serverlist supported (4000+)
					- Colored editor tabs
					- Bugfix to tooltips and specialised sync messaging

2014-10-15 - 1.35	- Added support for UTF-8 / Chinese Language
					- Provide a dark code editor theme
					- (sqlDashboards) Fix heatmap to support single string columns again
					
2014-09-01 - 1.34	- Added Server Tree options to allow cloning connectsion, renaming folders, etc.
					- Bugfixes to server edit screen, autocomplete and jump-to-definition

2014-04-18 - 1.33	- Allow renaming folders and adding connections to folders.
					- Bugfix editing connectioning dialog was changing port to default
					- Bugfix more careful returning connections to connection pool
					- (sqlDashboards) Add SqlChart command line charting application.
					  
2014-03-13 - 1.32	- (sqlDashboards) Candlestick chart - has separate charts for volume and prices.
					- (sqlDashboards) Add live yahoo finance sqlDashboards demos for built-in database and kdb+
					- (sqlDashboards) Command Line Chart Generator added.
					- (sqlDashboards) Major change to how dashboard arguments are formed.

2014-03-01 - 1.31	- minor bugfix release.  

2014-02-11 - 1.30	- Ctrl-P power bar that allows performing most actions from the editor
						e.g. lookup docs, open file, switch server. Also Ctrl-I,Ctrl-U
					- Many many Changes to sqlDashboards, including forms...
					- Scrolling line numbers on result table (Thanks Ankit)
					- Configurable Server Connection Querying, (Thanks Sunny)
						settings for persist connection, wrap queries etc.
					- Allow code editor font selection (Thanks Jean-Pierre)
					- Ability to place servers in folders.
					- Servers can have a color set that will alter the editor background.
					
					
2013-11-11 - 1.29   - Added Chart Themes including dark bbg
					- Allowed multiple instances of qStudio
					
2013-06-27 - 1.28   - Added Csv Loader (pro)
					- Added qUnit unit testing (pro)
					- Bugfix to database management column copying.
					- Export selection/table bugs fixed and launches excel (thanks Jeremy / Ken)
					
2013-05-06 - 1.27   - Support charting and formatted text for all Kdb Time types including Timespan, Timestamp, Month etc.
					- Sorting columns by numeric columns now sorts numerically (thanks Ken)
					- Added more q keywords and documentation
					- Many small bug fixes (thanks Seetaram)

2013-03-25 - 1.26   - Add File Tree that allows browsing directory and providing autocomplete
					- qDoc supports custom user tags (Thanks Aaron)
					- Allow adding/exporting whole lists of servers at once (much quicker)
					- Installers are now signed.
					- Ctrl-D "goto definition" of function to open that file/position
					- (PRO) Unit Testing and function profiling partially integrated.
					
2013-01-30 - 1.25   - Faster chart drawing (~1.6x faster)
					- Added No Redraw chart option for those who want extra speed
					- Numerous bugfixes to charts that froze
					- Allow setting code editor font size
					- Fix display of boolean/byte lists

2013-01-30 - 1.23   - Added Tree view of list data.
					- Quicker drawing of large complex objects.
					- Crash asks user to report from in-app.
					- BugFix: Tooltips for tables with many columns caused Aero crash (thanks Sunny)
					- BugFix: Secured servers where populating tree throws exceptions
					
2013-01-25 - 1.21   - Mac UI Improvements (thanks Charlie)
					
2013-01-24 - 1.20   - Results Table shows wide tables with scrollbar (thanks Weiyi)
					
