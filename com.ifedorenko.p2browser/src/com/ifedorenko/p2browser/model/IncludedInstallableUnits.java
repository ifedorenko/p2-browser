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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
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

import com.ifedorenko.p2browser.director.InstallableUnitInfo;

@SuppressWarnings( "restriction" )
public class IncludedInstallableUnits
    implements IGroupedInstallableUnits
{
    private final Map<IVersionedId, InstallableUnitInfo> nodes;

    public IncludedInstallableUnits( Iterator<IInstallableUnit> iter )
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
                otherNode.addParent( node );
            }
        }

        this.nodes = nodes;
    }

    public static IncludedInstallableUnits getInstallableUnits( IQueryable<IInstallableUnit> queryable,
                                                                        IProgressMonitor monitor )
    {
        Iterator<IInstallableUnit> iter = queryable.query( QueryUtil.ALL_UNITS, monitor ).iterator();
        return new IncludedInstallableUnits( iter );
    }

    @Override
    public int size()
    {
        return nodes.size();
    }

    @Override
    public Collection<IInstallableUnit> getInstallableUnits()
    {
        return toInstallableUnits( nodes.values() );
    }

    @Override
    public Collection<IInstallableUnit> getIncludedInstallableUnits( IInstallableUnit parent, boolean transitive )
    {
        Collection<InstallableUnitInfo> included = getIncludedInstallableUnit( nodes, parent );
        if ( !transitive )
        {
            return toInstallableUnits( included );
        }

        return getIncludedInstallableUnits( included, new LinkedHashSet<IInstallableUnit>() );
    }

    @Override
    public Collection<IInstallableUnit> getRootIncludedInstallableUnits()
    {
        List<IInstallableUnit> roots = new ArrayList<IInstallableUnit>();

        for ( InstallableUnitInfo node : nodes.values() )
        {
            if ( node.getParents().isEmpty() )
            {
                roots.add( node.getInstallableUnit() );
            }
        }

        return roots;
    }

    private Collection<IInstallableUnit> getIncludedInstallableUnits( Collection<InstallableUnitInfo> included,
                                                                      Set<IInstallableUnit> result )
    {
        for ( InstallableUnitInfo unit : included )
        {
            if ( result.add( unit.getInstallableUnit() ) )
            {
                getIncludedInstallableUnits( getIncludedInstallableUnit( nodes, unit.getInstallableUnit() ), result );
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

    private Collection<IInstallableUnit> toInstallableUnits( Collection<InstallableUnitInfo> nodes )
    {
        ArrayList<IInstallableUnit> result = new ArrayList<IInstallableUnit>();
        for ( InstallableUnitInfo node : nodes )
        {
            result.add( node.getInstallableUnit() );
        }
        return result;
    }

    private static boolean isSingleVersion( VersionRange range )
    {
        // RequiredCapability.isVersionStrict
        return range.getIncludeMaximum() && range.getIncludeMinimum() && range.getMinimum().equals( range.getMaximum() );
    }

}
