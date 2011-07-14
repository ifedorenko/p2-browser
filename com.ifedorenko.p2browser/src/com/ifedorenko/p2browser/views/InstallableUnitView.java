/*******************************************************************************
 * Copyright (c) 2011 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/

package com.ifedorenko.p2browser.views;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collections;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.io.IUSerializer;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.ViewPart;

import com.ifedorenko.p2browser.model.IGroupedInstallableUnits;

public class InstallableUnitView
    extends ViewPart
{

    public static final String ID = "com.ifedorenko.p2browser.views.InstallableUnitView"; //$NON-NLS-1$

    private final FormToolkit toolkit = new FormToolkit( Display.getCurrent() );

    private IGroupedInstallableUnits metadata;

    private IInstallableUnit installableUnit;

    private Text xmlText;

    public InstallableUnitView()
    {
    }

    /**
     * Create contents of the view part.
     * 
     * @param parent
     */
    @Override
    public void createPartControl( Composite parent )
    {
        Composite container = toolkit.createComposite( parent, SWT.NONE );
        toolkit.paintBordersFor( container );
        container.setLayout( new FillLayout( SWT.HORIZONTAL ) );

        CTabFolder tabFolder = new CTabFolder( container, SWT.BORDER | SWT.BOTTOM );
        toolkit.adapt( tabFolder );
        toolkit.paintBordersFor( tabFolder );
        tabFolder.setSelectionBackground( Display.getCurrent().getSystemColor( SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT ) );

        CTabItem tbtmXml = new CTabItem( tabFolder, SWT.NONE );
        tbtmXml.setText( "XML" );

        Composite composite = new Composite( tabFolder, SWT.NONE );
        tbtmXml.setControl( composite );
        toolkit.paintBordersFor( composite );
        composite.setLayout( new FillLayout( SWT.HORIZONTAL ) );

        xmlText = new Text( composite, SWT.READ_ONLY | SWT.H_SCROLL | SWT.V_SCROLL | SWT.CANCEL | SWT.MULTI );
        xmlText.setEditable( false );
        toolkit.adapt( xmlText, true, true );

        tabFolder.setSelection( tbtmXml );

        createActions();
        initializeToolBar();
        initializeMenu();
    }

    public void dispose()
    {
        toolkit.dispose();
        super.dispose();
    }

    /**
     * Create the actions.
     */
    private void createActions()
    {
        // Create the actions
    }

    /**
     * Initialize the toolbar.
     */
    private void initializeToolBar()
    {
        IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();
    }

    /**
     * Initialize the menu.
     */
    private void initializeMenu()
    {
        IMenuManager manager = getViewSite().getActionBars().getMenuManager();
    }

    @Override
    public void setFocus()
    {
        // Set the focus
    }

    public void setInstallableUnit( IGroupedInstallableUnits metadata, IInstallableUnit installableUnit )
    {
        this.metadata = metadata;
        this.installableUnit = installableUnit;

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try
        {
            new IUSerializer( os ).write( Collections.singletonList( installableUnit ) );
            xmlText.setText( new String( os.toByteArray(), "UTF-8" ) );
        }
        catch ( UnsupportedEncodingException e )
        {
            xmlText.setText( e.getLocalizedMessage() );
        }
    }
}
