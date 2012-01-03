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
import java.util.Collections;
import java.util.Comparator;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
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
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.wb.swt.ResourceManager;

import com.ifedorenko.p2browser.director.InstallableUnitDAG;
import com.ifedorenko.p2browser.model.IGroupedInstallableUnits;
import com.ifedorenko.p2browser.model.InstallableUnitDependencyTree;
import com.ifedorenko.p2browser.model.match.IInstallableUnitMatcher;
import com.ifedorenko.p2browser.model.match.InstallableUnitsMatcher;
import com.ifedorenko.p2browser.views.InstallableUnitFilterComposite.IFilterChangeListener;

import copied.org.eclipse.equinox.internal.p2.director.PermissiveSlicer;

@SuppressWarnings( "restriction" )
public class DependencyHierarchyView
    extends ViewPart
{

    public static final String ID = "com.ifedorenko.p2browser.views.DependencyHierarchyView"; //$NON-NLS-1$

    TreeViewer hierarchyTreeViewer;

    TableViewer listTableViewer;

    InstallableUnitDAG dag;

    IInstallableUnitMatcher unitMatcher;

    Font boldFont;

    boolean filterHierarchy = true;

    boolean filterList = false;

    boolean sortList = true;

    private Action filterHierarchyAction;

    // TODO share decoration logic with MetadataRepositoryView
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
    };

    public DependencyHierarchyView()
    {
    }

    @Override
    public void createPartControl( Composite parent )
    {
        Composite composite = new Composite( parent, SWT.NONE );
        GridLayout gl_composite = new GridLayout( 1, false );
        gl_composite.marginWidth = 0;
        gl_composite.marginHeight = 0;
        composite.setLayout( gl_composite );

        final InstallableUnitFilterComposite filterComposite = new InstallableUnitFilterComposite( composite, SWT.NONE );
        filterComposite.addFilterChangeListener( new IFilterChangeListener()
        {
            public void filterChanged( EventObject event )
            {
                unitMatcher = filterComposite.getMatcher();
                applyHierachyFilter();
                applyListFilter();
            }
        } );
        filterComposite.setLayoutData( new GridData( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );

        SashForm sashForm = new SashForm( composite, SWT.NONE );
        sashForm.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true, 1, 1 ) );

        Section hierarchySection = new Section( sashForm, Section.TITLE_BAR );
        hierarchySection.setText( "Dependency Hierarchy" );

        ToolBar hierarchyToolBar = new ToolBar( hierarchySection, SWT.FLAT | SWT.RIGHT );
        hierarchySection.setTextClient( hierarchyToolBar );

        ToolBarManager hierarchyToolBarManager = new ToolBarManager( hierarchyToolBar );
        hierarchyToolBarManager.add( new Action( "Expand All",
                                                 ResourceManager.getPluginImageDescriptor( "com.ifedorenko.p2browser",
                                                                                           "icons/expandall.gif" ) )
        {
            public void run()
            {
                hierarchyTreeViewer.getTree().setRedraw( false );
                hierarchyTreeViewer.expandAll();
                hierarchyTreeViewer.getTree().setRedraw( true );
            };
        } );
        hierarchyToolBarManager.add( new Action( "Collapse All",
                                                 ResourceManager.getPluginImageDescriptor( "com.ifedorenko.p2browser",
                                                                                           "icons/collapseall.gif" ) )
        {
            public void run()
            {
                hierarchyTreeViewer.getTree().setRedraw( false );
                hierarchyTreeViewer.collapseAll();
                hierarchyTreeViewer.expandToLevel( 2 );
                hierarchyTreeViewer.getTree().setRedraw( true );
            };
        } );
        hierarchyToolBarManager.add( new Separator() );
        filterHierarchyAction =
            new Action( "Filter", ResourceManager.getPluginImageDescriptor( "com.ifedorenko.p2browser",
                                                                            "icons/filter.gif" ) )
            {
                @Override
                public int getStyle()
                {
                    return AS_CHECK_BOX;
                }

                public void run()
                {
                    filterHierarchy = isChecked();
                    applyHierachyFilter();
                };
            };
        filterHierarchyAction.setChecked( filterHierarchy );
        hierarchyToolBarManager.add( filterHierarchyAction );
        hierarchyToolBarManager.update( true );

        hierarchyTreeViewer = new TreeViewer( hierarchySection, SWT.BORDER | SWT.VIRTUAL );
        Tree hierarchyTree = hierarchyTreeViewer.getTree();
        hierarchySection.setClient( hierarchyTree );
        hierarchyTreeViewer.setUseHashlookup( true );
        hierarchyTreeViewer.setContentProvider( new InstallableUnitContentProvider( hierarchyTreeViewer )
        {
            @Override
            public Object[] getChildren( Object inputElement )
            {
                if ( inputElement instanceof IGroupedInstallableUnits )
                {
                    IGroupedInstallableUnits metadata = (IGroupedInstallableUnits) inputElement;
                    return toViewNodes( metadata, metadata.getRootIncludedInstallableUnits() );
                }
                return super.getChildren( inputElement );
            }
        } );

        ILabelProvider labelProvider = new LabelProvider();

        hierarchyTreeViewer.setLabelProvider( labelProvider );

        Section listSection = new Section( sashForm, Section.TITLE_BAR );

        listSection.setText( "Resolved Dependencies" );

        ToolBar listToolBar = new ToolBar( listSection, SWT.FLAT | SWT.RIGHT );
        listSection.setTextClient( listToolBar );
        ToolBarManager listToolbarManager = new ToolBarManager( listToolBar );
        Action sortListAction =
            new Action( "Sort", ResourceManager.getPluginImageDescriptor( "com.ifedorenko.p2browser", "icons/sort.gif" ) )
            {
                @Override
                public int getStyle()
                {
                    return AS_CHECK_BOX;
                }

                @Override
                public void run()
                {
                    sortList = isChecked();
                    applyListFilter();
                }
            };
        sortListAction.setChecked( sortList );
        listToolbarManager.add( sortListAction );
        Action filterListAction =
            new Action( "Filter", ResourceManager.getPluginImageDescriptor( "com.ifedorenko.p2browser",
                                                                            "icons/filter.gif" ) )
            {
                @Override
                public int getStyle()
                {
                    return AS_CHECK_BOX;
                }

                @Override
                public void run()
                {
                    filterList = isChecked();
                    applyListFilter();
                }
            };
        filterListAction.setChecked( filterList );
        listToolbarManager.add( filterListAction );
        listToolbarManager.update( true );

        listTableViewer = new TableViewer( listSection, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI );
        Table listTable = listTableViewer.getTable();
        listSection.setClient( listTable );

        listTableViewer.addSelectionChangedListener( new ISelectionChangedListener()
        {
            public void selectionChanged( SelectionChangedEvent event )
            {
                Set<IInstallableUnit> units = toInstallableUnits( listTableViewer.getSelection() );

                filterHierarchyAction.setEnabled( units == null );
                filterHierarchyAction.setChecked( units == null ? filterHierarchy : false );

                applyHierachyFilter( units );
            }
        } );

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
                if ( inputElement instanceof List )
                {
                    List<?> elements = (List<?>) inputElement;

                    if ( sortList )
                    {
                        elements = new ArrayList<Object>( elements ); // do not sort original collection
                        Collections.sort( elements, new Comparator<Object>()
                        {
                            @Override
                            public int compare( Object o1, Object o2 )
                            {
                                // unchecked cast is on purpose, so we known when element type changes ;-)
                                IInstallableUnit u1 = (IInstallableUnit) o1;
                                IInstallableUnit u2 = (IInstallableUnit) o2;

                                return u1.getId().compareTo( u2.getId() );
                            }
                        } );
                    }
                    return elements.toArray();
                }
                return null;
            }
        } );
        listTableViewer.setLabelProvider( labelProvider );

        listTableViewer.addFilter( new ViewerFilter()
        {
            @Override
            public boolean select( Viewer viewer, Object parentElement, Object element )
            {
                if ( !filterList || unitMatcher == null )
                {
                    // no filter
                    return true;
                }

                IInstallableUnit unit = null;
                if ( element instanceof IInstallableUnit )
                {
                    unit = (IInstallableUnit) element;
                }

                if ( unit == null )
                {
                    return true;
                }

                return unitMatcher.match( unit );
            }
        } );

        sashForm.setWeights( new int[] { 1, 1 } );

        Font font = listTableViewer.getTable().getFont();
        FontData[] fontDatas = font.getFontData();
        for ( FontData fontData : fontDatas )
        {
            fontData.setStyle( SWT.BOLD );
        }
        boldFont = new Font( getSite().getShell().getDisplay(), fontDatas );
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
        InstallableUnitDAG dag = slicer.slice( toArray( rootIUs ), monitor );

        // TODO is it okay to use permissive slicer here?

        Projector projector =
            new Projector( dag.toQueryable(), context, slicer.getNonGreedyIUs(), isListenerAttached() );
        IInstallableUnit entryPointIU = createEntryPointIU( rootIUs );
        IInstallableUnit[] alreadyExistingRoots = new IInstallableUnit[0];
        IQueryable<IInstallableUnit> installedIUs = new QueryableArray( new IInstallableUnit[0] );
        projector.encode( entryPointIU, alreadyExistingRoots, installedIUs, rootIUs, monitor );
        projector.invokeSolver( monitor );
        final Collection<IInstallableUnit> resolved = projector.extractSolution();

        if ( resolved == null )
        {
            Set<Explanation> explanation = projector.getExplanation( monitor );
            System.out.println( explanation );
        }

        dag = dag.filter( new InstallableUnitsMatcher( resolved ), false );

        this.dag = dag;

        setDependencyTree( new InstallableUnitDependencyTree( dag ) );

        listTableViewer.setInput( resolved );
    }

    void setDependencyTree( InstallableUnitDependencyTree dependencyTree )
    {
        hierarchyTreeViewer.getTree().setRedraw( false );
        hierarchyTreeViewer.setInput( dependencyTree );
        hierarchyTreeViewer.getTree().setItemCount( dependencyTree.getRootIncludedInstallableUnits().size() );
        hierarchyTreeViewer.refresh();
        hierarchyTreeViewer.expandAll();
        hierarchyTreeViewer.getTree().setRedraw( true );
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

    void applyHierachyFilter()
    {
        applyHierachyFilter( toInstallableUnits( listTableViewer.getSelection() ) );
    }

    void applyHierachyFilter( Set<IInstallableUnit> units )
    {
        InstallableUnitDAG filteredDag = this.dag;

        if ( units != null )
        {
            // apply right-pane selection filter
            filteredDag = filteredDag.filter( new InstallableUnitsMatcher( units ), true );
        }
        else if ( unitMatcher != null && filterHierarchy )
        {
            filteredDag = filteredDag.filter( unitMatcher, true );
        }

        setDependencyTree( new InstallableUnitDependencyTree( filteredDag ) );
    }

    Set<IInstallableUnit> toInstallableUnits( ISelection selection )
    {
        Set<IInstallableUnit> units = null;
        if ( selection instanceof IStructuredSelection && !selection.isEmpty() )
        {
            units = new HashSet<IInstallableUnit>();

            Iterator<?> iter = ( (IStructuredSelection) selection ).iterator();
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
        return units;
    }

    void applyListFilter()
    {
        listTableViewer.getTable().setRedraw( false );
        listTableViewer.refresh();
        listTableViewer.getTable().setRedraw( true );
    }
}
