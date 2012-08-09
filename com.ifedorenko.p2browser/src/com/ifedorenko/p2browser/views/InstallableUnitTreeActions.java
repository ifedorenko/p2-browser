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

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.p2.engine.DownloadManager;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRequest;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
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
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.wb.swt.ResourceManager;

import com.ifedorenko.p2browser.Activator;

@SuppressWarnings( "restriction" )
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
                openHierarchyView( DependencyHierarchyView.ID );
            }
        } );
        mntmOpenDependencies.setText( "Open Dependencies" );

        MenuItem mntmOpenReferences = new MenuItem( menu, SWT.NONE );
        mntmOpenReferences.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                openHierarchyView( ReferenceHierarchyView.ID );
            }
        } );
        mntmOpenReferences.setText( "Open References" );

        MenuItem mntmOpenFeatures = new MenuItem( menu, SWT.NONE );
        mntmOpenFeatures.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                openHierarchyView( FeatureReferenceHierarchyView.ID );
            }
        } );
        mntmOpenFeatures.setText( "Open Including Features" );

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

        if ( getRepositoryLocations() != null )
        {
            MenuItem mntmSaveAs = new MenuItem( menu, SWT.NONE );
            mntmSaveAs.addSelectionListener( new SelectionAdapter()
            {
                @Override
                public void widgetSelected( SelectionEvent e )
                {
                    saveArtifactAs();
                }
            } );
            mntmSaveAs.setText( "Save As..." );
        }

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
                treeViewer.getTree().setRedraw( false );
                treeViewer.expandAll();
                treeViewer.getTree().setRedraw( true );
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

    protected void openHierarchyView( String viewId )
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
            AbstractInstallableUnitHierarchyView iusView =
                (AbstractInstallableUnitHierarchyView) activePage.showView( viewId, sb.toString(),
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

    protected abstract Collection<URI> getRepositoryLocations();

    private Collection<InstallableUnitNode> getSelectedInstallableUnits()
    {
        return getSelection( InstallableUnitNode.class );
    }

    protected <T> Collection<T> getSelection( Class<T> type )
    {
        ArrayList<T> result = new ArrayList<T>();

        ISelection selection = treeViewer.getSelection();
        if ( selection instanceof IStructuredSelection )
        {
            Iterator<?> iterator = ( (IStructuredSelection) selection ).iterator();
            while ( iterator.hasNext() )
            {
                Object element = iterator.next();

                if ( type.isInstance( element ) )
                {
                    result.add( type.cast( element ) );
                }
            }
        }

        return result;
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
        List<Transfer> dataTypes = new ArrayList<Transfer>();
        List<Object> data = new ArrayList<Object>();

        addToClipboard( dataTypes, data );

        Clipboard clipboard = new Clipboard( getSite().getShell().getDisplay() );

        clipboard.setContents( data.toArray(), dataTypes.toArray( new Transfer[dataTypes.size()] ) );

        clipboard.dispose();
    }

    protected void addToClipboard( List<Transfer> dataTypes, List<Object> data )
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

            dataTypes.add( TextTransfer.getInstance() );
            data.add( sb.toString() );
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

    protected void saveArtifactAs()
    {
        final Collection<InstallableUnitNode> selection = getSelectedInstallableUnits();

        String directoryPath = new DirectoryDialog( getSite().getShell() ).open();
        if ( directoryPath == null )
        {
            return;
        }

        final File directory = new File( directoryPath );
        directory.mkdirs();

        final IProvisioningAgent agent = Activator.getDefault().getProvisioningAgent();

        Job job = new Job( "Saving artifacts" )
        {
            @Override
            protected IStatus run( IProgressMonitor monitor )
            {
                try
                {
                    downloadMetadata( monitor );

                    downloadArtifacts( monitor );

                    return Status.OK_STATUS;
                }
                catch ( ProvisionException e )
                {
                    return e.getStatus();
                }
            }

            protected void downloadMetadata( IProgressMonitor monitor )
                throws ProvisionException
            {
                IMetadataRepositoryManager repoManager =
                    (IMetadataRepositoryManager) agent.getService( IMetadataRepositoryManager.SERVICE_NAME );
                IMetadataRepository targetRepository;
                try
                {
                    targetRepository = repoManager.loadRepository( directory.toURI(), monitor );
                }
                catch ( ProvisionException e )
                {
                    targetRepository =
                        repoManager.createRepository( directory.toURI(), directory.getName(),
                                                      IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY,
                                                      new HashMap<String, String>() );
                }

                Collection<IInstallableUnit> units = new ArrayList<IInstallableUnit>();
                for ( InstallableUnitNode node : selection )
                {
                    units.add( node.getInstallableUnit() );
                }

                targetRepository.addInstallableUnits( units );
            }

            protected void downloadArtifacts( IProgressMonitor monitor )
                throws ProvisionException
            {
                IArtifactRepositoryManager repoManager =
                    (IArtifactRepositoryManager) agent.getService( IArtifactRepositoryManager.SERVICE_NAME );

                IArtifactRepository targetRepository;
                try
                {
                    targetRepository = repoManager.loadRepository( directory.toURI(), monitor );
                }
                catch ( ProvisionException e )
                {
                    targetRepository =
                        repoManager.createRepository( directory.toURI(), directory.getName(),
                                                      IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY,
                                                      new HashMap<String, String>() );
                }

                // IArtifactRepository targetRepository = repoManager.loadRepository( directory.toURI(), monitor );
                ProvisioningContext ctx = new ProvisioningContext( agent );
                Collection<URI> repos = getRepositoryLocations();
                if ( repos != null )
                {
                    // ctx.setMetadataRepositories( repos.toArray( new URI[repos.size()] ) );
                    ctx.setArtifactRepositories( repos.toArray( new URI[repos.size()] ) );
                }
                DownloadManager mgr = new DownloadManager( ctx, agent );

                for ( InstallableUnitNode node : selection )
                {
                    for ( IArtifactKey key : node.getInstallableUnit().getArtifacts() )
                    {
                        IArtifactRequest request = repoManager.createMirrorRequest( key, targetRepository, null, null );
                        mgr.add( request );
                    }
                }

                mgr.start( monitor );
            }
        };
        job.setUser( true );
        job.schedule();
    }
}
