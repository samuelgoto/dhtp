package com.kumbaya.www;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.ImmutableList;

class HealthZServlet extends HttpServlet {
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {

		List<String> keys = ImmutableList.of(
				"/dht/messages/incoming",
				"/dht/messages/outgoing",

				"/dht/messages/incoming/ping_response",
				"/dht/messages/incoming/store_response",
				"/dht/messages/incoming/find_node_response",
				"/dht/messages/incoming/find_value_response",
				"/dht/messages/incoming/ping_request",
				"/dht/messages/incoming/store_request",
				"/dht/messages/incoming/find_node_request",
				"/dht/messages/incoming/find_value_request",
				
				"/dht/messages/outgoing/ping_response",
				"/dht/messages/outgoing/store_response",
				"/dht/messages/outgoing/find_node_response",
				"/dht/messages/outgoing/find_value_response",
				"/dht/messages/outgoing/ping_request",
				"/dht/messages/outgoing/store_request",
				"/dht/messages/outgoing/find_node_request",
				"/dht/messages/outgoing/find_value_request");

		PrintWriter writer = response.getWriter();
		writer.write("<html>");
		writer.write("<head>");
		writer.write("<title>HealthZ</title>");
		writer.write("</head>");
		writer.write("<body>");
		writer.write("<h1>HealthZ</h1>");


		for (String key : keys) {
			writer.write("<iframe src='/varz" + key  + "' width='100%' height='400px'></iframe>");
		}

		writer.write("</body>");
		writer.write("</html>");

		response.flushBuffer();
	}
}
