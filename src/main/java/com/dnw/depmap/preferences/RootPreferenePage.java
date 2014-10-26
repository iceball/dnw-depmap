/**
 * !(#) RootPreferenePage.java
 * Copyright (c) 2014 DNW Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     DNW Technologies - initial API and implementation
 *
 * Create by manbaum since Oct 20, 2014.
 */
package com.dnw.depmap.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.dnw.depmap.Activator;
import com.dnw.plugin.preference.GroupFieldEditor;
import com.dnw.plugin.preference.TextFieldEditor;

/**
 * <p>
 * This class represents a preference page that is contributed to the Preferences dialog. By
 * subclassing <code>FieldEditorPreferencePage</code>, we can use the field support built into JFace
 * that allows us to create a page that is small and knows how to save, restore and apply itself.
 * </p>
 * <p>
 * This page is used to modify preferences only. They are stored in the preference store that
 * belongs to the main plug-in class. That way, preferences can be accessed directly via the
 * preference store.
 * </p>
 * 
 * @author manbaum
 * @since Oct 20, 2014
 */
public class RootPreferenePage extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {

	/**
	 * Constructor of RootPreferenePage.
	 * 
	 * @author manbaum
	 * @since Oct 20, 2014
	 */
	public RootPreferenePage() {
		super(GRID);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("Settings used when generating depandency map.\n");
	}

	private GroupFieldEditor gf1;
	private BooleanFieldEditor flagurl;
	private StringFieldEditor dburl;
	private BooleanFieldEditor flagdir;
	private DirectoryFieldEditor dbdir;
	private GroupFieldEditor gf2;
	private BooleanFieldEditor flagprefer;
	private TextFieldEditor whitelist;
	private TextFieldEditor blacklist;
	private GroupFieldEditor gf3;
	private BooleanFieldEditor flagexec;
	private TextFieldEditor statements;

	/**
	 * Creates the field editors. Field editors are abstractions of the common GUI blocks needed to
	 * manipulate various types of preferences. Each field editor knows how to save and restore
	 * itself.
	 * 
	 * @author manbaum
	 * @since Oct 20, 2014
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
	 */
	public void createFieldEditors() {
		final Composite p = getFieldEditorParent();

		gf1 = new GroupFieldEditor("Neo4j graph database", p);
		flagurl = new BooleanFieldEditor(PrefKeys.P_USESTANDALONE,
				"&Standalone database server (access thru Rest API)", gf1.getGroupControl(p));
		dburl = new StringFieldEditor(PrefKeys.P_DBURL, "&Url:", gf1.getGroupControl(p));
		flagdir = new BooleanFieldEditor(PrefKeys.P_USEEMBEDDED,
				"&Embedded database server (access thru Java API)", gf1.getGroupControl(p));
		dbdir = new DirectoryFieldEditor(PrefKeys.P_DBDIR, "S&tore:", gf1.getGroupControl(p));

		gf2 = new GroupFieldEditor(
				"Class/Method name filter (a regexp or a string prefix with '@', one item each line)",
				p);
		flagprefer = new BooleanFieldEditor(PrefKeys.P_PREFERWHITE, "P&refer white list",
				gf2.getGroupControl(p));
		whitelist = new TextFieldEditor(PrefKeys.P_WHITELIST, "&Whitelist:", 50, 2,
				gf2.getGroupControl(p));
		blacklist = new TextFieldEditor(PrefKeys.P_BLACKLIST, "&Blacklist:", 50, 2,
				gf2.getGroupControl(p));

		gf3 = new GroupFieldEditor(
				"Cypher statements executed before generating (one statement each line)", p);
		flagexec = new BooleanFieldEditor(PrefKeys.P_USEPREEXEC, "Enable e&xecuting statements",
				gf3.getGroupControl(p));
		statements = new TextFieldEditor(PrefKeys.P_PREEXEC, "", 56, 6, gf3.getGroupControl(p));

		gf1.addField(flagurl);
		gf1.addField(dburl);
		gf1.addField(flagdir);
		gf1.addField(dbdir);
		addField(gf1);

		gf2.addField(flagprefer);
		gf2.addField(whitelist);
		gf2.addField(blacklist);
		addField(gf2);

		gf3.addField(flagexec);
		gf3.addField(statements);
		addField(gf3);
	}

	/**
	 * Initializes all field editors.
	 * 
	 * @author manbaum
	 * @since Oct 24, 2014
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#initialize()
	 */
	@Override
	protected void initialize() {
		super.initialize();
		Composite p = getFieldEditorParent();
		boolean value1 = flagurl.getBooleanValue();
		dburl.setEnabled(value1, gf1.getGroupControl(p));
		boolean value2 = flagurl.getBooleanValue();
		dbdir.setEnabled(!value2, gf1.getGroupControl(p));
		boolean value5 = flagexec.getBooleanValue();
		statements.setEnabled(value5, gf3.getGroupControl(p));
	}

	/**
	 * The field editor preference page implementation of this <code>IPreferencePage</code> (and
	 * <code>IPropertyChangeListener</code>) method intercepts <code>IS_VALID</code> events but
	 * passes other events on to its superclass.
	 * 
	 * @author manbaum
	 * @since Oct 23, 2014
	 * @param event the property change event object describing which property changed and how.
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		super.propertyChange(event);
		FieldEditor f = (FieldEditor)event.getSource();
		Composite p = getFieldEditorParent();
		if (event.getProperty() == FieldEditor.VALUE) {
			if (f.getPreferenceName().equals(PrefKeys.P_USESTANDALONE)) {
				boolean value = (Boolean)event.getNewValue();
				dburl.setEnabled(value, gf1.getGroupControl(p));
			} else if (f.getPreferenceName().equals(PrefKeys.P_USEEMBEDDED)) {
				boolean value = (Boolean)event.getNewValue();
				dbdir.setEnabled(value, gf1.getGroupControl(p));
			} else if (f.getPreferenceName().equals(PrefKeys.P_USEPREEXEC)) {
				boolean value = (Boolean)event.getNewValue();
				statements.setEnabled(value, gf3.getGroupControl(p));
			}
		}
	}

	/**
	 * <p>
	 * Initializes this preference page for the given workbench. This method is called automatically
	 * </p>
	 * <p>
	 * as the preference page is being created and initialized. Clients must not call this method.
	 * </p>
	 * 
	 * @author manbaum
	 * @since Oct 20, 2014
	 * @param workbench the workbench.
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}
}