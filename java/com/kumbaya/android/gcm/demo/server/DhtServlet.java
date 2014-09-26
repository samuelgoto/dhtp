/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.gcm.demo.server;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.limewire.mojito.messages.DHTMessage;
import org.limewire.mojito.messages.MessageFactory;
import org.limewire.mojito.messages.impl.DefaultMessageFactory;
import org.limewire.util.CommonUtils;

import com.google.android.gcm.server.Constants;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;

@SuppressWarnings("serial")
public class DhtServlet extends BaseServlet {
	private static final Logger logger =
			Logger.getLogger(DhtServlet.class.getName());

	static final String ATTRIBUTE_STATUS = "status";

	private Sender sender;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		sender = newSender(config);
	}

	/**
	 * Displays the existing messages and offer the option to send a new one.
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		resp.setContentType("text/html");
		PrintWriter out = resp.getWriter();
		out.write("Hello World");
		resp.setStatus(HttpServletResponse.SC_OK);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse resp)
			throws IOException {

		int length = request.getContentLength();
		byte[] data = new byte[length];
		DataInputStream dataIs = new DataInputStream(
				request.getInputStream());
		dataIs.readFully(data);
		dataIs.close();

		String hostname = request.getHeader("X-Node-Host");
		String port = request.getHeader("X-Node-Port");
		String destination = request.getHeader("X-Node-Destination");

		InetSocketAddress src = InetSocketAddress.createUnresolved(
				hostname, Integer.valueOf(port));

		logger.info("Message from: " + src + ", type: " + 
					request.getHeader("X-Node-Debug") + ", to: " +
					destination);

		String regId = Datastore.findRegIdByNodeId(destination);

		if (regId == null) {
			retryTask(resp);
			return;
		}
		
		Message message = new Message.Builder()
			.addData("debug", "message from: " + src.toString())
			.addData("X-Node-Host", hostname)
			.addData("X-Node-Port", port)
			.addData("body", new String(Base64.encodeBase64(data)))
			.build();
		
		// logger.info("Sending message to " + regId);

		sendSingleMessage(regId, message, resp);
		

		// doGet(request, resp);
	}

	/**
	 * Creates the {@link Sender} based on the servlet settings.
	 */
	protected Sender newSender(ServletConfig config) {
		String key = (String) config.getServletContext()
				.getAttribute(ApiKeyInitializer.ATTRIBUTE_ACCESS_KEY);
		return new Sender(key);
	}

	private Message createMessage() {
		Message message = new Message.Builder()
			.addData("foo", "bar")
			.build();
		return message;
	}

	private void sendSingleMessage(String regId, Message message, HttpServletResponse resp) {
		// logger.info("Sending message to device " + regId);
		Result result;
		try {
			result = sender.sendNoRetry(message, regId);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Exception posting " + message, e);
			taskDone(resp);
			return;
		}
		if (result == null) {
			retryTask(resp);
			return;
		}
		if (result.getMessageId() != null) {
			logger.info("Succesfully sent message to device " + regId);
			String canonicalRegId = result.getCanonicalRegistrationId();
			if (canonicalRegId != null) {
				// same device has more than on registration id: update it
				logger.finest("canonicalRegId " + canonicalRegId);
				Datastore.updateRegistration(regId, canonicalRegId);
			}
		} else {
			String error = result.getErrorCodeName();
			if (error.equals(Constants.ERROR_NOT_REGISTERED)) {
				// application has been removed from device - unregister it
				Datastore.unregister(regId);
			} else {
				logger.severe("Error sending message to device " + regId
						+ ": " + error);
			}
		}
	}

	/**
	 * Indicates to App Engine that this task should be retried.
	 */
	private void retryTask(HttpServletResponse resp) {
		resp.setStatus(500);
	}

	/**
	 * Indicates to App Engine that this task is done.
	 */
	private void taskDone(HttpServletResponse resp) {
		resp.setStatus(200);
	}

}
