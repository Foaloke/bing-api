package it.unipi.di.acube.main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.io.Resources;

import it.unipi.di.acube.BingInterface;
import it.unipi.di.acube.downloader.Downloader;

public class QueryBing {

	private static final int STUDENT_ID = 231079;
	//private static final int BING_START_INDEX = 1851;
	private static final int BING_START_INDEX = 2505;
	private static final int BING_END_INDEX = 3700;
	
	private static final int MAX_BING_RESULTS = 100;
	
	private static int count = 9269;
	
	public static void main(String[] args) throws Exception {

		if (args.length >= 2) {
			BingInterface.setCache(args[1]);
		}		
		String key = args[0];

		BingInterface bing = new BingInterface(key);
		
		String[] codeLineData;
		int codeLineIndex;
		String codeLineSearch;
		for( String codeLine : readResource("codes.txt").split("\n") ){
			codeLineData = codeLine.split("\t");
			codeLineIndex = Integer.parseInt(codeLineData[0]);
			codeLineSearch = codeLineData[1].trim();
			if( codeLineIndex>=BING_START_INDEX && codeLineIndex <= BING_END_INDEX){
				crawl(bing, codeLineSearch);
			}
		}
		
		BingInterface.flush();
		
	}
	

	private static void crawl(BingInterface bing, String searchTerm) throws Exception {
		
		JSONObject response = bing.queryBing(searchTerm, MAX_BING_RESULTS);

		JSONArray webresults
			= response
				.getJSONObject("d")
				.getJSONArray("results")
				.getJSONObject(0)
				.getJSONArray("Web");

		String url;
		String fileName;
		List<String> reportLines = new ArrayList<>();
		int successful = 0;
		for(int i = 0; (i<webresults.length()) && (successful<=15); i++) {
			
			url = ((JSONObject)webresults.get(i)).getString("Url");			
			String content = Downloader.download(url);
			
			if(content != ""){
				fileName = STUDENT_ID+"-"+(count++);
				Downloader.writeFile(Downloader.SCRAPED_DIR_PATH, "sites", fileName, content, "html");
				reportLines.add(searchTerm +"\t"+fileName+".html\t"+url);
				successful++;
			}
			
		}
		
		Downloader.appendToFile("out", "report", "tonnicchi", Joiner.on("\n").join(reportLines)+"\n", "csv");
		
	}


	private static String readResource(String sourceFile) throws IOException {
		return Resources.toString(Resources.getResource(sourceFile), Charsets.UTF_8);
	}

}
