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

package com.ifedorenko.p2browser.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;

import com.ifedorenko.p2browser.director.InstallableUnitDAG;
import com.ifedorenko.p2browser.director.InstallableUnitInfo;

@SuppressWarnings( "restriction" )
public class IncludedInstallableUnits
{

    public InstallableUnitDAG toInstallableUnitDAG( Iterator<IInstallableUnit> iter )
    {
        Map<IVersionedId, InstallableUnitInfo> nodes = new LinkedHashMap<IVersionedId, InstallableUnitInfo>();

        while ( iter.hasNext() )
        {
            IInstallableUnit iu = iter.next();
            nodes.put( new VersionedId( iu.getId(), iu.getVersion() ), new InstallableUnitInfo( iu ) );
        }

        for ( InstallableUnitInfo node : nodes.values() )
        {
            for ( InstallableUnitInfo otherNode : getIncludedInstallableUnit( nodes, node.getInstallableUnit() ) )
            {
                node.addChild( otherNode );
            }
        }

        Map<IInstallableUnit, InstallableUnitInfo> units = new LinkedHashMap<IInstallableUnit, InstallableUnitInfo>();
        for ( InstallableUnitInfo info : nodes.values() )
        {
            units.put( info.getInstallableUnit(), info );
        }

        // break cycles
        for ( InstallableUnitInfo unit : units.values() )
        {
            breakCycles( unit, new LinkedList<InstallableUnitInfo>() );
        }

        return new InstallableUnitDAG( units );
    }

    private void breakCycles( InstallableUnitInfo unit, Deque<InstallableUnitInfo> visited )
    {
        ArrayList<InstallableUnitInfo> cycles = new ArrayList<InstallableUnitInfo>();

        for ( InstallableUnitInfo child : unit.getChildren() )
        {
            if ( visited.contains( child ) )
            {
                cycles.add( child );
                continue;
            }
            visited.addFirst( child );
            breakCycles( child, visited );
            visited.removeFirst();
        }

        if ( !cycles.isEmpty() )
        {
            for ( InstallableUnitInfo child : cycles )
            {
                unit.removeChild( child );
            }
        }
    }

    public InstallableUnitDAG toInstallableUnitDAG( IQueryable<IInstallableUnit> queryable, IProgressMonitor monitor )
    {
        Iterator<IInstallableUnit> iter = queryable.query( QueryUtil.ALL_UNITS, monitor ).iterator();
        return toInstallableUnitDAG( iter );
    }

    public Collection<IInstallableUnit> getIncludedInstallableUnits( Map<IVersionedId, InstallableUnitInfo> nodes,
                                                                     IInstallableUnit parent )
    {
        Collection<InstallableUnitInfo> included = getIncludedInstallableUnit( nodes, parent );

        return getIncludedInstallableUnits( nodes, included, new LinkedHashSet<IInstallableUnit>() );
    }

    private Collection<IInstallableUnit> getIncludedInstallableUnits( Map<IVersionedId, InstallableUnitInfo> nodes,
                                                                      Collection<InstallableUnitInfo> included,
                                                                      Set<IInstallableUnit> result )
    {
        for ( InstallableUnitInfo unit : included )
        {
            if ( result.add( unit.getInstallableUnit() ) )
            {
                getIncludedInstallableUnits( nodes, getIncludedInstallableUnit( nodes, unit.getInstallableUnit() ),
                                             result );
            }
        }
        return result;
    }

    private static Collection<InstallableUnitInfo> getIncludedInstallableUnit( Map<IVersionedId, InstallableUnitInfo> nodes,
                                                                               IInstallableUnit parent )
    {
        Set<InstallableUnitInfo> result = new LinkedHashSet<InstallableUnitInfo>();

        for ( IRequirement r : parent.getRequirements() )
        {
            if ( r instanceof IRequiredCapability )
            {
                IRequiredCapability rc = (IRequiredCapability) r;

                if ( isSingleVersion( rc.getRange() ) )
                {
                    InstallableUnitInfo otherNode =
                        nodes.get( new VersionedId( rc.getName(), rc.getRange().getMinimum() ) );
                    if ( otherNode != null )
                    {
                        result.add( otherNode );
                    }
                }
            }
        }

        return result;
    }

    public static boolean isSingleVersion( VersionRange range )
    {
        // RequiredCapability.isVersionStrict
        return range.getIncludeMaximum() && range.getIncludeMinimum() && range.getMinimum().equals( range.getMaximum() );
    }

}
