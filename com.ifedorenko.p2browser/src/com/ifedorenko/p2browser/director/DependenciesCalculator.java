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
package com.ifedorenko.p2browser.director;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
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

import com.ifedorenko.p2browser.model.match.InstallableUnitsMatcher;

import copied.org.eclipse.equinox.internal.p2.director.PermissiveSlicer;

@SuppressWarnings( "restriction" )
public class DependenciesCalculator
    implements IInstallableUnitHierarchyCalculator
{
    private final IQueryable<IInstallableUnit> allIUs;

    private final Collection<IInstallableUnit> rootIUs;

    private InstallableUnitDAG dag;

    private List<IInstallableUnit> resolved;

    public DependenciesCalculator( IQueryable<IInstallableUnit> allIUs, Collection<IInstallableUnit> rootIUs )
    {
        this.allIUs = allIUs;
        this.rootIUs = rootIUs;
    }

    @Override
    public void run( IProgressMonitor monitor )
        throws InvocationTargetException, InterruptedException
    {
        Map<String, String> context = Collections.<String, String> emptyMap();
        PermissiveSlicer slicer = new PermissiveSlicer( allIUs, context, true, false, true, false, false );
        InstallableUnitDAG dag = slicer.slice( toArray( rootIUs ), monitor );

        // TODO is it okay to use permissive slicer here?

        Projector projector = new Projector( dag.toQueryable(), context, slicer.getNonGreedyIUs(), false );
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
        this.resolved = new ArrayList<IInstallableUnit>( resolved );
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

    public InstallableUnitDAG getHierarchy()
    {
        return dag;
    }

    public List<IInstallableUnit> getList()
    {
        return resolved;
    }
}
