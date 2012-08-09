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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.operation.IRunnableWithProgress;
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

import com.ifedorenko.p2browser.director.IInstallableUnitHierarchyCalculator;
import com.ifedorenko.p2browser.director.InstallableUnitDAG;
import com.ifedorenko.p2browser.model.IGroupedInstallableUnits;
import com.ifedorenko.p2browser.model.InstallableUnitDependencyTree;
import com.ifedorenko.p2browser.model.match.IInstallableUnitMatcher;
import com.ifedorenko.p2browser.model.match.InstallableUnitsMatcher;
import com.ifedorenko.p2browser.views.InstallableUnitFilterComposite.IFilterChangeListener;

public abstract class AbstractInstallableUnitHierarchyView
    extends ViewPart
{
    TreeViewer hierarchyTreeViewer;

    TableViewer listTableViewer;

    InstallableUnitDAG dag;

    IInstallableUnitMatcher unitMatcher;

    Font boldFont;

    boolean filterHierarchy = true;

    boolean filterList = false;

    boolean sortList = true;

    Action filterHierarchyAction;

    Job applyFilterJob = new Job( "Apply filter" )
    {
        @Override
        protected IStatus run( IProgressMonitor monitor )
        {
            getSite().getShell().getDisplay().asyncExec( new Runnable()
            {
                @Override
                public void run()
                {
                    applyHierachyFilter();
                    applyListFilter();
                }
            } );
            return Status.OK_STATUS;
        }
    };

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

    // windowbuilder wants this for some reason
    public AbstractInstallableUnitHierarchyView()
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
                applyFilterJob.schedule( 500L );
            }
        } );
        filterComposite.setLayoutData( new GridData( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );

        SashForm sashForm = new SashForm( composite, SWT.NONE );
        sashForm.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true, 1, 1 ) );

        Section hierarchySection = new Section( sashForm, Section.TITLE_BAR );
        hierarchySection.setText( getHierarchySectionTitle() );

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
        listSection.setText( getListSectionTitle() );

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

    String getListSectionTitle()
    {
        return "List section";
    }

    String getHierarchySectionTitle()
    {
        return "Hierarchy section";
    }

    @Override
    public void setFocus()
    {
        // Set the focus
    }

    public void setMetadata( IQueryable<IInstallableUnit> units, Collection<IInstallableUnit> roots )
    {
        IInstallableUnitHierarchyCalculator calculator = getCalculator( units, roots );

        run( calculator );
        dag = calculator.getHierarchy();

        setDependencyTree( new InstallableUnitDependencyTree( dag ) );
        listTableViewer.setInput( calculator.getList() );
    }

    protected abstract IInstallableUnitHierarchyCalculator getCalculator( IQueryable<IInstallableUnit> units,
                                                                          Collection<IInstallableUnit> roots );

    void setDependencyTree( InstallableUnitDependencyTree dependencyTree )
    {
        hierarchyTreeViewer.getTree().setRedraw( false );
        hierarchyTreeViewer.setInput( dependencyTree );
        hierarchyTreeViewer.getTree().setItemCount( dependencyTree.getRootIncludedInstallableUnits().size() );
        hierarchyTreeViewer.refresh();
        if ( dependencyTree.size() < 100 )
        {
            hierarchyTreeViewer.expandAll();
        }
        else
        {
            hierarchyTreeViewer.expandToLevel( 2 );
        }
        hierarchyTreeViewer.getTree().setRedraw( true );
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

    protected void run( IRunnableWithProgress runnable )
    {
        try
        {
            getViewSite().getWorkbenchWindow().run( true, true, runnable );
        }
        catch ( InvocationTargetException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch ( InterruptedException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
