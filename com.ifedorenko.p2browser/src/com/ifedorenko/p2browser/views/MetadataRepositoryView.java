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
import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository;
import org.eclipse.equinox.internal.p2.updatesite.artifact.UpdateSiteArtifactRepository;
import org.eclipse.equinox.internal.p2.updatesite.metadata.UpdateSiteMetadataRepository;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.expression.ExpressionUtil;
import org.eclipse.equinox.p2.query.ExpressionMatchQuery;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.ViewPart;

import com.ifedorenko.p2browser.Activator;
import com.ifedorenko.p2browser.dialogs.RepositoryLocationDialog;
import com.ifedorenko.p2browser.director.InstallableUnitDAG;
import com.ifedorenko.p2browser.model.IGroupedInstallableUnits;
import com.ifedorenko.p2browser.model.IncludedInstallableUnits;
import com.ifedorenko.p2browser.model.InstallableUnitDependencyTree;
import com.ifedorenko.p2browser.model.UngroupedInstallableUnits;
import com.ifedorenko.p2browser.model.match.IInstallableUnitMatcher;
import com.ifedorenko.p2browser.views.InstallableUnitFilterComposite.IFilterChangeListener;

@SuppressWarnings( "restriction" )
public class MetadataRepositoryView
    extends ViewPart
{

    public static final String ID = "com.ifedorenko.p2browser.views.MetadataRepositoryView"; //$NON-NLS-1$

    private final FormToolkit toolkit = new FormToolkit( Display.getCurrent() );

    /**
     * Root repository URIs explicitly added by the user
     */
    private final Set<URI> repositories = Collections.synchronizedSet( new LinkedHashSet<URI>() );

    /**
     * All repositories, including children of composite repositories
     */
    private final Map<URI, IMetadataRepository> allrepositories =
        Collections.synchronizedMap( new LinkedHashMap<URI, IMetadataRepository>() );

    private final Map<URI, IGroupedInstallableUnits> repositoryContent =
        Collections.synchronizedMap( new HashMap<URI, IGroupedInstallableUnits>() );

    private boolean revealCompositeRepositories = true;

    private boolean groupIncludedIUs = false;

    private TreeViewer treeViewer;

    private IInstallableUnitMatcher unitMatcher;

    private class LabelProvider
        extends InstallableUnitLabelProvider
        implements IFontProvider
    {

        @Override
        public Font getFont( Object element )
        {
            if ( unitMatcher != null )
            {
                IInstallableUnit unit = toInstallableUnit( element );
                if ( unit != null && unitMatcher.match( unit ) )
                {
                    return boldFont;
                }
            }
            return null;
        }

        @Override
        public String getText( Object element )
        {
            if ( element instanceof IRepository<?> )
            {
                String prefix = "";
                if ( element instanceof UpdateSiteMetadataRepository || element instanceof UpdateSiteArtifactRepository )
                {
                    prefix = "[PRE-P2 COMPAT] ";
                }

                return prefix + ( (IRepository<?>) element ).getLocation().toString();
            }
            return super.getText( element );
        }

    }

    private Job refreshTreeJob = new Job( "Refresh" )
    {
        @Override
        protected IStatus run( IProgressMonitor monitor )
        {
            repositoryContent.clear();
            for ( URI location : repositories )
            {
                loadRepositoryContent( repositoryContent, location, monitor );
            }

            refreshTreeInDisplayThread();

            return Status.OK_STATUS;
        }
    };

    private Font boldFont;

    public MetadataRepositoryView()
    {
    }

    @Override
    public void createPartControl( Composite parent )
    {
        FillLayout fillLayout = (FillLayout) parent.getLayout();
        fillLayout.type = SWT.VERTICAL;
        Composite container = toolkit.createComposite( parent, SWT.NONE );
        toolkit.paintBordersFor( container );
        GridLayout gl_container = new GridLayout( 2, false );
        gl_container.marginHeight = 0;
        gl_container.marginWidth = 0;
        container.setLayout( gl_container );
        {
            // Composite filterComposite = new Composite( container, SWT.NONE );
            final InstallableUnitFilterComposite filterComposite =
                new InstallableUnitFilterComposite( container, SWT.NONE );
            GridData gd_filterComposite = new GridData( SWT.FILL, SWT.CENTER, true, false, 1, 1 );
            gd_filterComposite.horizontalIndent = 5;
            filterComposite.setLayoutData( gd_filterComposite );
            filterComposite.addFilterChangeListener( new IFilterChangeListener()
            {
                @Override
                public void filterChanged( EventObject event )
                {
                    unitMatcher = filterComposite.getMatcher();
                    refreshTreeJob.schedule( 500L );
                }
            } );
        }
        {
            Composite composite = new Composite( container, SWT.NONE );
            composite.setLayoutData( new GridData( SWT.LEFT, SWT.FILL, false, true, 1, 2 ) );
            toolkit.adapt( composite );
            toolkit.paintBordersFor( composite );
            GridLayout gl_composite = new GridLayout( 1, false );
            gl_composite.marginHeight = 0;
            gl_composite.marginWidth = 0;
            composite.setLayout( gl_composite );
            {
                Label lblView = new Label( composite, SWT.NONE );
                toolkit.adapt( lblView, true, true );
                lblView.setText( "Repositories" );
            }
            {
                Button btnAdd = new Button( composite, SWT.NONE );
                btnAdd.addSelectionListener( new SelectionAdapter()
                {
                    @Override
                    public void widgetSelected( SelectionEvent e )
                    {
                        RepositoryLocationDialog dialog = new RepositoryLocationDialog( getSite().getShell() );
                        if ( dialog.open() == IDialogConstants.OK_ID )
                        {
                            addRepository( dialog.getLocation() );
                        }
                    }
                } );
                GridData gd_btnAdd = new GridData( SWT.FILL, SWT.TOP, false, false, 1, 1 );
                gd_btnAdd.horizontalIndent = 10;
                btnAdd.setLayoutData( gd_btnAdd );
                toolkit.adapt( btnAdd, true, true );
                btnAdd.setText( "Add..." );
            }
            {
                Button btnRemove = new Button( composite, SWT.NONE );
                GridData gd_btnRemove = new GridData( SWT.FILL, SWT.CENTER, false, false, 1, 1 );
                gd_btnRemove.horizontalIndent = 10;
                btnRemove.setLayoutData( gd_btnRemove );
                toolkit.adapt( btnRemove, true, true );
                btnRemove.setText( "Remove" );
            }
            {
                Button btnReloadAll = new Button( composite, SWT.NONE );
                btnReloadAll.addSelectionListener( new SelectionAdapter()
                {
                    @Override
                    public void widgetSelected( SelectionEvent e )
                    {
                        reloadAllRepositories();
                    }
                } );
                GridData gd_btnReloadAll = new GridData( SWT.FILL, SWT.CENTER, false, false, 1, 1 );
                gd_btnReloadAll.horizontalIndent = 10;
                btnReloadAll.setLayoutData( gd_btnReloadAll );
                toolkit.adapt( btnReloadAll, true, true );
                btnReloadAll.setText( "Reload all" );
            }
            {
                Button btnSaveAs = new Button( composite, SWT.NONE );
                btnSaveAs.addSelectionListener( new SelectionAdapter()
                {
                    @Override
                    public void widgetSelected( SelectionEvent e )
                    {
                        saveAsRepository();
                    }
                } );
                GridData gd_btnSaveAs = new GridData( SWT.FILL, SWT.CENTER, false, false, 1, 1 );
                gd_btnSaveAs.horizontalIndent = 10;
                btnSaveAs.setLayoutData( gd_btnSaveAs );
                toolkit.adapt( btnSaveAs, true, true );
                btnSaveAs.setText( "Save As..." );
            }
            {
                Label lblView = new Label( composite, SWT.NONE );
                toolkit.adapt( lblView, true, true );
                lblView.setText( "View" );
            }
            {
                final Button btnGroupIncluded = new Button( composite, SWT.CHECK );
                GridData gd_btnGroupIncluded = new GridData( SWT.LEFT, SWT.CENTER, false, false, 1, 1 );
                gd_btnGroupIncluded.horizontalIndent = 10;
                btnGroupIncluded.setLayoutData( gd_btnGroupIncluded );
                btnGroupIncluded.addSelectionListener( new SelectionAdapter()
                {
                    @Override
                    public void widgetSelected( SelectionEvent e )
                    {
                        groupIncludedIUs = btnGroupIncluded.getSelection();
                        refreshTreeJob.schedule( 500L );
                    }
                } );
                btnGroupIncluded.setSelection( groupIncludedIUs );
                toolkit.adapt( btnGroupIncluded, true, true );
                btnGroupIncluded.setText( "Group included" );
            }
            {
                final Button btnChildRepositories = new Button( composite, SWT.CHECK );
                GridData gd_btnChildRepositories = new GridData( SWT.LEFT, SWT.CENTER, false, false, 1, 1 );
                gd_btnChildRepositories.horizontalIndent = 10;
                btnChildRepositories.setLayoutData( gd_btnChildRepositories );
                btnChildRepositories.setToolTipText( "Reveal composite repository structure" );
                btnChildRepositories.addSelectionListener( new SelectionAdapter()
                {
                    @Override
                    public void widgetSelected( SelectionEvent e )
                    {
                        revealCompositeRepositories = btnChildRepositories.getSelection();
                        refreshTreeJob.schedule( 500L );
                    }
                } );
                btnChildRepositories.setSelection( revealCompositeRepositories );
                toolkit.adapt( btnChildRepositories, true, true );
                btnChildRepositories.setText( "Child repositories" );
            }
        }
        {
            treeViewer = new TreeViewer( container, SWT.BORDER | SWT.MULTI | SWT.VIRTUAL );
            treeViewer.setUseHashlookup( true );
            Tree tree = treeViewer.getTree();
            tree.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true, 1, 1 ) );
            tree.setLinesVisible( true );
            treeViewer.setLabelProvider( new LabelProvider() );
            treeViewer.setContentProvider( new InstallableUnitContentProvider( treeViewer )
            {
                @Override
                protected Object[] getChildren( Object parentElement )
                {
                    if ( parentElement == repositories )
                    {
                        List<Object> result = new ArrayList<Object>();
                        for ( URI location : repositories )
                        {
                            IMetadataRepository repository = allrepositories.get( location );
                            if ( repository != null )
                            {
                                result.add( repository );
                            }
                            else
                            {
                                result.add( location );
                            }
                        }
                        return toMetadataRepositories( repositories ).toArray();
                    }
                    else if ( revealCompositeRepositories && parentElement instanceof CompositeMetadataRepository )
                    {
                        return getImmediateChildrenRepositories( (CompositeMetadataRepository) parentElement );
                    }
                    else if ( parentElement instanceof IMetadataRepository )
                    {
                        final IMetadataRepository repo = (IMetadataRepository) parentElement;
                        final IGroupedInstallableUnits content = repositoryContent.get( repo.getLocation() );

                        if ( content != null )
                        {
                            return toViewNodes( content, content.getRootIncludedInstallableUnits() );
                        }

                        return null;
                    }
                    return super.getChildren( parentElement );
                }
            } );
            treeViewer.setInput( repositories );
            treeViewer.getTree().setItemCount( repositories.size() );
            toolkit.paintBordersFor( tree );

            Font font = tree.getFont();
            FontData[] fontDatas = font.getFontData();
            for ( FontData fontData : fontDatas )
            {
                fontData.setStyle( SWT.BOLD );
            }
            boldFont = new Font( tree.getDisplay(), fontDatas );
        }

        new InstallableUnitTreeActions( getViewSite(), treeViewer )
        {
            @Override
            protected IQueryable<IInstallableUnit> getAllInstallableUnits()
            {
                return toQueryable( toMetadataRepositories( repositories ) );
            }

            @Override
            protected Collection<URI> getRepositoryLocations()
            {
                return repositories;
            }

            @Override
            protected void addToClipboard( List<Transfer> dataTypes, List<Object> data )
            {
                Collection<IMetadataRepository> repositories = getSelection( IMetadataRepository.class );

                for ( IMetadataRepository repository : repositories )
                {
                    dataTypes.add( TextTransfer.getInstance() );
                    data.add( repository.getLocation().toString() );
                }

                super.addToClipboard( dataTypes, data );
            }
        };
    }

    protected void saveAsRepository()
    {
        DirectoryDialog fd = new DirectoryDialog( getViewSite().getShell() );
        final String path = fd.open();
        Job job = new Job( "Saving repository" )
        {
            @Override
            protected IStatus run( IProgressMonitor monitor )
            {
                try
                {
                    File directory = new File( path );
                    directory.mkdirs();

                    saveInstallableUnitsMetadata( directory, monitor );

                    saveArtifactsMeatdata( directory, monitor );

                    return Status.OK_STATUS;
                }
                catch ( ProvisionException e )
                {
                    return e.getStatus();
                }
                catch ( OperationCanceledException e )
                {
                    return Status.CANCEL_STATUS;
                }
            }
        };
        job.setUser( true );
        job.schedule();
    }

    protected void reloadAllRepositories()
    {
        Job job = new Job( "Reload repository metadata" )
        {
            @Override
            protected IStatus run( IProgressMonitor monitor )
            {
                List<IStatus> errors = new ArrayList<IStatus>();
                try
                {
                    final Map<URI, IMetadataRepository> allrepositories = new LinkedHashMap<URI, IMetadataRepository>();

                    final Map<URI, IGroupedInstallableUnits> repositoryContent =
                        new LinkedHashMap<URI, IGroupedInstallableUnits>();

                    IMetadataRepositoryManager repoMgr = Activator.getRepositoryManager();

                    for ( URI location : MetadataRepositoryView.this.repositories )
                    {
                        loadRepository( repoMgr, allrepositories, location, true, errors, monitor );
                        loadRepositoryContent( repositoryContent, location, monitor );
                    }

                    MetadataRepositoryView.this.allrepositories.clear();
                    MetadataRepositoryView.this.allrepositories.putAll( allrepositories );

                    MetadataRepositoryView.this.repositoryContent.clear();
                    MetadataRepositoryView.this.repositoryContent.putAll( repositoryContent );

                    refreshTreeInDisplayThread();
                }
                catch ( ProvisionException e )
                {
                    errors.add( e.getStatus() );
                }
                catch ( OperationCanceledException e )
                {
                    return Status.CANCEL_STATUS;
                }
                return toStatus( errors );
            }
        };
        job.setUser( true );
        job.schedule();
    }

    private IQueryable<IInstallableUnit> toQueryable( Collection<IMetadataRepository> repositories )
    {
        return QueryUtil.compoundQueryable( repositories );
    }

    protected Collection<IMetadataRepository> toMetadataRepositories( Collection<URI> locations )
    {
        List<IMetadataRepository> result = new ArrayList<IMetadataRepository>();

        for ( URI location : locations )
        {
            IMetadataRepository repository = allrepositories.get( location );
            if ( repository != null )
            {
                result.add( repository );
            }
        }

        return result;
    }

    protected void addRepository( final URI location )
    {
        Job job = new Job( "Load repository metadata" )
        {
            @Override
            protected IStatus run( IProgressMonitor monitor )
            {
                List<IStatus> errors = new ArrayList<IStatus>();

                try
                {
                    IMetadataRepositoryManager repoMgr = Activator.getRepositoryManager();

                    repositories.add( location );

                    loadRepository( repoMgr, allrepositories, location, false, errors, monitor );

                    loadRepositoryContent( repositoryContent, location, monitor );
                }
                catch ( ProvisionException e )
                {
                    errors.add( e.getStatus() );
                }
                catch ( OperationCanceledException e )
                {
                    repositories.remove( location );

                    return Status.CANCEL_STATUS;
                }

                refreshTreeInDisplayThread();

                return toStatus( errors );
            }
        };
        job.setUser( true );
        job.schedule();
    }

    protected Object[] getImmediateChildrenRepositories( final CompositeMetadataRepository repository )
    {
        List<Object> result = new ArrayList<Object>();

        for ( URI childUri : repository.getChildren() )
        {
            IMetadataRepository child = allrepositories.get( childUri );
            if ( child != null )
            {
                result.add( child );
            }
            else
            {
                result.add( "Missing " + childUri );
            }
        }

        return result.toArray();
    }

    public void dispose()
    {
        toolkit.dispose();
        super.dispose();
    }

    @Override
    public void setFocus()
    {
        // Set the focus
    }

    private void refreshTreeInDisplayThread()
    {
        getSite().getShell().getDisplay().asyncExec( new Runnable()
        {
            @Override
            public void run()
            {
                treeViewer.getTree().setRedraw( false );
                treeViewer.getTree().setItemCount( repositories.size() );
                treeViewer.refresh();
                if ( unitMatcher != null )
                {
                    treeViewer.expandAll();
                }
                treeViewer.getTree().setRedraw( true );
            }
        } );
    }

    private void loadRepository( IMetadataRepositoryManager repoMgr, Map<URI, IMetadataRepository> allrepositories,
                                 URI location, boolean refresh, List<IStatus> errors, IProgressMonitor monitor )
        throws ProvisionException, OperationCanceledException
    {
        if ( !allrepositories.containsKey( location ) )
        {
            try
            {
                IMetadataRepository repository;
                if ( refresh )
                {
                    repository = repoMgr.refreshRepository( location, monitor );
                }
                else
                {
                    repository = repoMgr.loadRepository( location, monitor );
                }
                allrepositories.put( location, repository );

                if ( repository instanceof CompositeMetadataRepository )
                {
                    for ( URI childUri : ( (CompositeMetadataRepository) repository ).getChildren() )
                    {
                        // composite repository refresh refreshes all child repositories. do not re-refresh children
                        // here
                        loadRepository( repoMgr, allrepositories, childUri, false, errors, monitor );
                    }
                }
            }
            catch ( ProvisionException e )
            {
                errors.add( e.getStatus() );
            }
        }
    }

    private void loadRepositoryContent( Map<URI, IGroupedInstallableUnits> repositoryContent, URI location,
                                        IProgressMonitor monitor )
    {
        IMetadataRepository repository = allrepositories.get( location );

        if ( repository == null )
        {
            // repository failed to load for some reason
            return;
        }

        if ( revealCompositeRepositories && repository instanceof CompositeMetadataRepository )
        {
            for ( URI childUri : ( (CompositeMetadataRepository) repository ).getChildren() )
            {
                loadRepositoryContent( repositoryContent, childUri, monitor );
            }
        }
        else
        {
            InstallableUnitDAG dag;
            if ( groupIncludedIUs )
            {
                dag = new IncludedInstallableUnits().toInstallableUnitDAG( repository, monitor );
            }
            else
            {
                dag = new UngroupedInstallableUnits().toInstallableUnitDAG( repository, monitor );
            }

            if ( unitMatcher != null )
            {
                dag = dag.filter( unitMatcher, groupIncludedIUs );
            }

            dag = dag.sort( new InstallableUnitComparator() );

            repositoryContent.put( location, new InstallableUnitDependencyTree( dag ) );
        }
    }

    protected IStatus toStatus( List<IStatus> errors )
    {
        if ( errors.isEmpty() )
        {
            return Status.OK_STATUS;
        }
        else if ( errors.size() == 1 )
        {
            return errors.get( 0 );
        }
        else
        {
            MultiStatus status =
                new MultiStatus( Activator.PLUGIN_ID, -1, errors.toArray( new IStatus[errors.size()] ),
                                 "Problems loading repository", null );
            return status;
        }
    }

    void saveInstallableUnitsMetadata( File directory, IProgressMonitor monitor )
        throws ProvisionException
    {
        IMetadataRepositoryManager repoMgr = Activator.getRepositoryManager();

        IMetadataRepository target =
            repoMgr.createRepository( directory.toURI(), directory.getName(),
                                      IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, new HashMap<String, String>() );

        Set<IInstallableUnit> units = new LinkedHashSet<IInstallableUnit>();

        final Collection<IMetadataRepository> repositories = toMetadataRepositories( this.repositories );

        for ( IMetadataRepository repository : repositories )
        {
            units.addAll( repository.query( QueryUtil.ALL_UNITS, monitor ).toUnmodifiableSet() );
        }

        target.addInstallableUnits( units );
    }

    void saveArtifactsMeatdata( File directory, IProgressMonitor monitor )
        throws ProvisionException
    {
        final IProvisioningAgent provisioningAgent = Activator.getDefault().getProvisioningAgent();

        final IArtifactRepositoryManager repoMgr =
            (IArtifactRepositoryManager) provisioningAgent.getService( IArtifactRepositoryManager.SERVICE_NAME );

        final IArtifactRepository target =
            repoMgr.createRepository( directory.toURI(), directory.getName(),
                                      IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, new HashMap<String, String>() );

        final Set<IArtifactDescriptor> artifacts = new LinkedHashSet<IArtifactDescriptor>();

        final ExpressionMatchQuery<IArtifactDescriptor> ALL_ARTIFACTS =
            new ExpressionMatchQuery<IArtifactDescriptor>( IArtifactDescriptor.class, ExpressionUtil.TRUE_EXPRESSION );

        for ( final URI location : repositories )
        {
            final IArtifactRepository repository = repoMgr.loadRepository( location, monitor );

            artifacts.addAll( repository.descriptorQueryable().query( ALL_ARTIFACTS, monitor ).toUnmodifiableSet() );
        }

        target.addDescriptors( artifacts.toArray( new IArtifactDescriptor[artifacts.size()] ), monitor );
    }

    private static String trim( String str )
    {
        if ( str == null )
        {
            return null;
        }
        str = str.trim();
        if ( "".equals( str ) )
        {
            return null;
        }
        return str;
    }

}
