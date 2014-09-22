package com.kumbaya.www;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
class ThreadZServlet extends HttpServlet {
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {

		PrintWriter writer = response.getWriter();
		writer.write("<html>");
		writer.write("<head>");
		writer.write("<title>Threads</title>");
		writer.write("</head>");
		writer.write("<body>");
		writer.write("<h1>Threads</h1>");

		Set<Thread> threads = Thread.getAllStackTraces().keySet();
		for (Thread thread : threads) {
			writer.write("<div style='margin-top: 30px;'>");
			writer.write("<p><b>" + thread.getName() + ": " + thread.getState() + "</b></p>");        
			for (StackTraceElement stackTrace : thread.getStackTrace()) {
				writer.write("<p style='margin-left: 50px;'>" + stackTrace.toString() + "</p>");
			}
			writer.write("</div>");
		}

		writer.write("</body>");
		writer.write("</html>");

		response.flushBuffer();
	}
}
