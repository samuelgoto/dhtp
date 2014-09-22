package com.kumbaya.www;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.kumbaya.monitor.Sampler;
import com.kumbaya.monitor.Sampler.Sample;

@SuppressWarnings("serial")
class VarZGraphServlet extends HttpServlet {
	@Inject Provider<Sampler> sampler;
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {

		String varZ = request.getPathInfo();

		List<Sample> samples = sampler.get().get(varZ);
		
		PrintWriter writer = response.getWriter();

		String array = "";
		for (Sample sample : samples) {
			array += "data.push([new Date(" + sample.date().getTime() + "), " + sample.value() + "]);";
		}
		
		String html = ""+
		"<html>" +
		"  <head>" +
		"    <script type='text/javascript' src='http://dygraphs.com/1.0.1/dygraph-combined.js'></script>" +
		"  </head>" +
		"  <body>" +
		"    <div id='dygraph' style='width: 100%; height: 100%;'></div>" +
		"    <script type='text/javascript'>" +
		"      var data = [];" +
		array +
		"      g = new Dygraph(" +
		"          document.getElementById('dygraph')," +
		"          data, {" +
        "            title: '" + varZ + " '," +
        "            ylabel: 'QPS'," +
        "            legend: 'always'," +
        "            drawGrid: false," +
        "            fillGraph: true," +
        "            drawPoints: true," +
        "            animatedZooms: true," +
        "            pointSize: 4," +
        "            labelsDivStyles: { 'textAlign': 'right' }," +
        "            showRangeSelector: true" +
		"        });" +
		"    </script>" +
		"  </body>" +
		"</html>" +
		"";

		writer.write(html);
		
		response.flushBuffer();
	}
}
