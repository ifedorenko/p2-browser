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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.wb.swt.ResourceManager;

abstract class InstallableUnitTreeActions
{
    private final TreeViewer treeViewer;

    private Action collapseAll;

    private Action expandAll;

    private final IViewSite viewSite;

    private IWorkbenchPartSite getSite()
    {
        return viewSite;
    }

    public IViewSite getViewSite()
    {
        return viewSite;
    }

    public InstallableUnitTreeActions( IViewSite viewSite, TreeViewer treeViewer )
    {
        this.viewSite = viewSite;
        this.treeViewer = treeViewer;

        createActions();

        initializeConextMenu();

        initializeToolBar();
    }

    private void initializeConextMenu()
    {
        Tree tree = treeViewer.getTree();
        Menu menu = new Menu( tree );
        tree.setMenu( menu );

        MenuItem mntmOpen = new MenuItem( menu, SWT.NONE );
        mntmOpen.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                openInstallableUnit();
            }
        } );
        mntmOpen.setText( "Open IU" );

        MenuItem mntmOpenDependencies = new MenuItem( menu, SWT.NONE );
        mntmOpenDependencies.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                openIncludedInstallableUnits();
            }
        } );
        mntmOpenDependencies.setText( "Open Dependencies" );

        new MenuItem( menu, SWT.SEPARATOR );

        MenuItem mntmCopy = new MenuItem( menu, SWT.NONE );
        mntmCopy.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                copyToClipboard();
            }
        } );
        mntmCopy.setText( "Copy" );

        new MenuItem( menu, SWT.SEPARATOR );

        MenuItem mntmExpand = new MenuItem( menu, SWT.NONE );
        mntmExpand.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                expandSelected();
            }
        } );
        mntmExpand.setText( "Expand" );

        MenuItem mntmCollapse = new MenuItem( menu, SWT.NONE );
        mntmCollapse.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                collapseSelected();
            }
        } );
        mntmCollapse.setText( "Collapse" );
    }

    private void createActions()
    {
        collapseAll = new Action( "Collapse All" )
        {
            @Override
            public void run()
            {
                treeViewer.collapseAll();
            }
        };
        collapseAll.setImageDescriptor( ResourceManager.getPluginImageDescriptor( "com.ifedorenko.p2browser",
                                                                                  "icons/collapseall.gif" ) );

        expandAll = new Action( "Expand All" )
        {
            @Override
            public void run()
            {
                treeViewer.expandAll();
            }
        };
        expandAll.setImageDescriptor( ResourceManager.getPluginImageDescriptor( "com.ifedorenko.p2browser",
                                                                                "icons/expandall.gif" ) );
    }

    private void initializeToolBar()
    {
        IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();
        tbm.add( expandAll );
        tbm.add( collapseAll );
    }

    // private void initializeMenu()
    // {
    // IMenuManager manager = getViewSite().getActionBars().getMenuManager();
    // }

    protected void openIncludedInstallableUnits()
    {
        IWorkbenchPage activePage = getSite().getWorkbenchWindow().getActivePage();

        Collection<InstallableUnitNode> selection = getSelectedInstallableUnits();

        StringBuilder sb = new StringBuilder();
        for ( InstallableUnitNode node : selection )
        {
            sb.append( node.getInstallableUnit().getId() );
        }

        try
        {
            DependencyHierarchyView iusView =
                (DependencyHierarchyView) activePage.showView( DependencyHierarchyView.ID, sb.toString(),
                                                               IWorkbenchPage.VIEW_ACTIVATE
                                                                   | IWorkbenchPage.VIEW_CREATE );

            iusView.setMetadata( getAllInstallableUnits(), InstallableUnitNode.toInstallableUnits( selection ) );
        }
        catch ( PartInitException e1 )
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    protected abstract IQueryable<IInstallableUnit> getAllInstallableUnits();

    private Collection<InstallableUnitNode> getSelectedInstallableUnits()
    {
        ArrayList<InstallableUnitNode> units = new ArrayList<InstallableUnitNode>();

        ISelection selection = treeViewer.getSelection();
        if ( selection instanceof IStructuredSelection )
        {
            Iterator<?> iterator = ( (IStructuredSelection) selection ).iterator();
            while ( iterator.hasNext() )
            {
                Object element = iterator.next();

                if ( element instanceof InstallableUnitNode )
                {
                    units.add( (InstallableUnitNode) element );
                }
            }
        }

        return units;
    }

    protected void openInstallableUnit()
    {
        IWorkbenchPage activePage = getSite().getWorkbenchWindow().getActivePage();

        Collection<InstallableUnitNode> selection = getSelectedInstallableUnits();

        for ( InstallableUnitNode node : selection )
        {
            try
            {
                InstallableUnitView iuView =
                    (InstallableUnitView) activePage.showView( InstallableUnitView.ID,
                                                               node.getInstallableUnit().getId(),
                                                               IWorkbenchPage.VIEW_ACTIVATE
                                                                   | IWorkbenchPage.VIEW_CREATE );
                iuView.setInstallableUnit( node.getMetadata(), node.getInstallableUnit() );
            }
            catch ( PartInitException e1 )
            {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
    }

    protected void copyToClipboard()
    {
        Collection<InstallableUnitNode> selection = getSelectedInstallableUnits();

        if ( selection != null && !selection.isEmpty() )
        {
            StringBuilder sb = new StringBuilder();

            for ( InstallableUnitNode node : selection )
            {
                if ( sb.length() > 0 )
                {
                    sb.append( '\n' );
                }
                sb.append( node.getInstallableUnit().getId() );
                sb.append( '\t' );
                sb.append( node.getInstallableUnit().getVersion().toString() );
            }

            Clipboard clipboard = new Clipboard( getSite().getShell().getDisplay() );

            Transfer[] dataTypes = new Transfer[] { TextTransfer.getInstance() };
            Object[] data = new Object[] { sb.toString() };

            clipboard.setContents( data, dataTypes );
        }
    }

    protected void expandSelected()
    {
        ISelection selection = treeViewer.getSelection();
        if ( selection instanceof IStructuredSelection && !selection.isEmpty() )
        {
            Iterator<?> iterator = ( (IStructuredSelection) selection ).iterator();
            while ( iterator.hasNext() )
            {
                Object element = iterator.next();
                treeViewer.expandToLevel( element, TreeViewer.ALL_LEVELS );
            }
        }
    }

    protected void collapseSelected()
    {
        ISelection selection = treeViewer.getSelection();
        if ( selection instanceof IStructuredSelection && !selection.isEmpty() )
        {
            Iterator<?> iterator = ( (IStructuredSelection) selection ).iterator();
            while ( iterator.hasNext() )
            {
                Object element = iterator.next();
                treeViewer.collapseToLevel( element, TreeViewer.ALL_LEVELS );
            }
        }
    }

}
