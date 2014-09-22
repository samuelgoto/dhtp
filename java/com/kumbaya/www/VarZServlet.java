package com.kumbaya.www;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.kumbaya.monitor.Sampler;

@SuppressWarnings("serial")
class VarZServlet extends HttpServlet {
	@Inject Provider<Sampler> sampler;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {

		Set<String> keys = sampler.get().keys();
		
		PrintWriter writer = response.getWriter();
		writer.write("<html>");
		writer.write("<head>");
		writer.write("<title>VarZ</title>");
		writer.write("</head>");
		writer.write("<body>");
		writer.write("<h1>VarZ</h1>");

		writer.write("<table border=1>");
		writer.write("<tr><td>VarZ</td><td>Values</td></tr>");
		
		for (String key : keys) {
			writer.write("<tr>");
			writer.write("<td>");
			writer.write("<a href='/varz" + key  + "'>");
			writer.write(key);
			writer.write("<a>");
			writer.write("</td>");
			writer.write("<td> ... </td>");
			writer.write("</tr>");
		}
		writer.write("</table>");

		writer.write("</body>");
		writer.write("</html>");

		response.flushBuffer();
	}
}
