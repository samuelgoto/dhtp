package com.kumbaya.www;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context;
import org.limewire.mojito.EntityKey;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueType;

import com.google.inject.Inject;
import com.kumbaya.dht.Dht;
import com.kumbaya.dht.Keys;
import com.kumbaya.dht.Values;

class IndexServlet extends HttpServlet {
	private static final Log log = LogFactory.getLog(IndexServlet.class);

	private static final long serialVersionUID = 1L;
	private final Context context;
	private final Dht dht;

	@Inject
	IndexServlet(Context context, Dht dht) {
		this.context = context;
		this.dht = dht;
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {

		log.info("Storing a value");

		PrintWriter writer = response.getWriter();

		String key = request.getParameter("key");
		String value = request.getParameter("value");

		try {
			dht.put(Keys.of(key), Values.of(value));
			response.sendRedirect("/" + key);
		} catch (InterruptedException e) {
			writer.write(e.getMessage());
			return;
		} catch (ExecutionException e) {
			writer.write(e.getMessage());
			return;
		}
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {

		PrintWriter writer = response.getWriter();
		writer.write("<html>");
		writer.write("<body>");
		String path = request.getRequestURI();
		if (!"/".equals(path) && !"/favicon.ico".equals(path)) {
			writer.write("<pre>");
			try {
				log.info("Getting a value");
				EntityKey key = EntityKey.createEntityKey(
						Keys.of(path.substring(1)), DHTValueType.TEXT);
				List<DHTValueEntity> result = dht.get(key, 200);
				writer.write(result.toString());
				log.info("Done");
			} catch (InterruptedException e) {
				writer.write(e.toString());
				log.error(e);
			} catch (ExecutionException e) {
				writer.write(e.toString());
				log.error(e);
			} catch (TimeoutException e) {
				writer.write(e.toString());
				log.error(e);
			}
			writer.write("</pre>");
		}
		writer.write("<pre>");
		writer.write("Local node:\n\n");
		writer.write(context.toString());
		writer.write("\n");
		writer.write("Routing table:\n\n");
		writer.write(context.getRouteTable().toString());
		writer.write("\n");
		writer.write("Database:\n\n");
		writer.write(context.getDatabase().toString());
		writer.write("</pre>");
		writer.write("<br>");
		writer.write("<form method='post'>");
		writer.write("  key: <input type='text' name='key'>");
		writer.write("  value: <input type='text' name='value'>");
		writer.write("  <input type='submit' value='create'>");
		writer.write("</form>");
		writer.write("</body>");
		writer.write("</html>");

		response.flushBuffer();
	}
}
