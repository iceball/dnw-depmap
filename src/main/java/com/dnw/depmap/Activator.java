/**
 * !(#) Activator.java
 * Copyright (c) 2014 DNW Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     DNW Technologies - initial API and implementation
 *
 * Create by manbaum since Sep 29, 2014.
 */
package com.dnw.depmap;

import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.dnw.depmap.ast.MethodDeclarationVisitor;
import com.dnw.depmap.ast.MethodInvocationVisitor;
import com.dnw.depmap.ast.TypeDeclarationVisitor;
import com.dnw.depmap.neo.NeoDao;
import com.dnw.depmap.neo.NeoWriter;
import com.dnw.depmap.preferences.PrefKeys;
import com.dnw.depmap.resource.JavaFileVisitor;
import com.dnw.matcher.CompositeList;
import com.dnw.matcher.RegexMatcher;
import com.dnw.matcher.StringMatcher;
import com.dnw.neo.EmbeddedNeoAccessor;
import com.dnw.neo.NeoAccessor;
import com.dnw.neo.RestfulNeoAccessor;
import com.dnw.plugin.ast.GeneralVisitorRegistry;
import com.dnw.plugin.ast.IVisitorDelegator;
import com.dnw.plugin.ast.IVisitorRegistry;
import com.dnw.plugin.ast.NodeTypeBitMapSet;
import com.dnw.plugin.ast.RegistryBasedVisitorDelegator;
import com.dnw.plugin.resource.FileExtResourceVisitorFactory;
import com.dnw.plugin.util.ConsoleUtil;

