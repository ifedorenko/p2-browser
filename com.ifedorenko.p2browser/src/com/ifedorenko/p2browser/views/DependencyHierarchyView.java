package com.ifedorenko.p2browser.views;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.director.Explanation;
import org.eclipse.equinox.internal.p2.director.Projector;
import org.eclipse.equinox.internal.p2.director.QueryableArray;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.part.ViewPart;

import com.ifedorenko.p2browser.model.IGroupedInstallableUnits;
import com.ifedorenko.p2browser.model.InstallableUnitDependencyTree;

import copied.org.eclipse.equinox.internal.p2.director.DependencyMesh;
import copied.org.eclipse.equinox.internal.p2.director.PermissiveSlicer;

@SuppressWarnings( "restriction" )
public class DependencyHierarchyView
    extends ViewPart
{

    public static final String ID = "com.ifedorenko.p2browser.views.DependencyHierarchyView"; //$NON-NLS-1$

    private TreeViewer hierarchyTreeViewer;

    private TableViewer listTableViewer;

    private static class IUViewerFilter
        extends ViewerFilter
    {

        private Set<IInstallableUnit> units;

        @Override
        public boolean select( Viewer viewer, Object parentElement, Object element )
        {
            if ( units == null )
            {
                return true;
            }

            if ( !( element instanceof InstallableUnitNode ) )
            {
                return true;
            }

            InstallableUnitNode node = (InstallableUnitNode) element;

            return node.match( new InstallableUnitMatcher()
            {
                @Override
                public boolean match( IInstallableUnit unit )
                {
                    if ( units == null )
                    {
                        return true;
                    }

                    return units.contains( unit );
                }
            } );
        }

        public boolean setUnits( Set<IInstallableUnit> units )
        {
            boolean changed = !eq( this.units, units );
            this.units = units;
            return changed;
        }

        private static boolean eq( Set<IInstallableUnit> a, Set<IInstallableUnit> b )
        {
            if ( a == null && b == null )
            {
                return true;
            }
            if ( a != null && b != null )
            {
                return a.equals( b );
            }
            return false;
        }
    }

    IUViewerFilter iufilter = new IUViewerFilter();

    public DependencyHierarchyView()
    {
    }

    @Override
    public void createPartControl( Composite parent )
    {
        SashForm sashForm = new SashForm( parent, SWT.NONE );

        Composite hierarchyComposite = new Composite( sashForm, SWT.NONE );
        hierarchyComposite.setLayout( new GridLayout( 1, false ) );

        Label lblHierarchy = new Label( hierarchyComposite, SWT.NONE );
        lblHierarchy.setText( "Dependency Hierarchy" );

        hierarchyTreeViewer = new TreeViewer( hierarchyComposite, SWT.BORDER );
        Tree hierarchyTree = hierarchyTreeViewer.getTree();
        hierarchyTree.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true, 1, 1 ) );

        hierarchyTreeViewer.setContentProvider( new InstallableUnitContentProvider()
        {
            @Override
            public Object[] getElements( Object inputElement )
            {
                if (inputElement instanceof IGroupedInstallableUnits)
                {
                    IGroupedInstallableUnits metadata = (IGroupedInstallableUnits) inputElement;
                    return toViewNodes( metadata, metadata.getRootIncludedInstallableUnits() );
                }
                return null;
            }
        } );
        hierarchyTreeViewer.addFilter( iufilter );

        ILabelProvider labelProvider = new InstallableUnitLabelProvider();

        hierarchyTreeViewer.setLabelProvider( labelProvider );

        Composite listComposite = new Composite( sashForm, SWT.NONE );
        listComposite.setLayout( new GridLayout( 1, false ) );

        Label lblResolvedDependencies = new Label( listComposite, SWT.NONE );
        lblResolvedDependencies.setText( "Resolved Dependencies" );

        listTableViewer = new TableViewer( listComposite, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI );
        listTableViewer.addSelectionChangedListener( new ISelectionChangedListener()
        {
            public void selectionChanged( SelectionChangedEvent event )
            {
                IStructuredSelection selection = (IStructuredSelection) listTableViewer.getSelection();

                Set<IInstallableUnit> units = null;
                if ( !selection.isEmpty() )
                {
                    units = new HashSet<IInstallableUnit>();

                    Iterator<?> iter = selection.iterator();
                    while ( iter.hasNext() )
                    {
                        Object element = iter.next();
                        if ( element instanceof IInstallableUnit )
                        {
                            units.add( (IInstallableUnit) element );
                        }
                    }

                    if ( units.isEmpty() )
                    {
                        units = null;
                    }
                }

                if ( iufilter.setUnits( units ) )
                {
                    hierarchyTreeViewer.refresh();
                    hierarchyTreeViewer.expandAll();
                }
            }
        } );

        Table listTable = listTableViewer.getTable();
        listTable.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true, 1, 1 ) );
        listTableViewer.setContentProvider( new IStructuredContentProvider()
        {
            @Override
            public void inputChanged( Viewer viewer, Object oldInput, Object newInput )
            {
            }

            @Override
            public void dispose()
            {
            }

            @Override
            public Object[] getElements( Object inputElement )
            {
                if ( inputElement instanceof Collection )
                {
                    return ( (Collection<?>) inputElement ).toArray();
                }
                return null;
            }
        } );
        listTableViewer.setLabelProvider( labelProvider );

        sashForm.setWeights( new int[] { 1, 1 } );

        createActions();
        initializeToolBar();
        initializeMenu();
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
        IToolBarManager toolbarManager = getViewSite().getActionBars().getToolBarManager();
    }

    /**
     * Initialize the menu.
     */
    private void initializeMenu()
    {
        IMenuManager menuManager = getViewSite().getActionBars().getMenuManager();
    }

    @Override
    public void setFocus()
    {
        // Set the focus
    }

    public void setMetadata( IQueryable<IInstallableUnit> allIUs, Collection<IInstallableUnit> rootIUs )
    {
        NullProgressMonitor monitor = new NullProgressMonitor();

        Map<String, String> context = Collections.<String, String> emptyMap();
        PermissiveSlicer slicer = new PermissiveSlicer( allIUs, context, true, false, true, false, false );
        DependencyMesh mesh = slicer.slice( toArray( rootIUs ), monitor );

        // TODO is it okay to use permissive slicer here?

        Projector projector =
            new Projector( mesh.toQueryable(), context, slicer.getNonGreedyIUs(), isListenerAttached() );
        IInstallableUnit entryPointIU = createEntryPointIU( rootIUs );
        IInstallableUnit[] alreadyExistingRoots = new IInstallableUnit[0];
        IQueryable<IInstallableUnit> installedIUs = new QueryableArray( new IInstallableUnit[0] );
        projector.encode( entryPointIU, alreadyExistingRoots, installedIUs, rootIUs, monitor );
        projector.invokeSolver( monitor );
        Collection<IInstallableUnit> resolved = projector.extractSolution();

        if ( resolved == null )
        {
            Set<Explanation> explanation = projector.getExplanation( monitor );
            System.out.println( explanation );
        }

        InstallableUnitDependencyTree dependencyTree =
            new InstallableUnitDependencyTree( mesh.filterResolved( resolved ) );

        hierarchyTreeViewer.setInput( dependencyTree );
        hierarchyTreeViewer.refresh();
        hierarchyTreeViewer.expandAll();

        listTableViewer.setInput( resolved );
    }

    private IInstallableUnit createEntryPointIU( Collection<IInstallableUnit> rootIUs )
    {
        InstallableUnitDescription iud = new MetadataFactory.InstallableUnitDescription();
        String time = Long.toString( System.currentTimeMillis() );
        iud.setId( time );
        iud.setVersion( Version.createOSGi( 0, 0, 0, time ) );

        ArrayList<IRequirement> requirements = new ArrayList<IRequirement>();
        for ( IInstallableUnit iu : rootIUs )
        {
            VersionRange range = new VersionRange( iu.getVersion(), true, iu.getVersion(), true );
            requirements.add( MetadataFactory.createRequirement( IInstallableUnit.NAMESPACE_IU_ID, iu.getId(), range,
                                                                 iu.getFilter(), 1 /* min */, iu.isSingleton() ? 1
                                                                                 : Integer.MAX_VALUE /* max */, true /* greedy */) );
        }

        iud.setRequirements( (IRequirement[]) requirements.toArray( new IRequirement[requirements.size()] ) );

        return MetadataFactory.createInstallableUnit( iud );
    }

    private IInstallableUnit[] toArray( Collection<IInstallableUnit> collection )
    {
        return collection.toArray( new IInstallableUnit[collection.size()] );
    }
}
