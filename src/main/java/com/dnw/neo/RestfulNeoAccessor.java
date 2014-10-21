/**
 * !(#) RestfulNeoAccessor.java
 * Copyright (c) 2014 DNW Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     DNW Technologies - initial API and implementation
 *
 * Create by manbaum since Oct 12, 2014.
 */
package com.dnw.neo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.dnw.depmap.Activator;
import com.dnw.json.L;
import com.dnw.json.M;

/**
 * Accessor of the Neo4j graph database using REST API.
 * 
 * @author manbaum
 * @since Oct 12, 2014
 */
public class RestfulNeoAccessor implements NeoAccessor {

	private final Client client;
	private final WebTarget rootTarget;

	/**
	 * Constructor of RestfulNeoAccessor.
	 * 
	 * @author manbaum
	 * @since Oct 21, 2014
	 * @param serverRoot
	 */
	public RestfulNeoAccessor(String rootUrl) {
		client = ClientBuilder.newBuilder().build();
		rootTarget = client.target(rootUrl);
	}

	/**
	 * Method sendRequest.
	 * 
	 * @author manbaum
	 * @since Oct 21, 2014
	 * @param target
	 * @param data
	 * @return
	 */
	private Response sendRequest(WebTarget target, M data) {
		Response response = target.request().accept(MediaType.APPLICATION_JSON)
				.acceptEncoding("UTF-8").post(Entity.json(data.json()));
		if (response.getStatus() != 200) {
			throw new IllegalStateException("error.response: " + response.getStatusInfo());
		}
		InputStream is = (InputStream)response.getEntity();
		if (is != null) {
			StringBuffer sb = new StringBuffer();
			BufferedReader r = new BufferedReader(new InputStreamReader(is));
			while (true) {
				try {
					String line = r.readLine();
					if (line == null)
						break;
					sb.append(line);
				} catch (IOException e) {
					Activator.console.println(e);
					break;
				}
			}
			Activator.console.println(">>> " + sb.toString());
		}
		return response;
	}

	/**
	 * Overrider method execute.
	 * 
	 * @author manbaum
	 * @since Oct 18, 2014
	 * @param statement
	 * @see com.dnw.neo.NeoAccessor#execute(java.lang.String)
	 */
	@Override
	public void execute(String statement) {
		M s = M.m().a("statement", statement);
		M m = M.m().a("statements", L.l().a(s));
		WebTarget target = rootTarget.path("transaction/commit");
		sendRequest(target, m);
	}

	/**
	 * Executes a cypher query.
	 * 
	 * @author manbaum
	 * @since Oct 12, 2014
	 * @param statement the cypher statement.
	 * @param params the query parameters.
	 * @see com.dnw.neo.NeoAccessor#execute(java.lang.String, com.dnw.json.M)
	 */
	@Override
	public void execute(String statement, M params) {
		M s = M.m().a("statement", statement).a("parameters", params);
		M m = M.m().a("statements", L.l().a(s));
		WebTarget target = rootTarget.path("transaction/commit");
		sendRequest(target, m);
	}

	/**
	 * Startup the embedded database server.
	 * 
	 * @author manbaum
	 * @since Oct 12, 2014
	 * @see com.dnw.neo.NeoAccessor#startup()
	 */
	@Override
	public void startup() {
	}

	/**
	 * Shutdown the embedded database server.
	 * 
	 * @author manbaum
	 * @since Oct 12, 2014
	 * @see com.dnw.neo.NeoAccessor#shutdown()
	 */
	@Override
	public void shutdown() {
	}

	/**
	 * Adds a hook to JVM, shutdown the embedded database server when the JVM shutdown.
	 * 
	 * @author manbaum
	 * @since Oct 12, 2014
	 * @see com.dnw.neo.NeoAccessor#configAutoShutdown()
	 */
	@Override
	public void configAutoShutdown() {
	}
}
