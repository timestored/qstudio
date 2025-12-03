package com.timestored.sqldash.exampledb;

import static com.timestored.misc.HtmlUtils.clean;
import static com.timestored.misc.HtmlUtils.cleanAtt;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.timestored.connections.ConnectionManager;
import com.timestored.connections.DBTestRunner;
import com.timestored.connections.DBTestRunnerFactory;
import com.timestored.connections.ServerConfig;
import com.timestored.misc.HtmlUtils;
import com.timestored.misc.IOUtils;
import com.timestored.sqldash.ChartParams;
import com.timestored.sqldash.ChartParams.ChartParamsBuilder;
import com.timestored.sqldash.SqlChart;


/**
 * Allows generating an html/php help page displaying png images of charts
 * together with the sql that generated them based on an {@link ExampleChartDB}. 
 *
 */
public class ExampleDbHtmlGenerator {

	private static final Logger LOG = Logger.getLogger(ExampleDbHtmlGenerator.class.getName());
	private static final String NL = "\r\n";
	private static final int IMG_WIDTH = 500;
	private static final int IMG_HEIGHT = 300;

	/**
	 * Generate png charts and both html/php pages that display the example charts
	 * together with decscriptions, anmes and code for the database and charts.
	 * @param outDir The directory files will be placed in.
	 */
	public static void generatePages(ExampleChartDB exampleChartDB, File outDir)
			throws IOException, SQLException {
		outDir.mkdirs();
		Map<String,ExampleChartQuery> charts = generate(outDir, exampleChartDB);

		String title = exampleChartDB.getName() + " Database Example QStudio Charts";

		String sqlFilename = exampleChartDB.getName().replace(' ', '-').toLowerCase() + ".sql";
		
		String html = generateHtml(exampleChartDB, charts, title, sqlFilename);

		String initSql = Joiner.on("\r\n").join(exampleChartDB.getInitSQL(true));
		IOUtils.writeStringToFile(initSql, new File(outDir, sqlFilename));

		String htmlFile = HtmlUtils.getXhtmlTop(title) + html + HtmlUtils.getXhtmlBottom();
		String phpFile = HtmlUtils.getTSTemplateTop(title) + html + HtmlUtils.getXhtmlBottom();

		IOUtils.writeStringToFile(htmlFile, new File(outDir, "index.html"));
		IOUtils.writeStringToFile(phpFile, new File(outDir, "index.php"));
	}
	
	/**
	 * Generate html describing the {@link ExampleChartDB} and for each
	 * chart generated display an image and description.
	 */
	private static String generateHtml(ExampleChartDB exampleChartDB,
			Map<String, ExampleChartQuery> charts, String title, String sqlFilename) {
		
		
		LOG.info("generateHtml for " + exampleChartDB.getName());
		
		StringBuilder sb = new StringBuilder();
		sb.append(NL);

		String dbName = exampleChartDB.getDbType().getNiceName();

		/*
		 * Header with title and description of overall database
		 */
		sb.append("<h1>").append(title).append("</h1>");
		sb.append(NL);
		sb.append("<p>").append(exampleChartDB.getDescription()).append("</p>");
		sb.append(NL);

		String initSql = Joiner.on("\r\n").join(exampleChartDB.getInitSQL(true));
		if(initSql!=null && initSql.trim().length()>0) {
			sb.append("<a href='" + sqlFilename + "'>Download " + sqlFilename + "</a>");
			HtmlUtils.appendQCodeArea(sb, initSql.substring(0, Math.min(initSql.length(), 1000)));
		}

		/*
		 * Content listing at top that links to each chart
		 */
		sb.append("<div class='conListing'> <h4>Contents</h4><ol>");
		for(Entry<String, ExampleChartQuery> e : charts.entrySet()) {
			ExampleChartQuery ecq = e.getValue();
			String lnk = ecq.getSupportedViewStrategy().getDescription() + " of " + ecq.getName();
			sb.append("<li><a href='#").append(cleanAtt(clean(ecq.getName()))).append("'>")
				.append(lnk).append("</a></li>");
		}
		sb.append("</ol></div>");
		
		
		/*
		 * Generate every chart example
		 */
		sb.append("<div id='chart-container'>").append(NL);
		for(Entry<String, ExampleChartQuery> e : charts.entrySet()) {
			
			String imgPath = e.getKey();
			ExampleChartQuery ecq = e.getValue();
			
			sb.append("<div class='qeg' id='" + cleanAtt(clean(ecq.getName())) + "'>");
			String v = ecq.getSupportedViewStrategy().getDescription() + " of ";
			sb.append("<h2>").append(v).append(ecq.getName()).append("</h2>").append(NL);
			
			HtmlUtils.appendImage(sb, imgPath, ecq.getName(), IMG_HEIGHT, IMG_WIDTH);
			
			HtmlUtils.appendQCodeArea(sb, ecq.getSqlQuery()); 
			sb.append("<p>").append(ecq.getDescription()).append("</p>").append(NL);
			sb.append("</div>").append(NL);
		}

		sb.append("</div>").append(NL);
		return sb.toString();
	}


	/**
	 * Start an example database, generate the example charts and an html
	 * describing and displaying them into the parentFolder, then stop the DB.
	 * @throws IOException 
	 */
	private static Map<String,ExampleChartQuery> generate(File parentFolder, 
			ExampleChartDB exampleChartDB) throws SQLException, IOException {
		
		Preconditions.checkArgument(parentFolder.isDirectory());
		
		// use DB runner to start the database
		DBTestRunner dbRunner;
		dbRunner = DBTestRunnerFactory.getDbRunner(exampleChartDB.getDbType());
		if(dbRunner == null) {
			throw new IllegalArgumentException("DB type not supported to run");
		}
		ConnectionManager connMan = dbRunner.start();
		final ServerConfig sc = dbRunner.getServerConfig();
		
		Map<String, ExampleChartQuery> generatedCharts = Maps.newHashMap();
		try {

			// try initializing the database
			for(String initSql : exampleChartDB.getInitSQL(false)) {
				LOG.fine("sending initSql: " + initSql.substring(0, Math.min(initSql.length(), 33)));
				boolean ranOk = connMan.execute(sc, initSql);
				if(!ranOk) {
					LOG.warning("Could not run initSQL: " + initSql);
//					throw new RuntimeException("Could not initialise exampleChartDB:" 
//							+ exampleChartDB.getName()); 
				}
			}

			// for every example use chartGenie to generate an image
			final String PRE = clean(exampleChartDB.getDbType().name()) + "-chart-";
			for(ExampleChartQuery exq : exampleChartDB.getQueries()) {
				
				String filename = PRE + clean(exq.getName()) + ".png";
						
				if(generatedCharts.containsKey(filename)) {
					LOG.severe("Filename overlap between charts within ExampleChartDb");
				}
				
				ChartParams chartParams = new ChartParamsBuilder()
					.serverConfig(sc)
					.height(IMG_HEIGHT)
					.width(IMG_WIDTH)
					.file(new File(parentFolder, filename))
					.viewStrategy(exq.getSupportedViewStrategy())
					.query(exq.getSqlQuery())
					.build();
				
				SqlChart.generate(chartParams);
				generatedCharts.put(filename, exq);
			}
			
		} finally {
			dbRunner.stop();
		}	

		return generatedCharts;
	}

}