/**
 * The activator class controls the plug-in life cycle.
 * 
 * @author manbaum
 * @since Sep 29, 2014
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.dnw.depmap";
	// The directory locates the Neo4j database store.
	private static String DBPATH = "/Users/manbaum/workspace/neo4j-community-2.1.5/data/graph.db";
	private static String DBURL = "http://localhost:7474/db/data";
	private static boolean useEmbedded = false;

	// The shared instance.
	private static Activator plugin;

	// The plug-in's console.
	public static final ConsoleUtil console = ConsoleUtil.getConsole(PLUGIN_ID);
	// This factory defines what kinds of files will be inspected in a project.
	// For now, we only support .java files.
	public static final FileExtResourceVisitorFactory factory = new FileExtResourceVisitorFactory();
	// The white list to limit what packages or classes should be focused.
	public static final CompositeList<String> filter = new CompositeList<String>();

	// The AST node type set defines a stop set. (not used now)
	// All nodes with its type in this set will be ignored, do not traverse it to improve the performance.
	public static final NodeTypeBitMapSet stopSet = new NodeTypeBitMapSet();
	// Register the corresponding AST visitor to visit the AST.
	public static final IVisitorRegistry registry = new GeneralVisitorRegistry();
	// Apply the stop set on the visitors.
	public static final IVisitorDelegator delegator = new RegistryBasedVisitorDelegator(registry,
			stopSet);

	// TODO: the following setting should be put in a Eclipse preference page.
	static {
		factory.registerVisitor("java", JavaFileVisitor.class);
		// factory.registerVisitor("xml", XmlFileVisitor.class);

		// filter.addAllowMatcher(new StringMatcher("java.lang.Object"));
		// filter.addAllowMatcher(new RegexMatcher("org\\.eclipse\\.jdt\\.core\\.dom\\..*"));
		// filter.addAllowMatcher(new RegexMatcher("com\\.dnw\\..*"));

		registry.add(TypeDeclaration.class, new TypeDeclarationVisitor());
		registry.add(MethodDeclaration.class, new MethodDeclarationVisitor());
		registry.add(MethodInvocation.class, new MethodInvocationVisitor());
	}

	// Neo4j database Cypher language executor.
	public NeoAccessor accessor;
	// Call Neo4j accessor to generate all AST nodes and its relations.
	public NeoDao neo;
	// If it's true, the database will be cleaned before AST traverse. 
	public static boolean preExec = false;
	public static String[] statements = new String[0];

	public IPropertyChangeListener listener;

	/**
	 * Constructor of Activator.
	 * 
	 * @author manbaum
	 * @since Sep 29, 2014
	 */
	public Activator() {
		listener = new IPropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent event) {
				loadPerference();
			}
		};
	}

	private void loadPerference() {
		IPreferenceStore store = super.getPreferenceStore();
		if (store.getBoolean(PrefKeys.P_USESTANDALONE)) {
			useEmbedded = false;
			DBURL = store.getString(PrefKeys.P_DBURL);
		} else if (store.getBoolean(PrefKeys.P_USEEMBEDDED)) {
			useEmbedded = true;
			DBPATH = store.getString(PrefKeys.P_DBDIR);
		} else {
			useEmbedded = false;
			DBURL = "http://localhost:7474/db/data";
			store.setValue(PrefKeys.P_USESTANDALONE, true);
			store.setValue(PrefKeys.P_DBURL, DBURL);
		}

		filter.clearAllows();
		if (store.getBoolean(PrefKeys.P_USEWHITE)) {
			String white = store.getString(PrefKeys.P_WHITELIST);
			if (white != null) {
				String[] list = white.split("\\s*;\\s*");
				for (String s : list) {
					console.println("allow: \"" + s + "\"");
					if (s.startsWith("@"))
						filter.addAllowMatcher(new StringMatcher(s.substring(1)));
					else
						filter.addAllowMatcher(new RegexMatcher(s));
				}
			}
		}

		filter.clearBlocks();
		if (store.getBoolean(PrefKeys.P_USEBLACK)) {
			String black = store.getString(PrefKeys.P_BLACKLIST);
			if (black != null) {
				String[] list = black.split("\\s*;\\s*");
				for (String s : list) {
					console.println("block: \"" + s + "\"");
					if (s.startsWith("@"))
						filter.addBlockMatcher(new StringMatcher(s.substring(1)));
					else
						filter.addBlockMatcher(new RegexMatcher(s));
				}
			}
		}

		preExec = store.getBoolean(PrefKeys.P_USEPREEXEC);
		if (preExec) {
			String ss = store.getString(PrefKeys.P_PREEXEC);
			if (ss != null) {
				statements = ss.split("\\s*;\\s*");
				for (String s : statements) {
					console.println("statement: \"" + s + "\"");
				}
			} else {
				preExec = false;
			}
		}

		accessor = useEmbedded ? new EmbeddedNeoAccessor(DBPATH) : new RestfulNeoAccessor(DBURL);
		neo = new NeoDao(new NeoWriter(accessor), filter);
	}

	/**
	 * Starts up this plug-in.
	 * 
	 * @author manbaum
	 * @since Sep 29, 2014
	 * @param context the bundle context for this plug-in.
	 * @throws Exception if this plug-in did not start up properly.
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		loadPerference();
		super.getPreferenceStore().addPropertyChangeListener(listener);
	}

	/**
	 * Stops this plug-in.
	 * 
	 * @author manbaum
	 * @since Sep 29, 2014
	 * @param context the bundle context for this plug-in.
	 * @throws Exception if this plug-in did not stopped properly.
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		super.getPreferenceStore().removePropertyChangeListener(listener);
		accessor.shutdown();
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance.
	 * 
	 * @author manbaum
	 * @since Sep 29, 2014
	 * @return the shared instance.
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Exposes the database operations.
	 * 
	 * @author manbaum
	 * @since Oct 13, 2014
	 * @return the DAO of Neo4j database.
	 */
	public static NeoDao neo() {
		return plugin.neo;
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in relative path. The path
	 * must not have a leading "." or path separator. Clients should use a path like
	 * "icons/mysample.gif" rather than "./icons/mysample.gif" or "/icons/mysample.gif".
	 * 
	 * @author manbaum
	 * @since Sep 29, 2014
	 * @param path the path.
	 * @return the image descriptor.
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
}
