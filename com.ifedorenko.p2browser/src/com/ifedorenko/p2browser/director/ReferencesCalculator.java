/*******************************************************************************
 * Copyright (c) 2012 Igor Fedorenko
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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;

public class ReferencesCalculator
    implements IInstallableUnitHierarchyCalculator
{
    private final IQueryable<IInstallableUnit> units;

    private final Collection<IInstallableUnit> roots;

    private final boolean allowDuplicates;

    private InstallableUnitDAG result;

    public ReferencesCalculator( IQueryable<IInstallableUnit> units, Collection<IInstallableUnit> roots )
    {
        this( units, roots, false );
    }

    protected ReferencesCalculator( IQueryable<IInstallableUnit> units, Collection<IInstallableUnit> roots,
                                    boolean allowDuplicates )
    {
        this.units = units;
        this.roots = roots;
        this.allowDuplicates = allowDuplicates;
    }

    private InstallableUnitDAG calculate( IProgressMonitor progress )
    {
        SubMonitor monitor = SubMonitor.convert( progress, 100 );

        // all requirements satisfied by a given IU
        Map<IInstallableUnit, Set<IRequirement>> providers = new LinkedHashMap<IInstallableUnit, Set<IRequirement>>();

        // all IUs with a given requirement
        Map<IRequirement, Set<IInstallableUnit>> requirements =
            new LinkedHashMap<IRequirement, Set<IInstallableUnit>>();

        index( providers, requirements, monitor.newChild( 80 ) );

        InstallableUnitDAG result = calculate( providers, requirements, monitor.newChild( 20 ) );

        monitor.done();

        return result;
    }

    private void index( Map<IInstallableUnit, Set<IRequirement>> providers,
                        Map<IRequirement, Set<IInstallableUnit>> requirements, SubMonitor monitor )
    {
        monitor.beginTask( "", 100 );

        Set<IInstallableUnit> units =
            this.units.query( QueryUtil.ALL_UNITS, monitor.newChild( 1 ) ).toUnmodifiableSet();

        monitor = monitor.newChild( 99 );
        monitor.beginTask( "", units.size() );

        for ( IInstallableUnit unit : units )
        {
            if ( monitor.isCanceled() )
            {
                break;
            }

            if ( !isInteresting( unit ) )
            {
                continue;
            }

            for ( IRequirement _requirement : unit.getRequirements() )
            {
                if ( !isInteresting( _requirement ) )
                {
                    continue;
                }
                put( requirements, _requirement, unit );
                IQuery<IInstallableUnit> query = QueryUtil.createMatchQuery( _requirement.getMatches() );
                Iterator<IInstallableUnit> _providers = this.units.query( query, monitor.newChild( 0 ) ).iterator();
                while ( _providers.hasNext() )
                {
                    IInstallableUnit _provider = _providers.next();
                    put( providers, _provider, _requirement );
                }
            }
            monitor.worked( 1 );
        }
    }

    protected boolean isInteresting( IRequirement _requirement )
    {
        return true;
    }

    protected boolean isInteresting( IInstallableUnit unit )
    {
        return !unit.getId().startsWith( "tooling" );
    }

    private InstallableUnitDAG calculate( Map<IInstallableUnit, Set<IRequirement>> providers,
                                          Map<IRequirement, Set<IInstallableUnit>> requirements, SubMonitor monitor )
    {
        monitor.beginTask( "", requirements.size() ); // don't ask why

        Map<IInstallableUnit, InstallableUnitInfo> result = new LinkedHashMap<IInstallableUnit, InstallableUnitInfo>();

        for ( IInstallableUnit unit : roots )
        {
            calculate( result, providers, requirements, unit, new LinkedList<IInstallableUnit>(),
                       new LinkedHashSet<IInstallableUnit>(), monitor );
        }

        return new InstallableUnitDAG( result );
    }

    private void calculate( Map<IInstallableUnit, InstallableUnitInfo> result,
                            Map<IInstallableUnit, Set<IRequirement>> providers,
                            Map<IRequirement, Set<IInstallableUnit>> requirements, IInstallableUnit unit,
                            LinkedList<IInstallableUnit> backtrace, Set<IInstallableUnit> considered, SubMonitor monitor )
    {
        if ( monitor.isCanceled() )
        {
            return;
        }

        Set<IRequirement> satisfiedRequirements = providers.get( unit );
        if ( satisfiedRequirements != null )
        {
            for ( IRequirement requirement : satisfiedRequirements )
            {
                Set<IInstallableUnit> requirementReferences = requirements.get( requirement );
                for ( IInstallableUnit reference : requirementReferences )
                {
                    if ( !reference.equals( unit ) && !backtrace.contains( reference ) )
                    {
                        boolean duplicate = !considered.add( reference );
                        if ( allowDuplicates || !duplicate )
                        {
                            addChild( result, unit, reference );
                        }
                        if ( !duplicate )
                        {
                            backtrace.addLast( reference );
                            calculate( result, providers, requirements, reference, backtrace, considered, monitor );
                            backtrace.removeLast();
                        }
                    }
                }
            }
        }

        monitor.worked( 1 );
    }

    private static <K, V> void put( Map<K, Set<V>> map, K key, V value )
    {
        Set<V> set = map.get( key );
        if ( set == null )
        {
            set = new LinkedHashSet<V>();
            map.put( key, set );
        }
        set.add( value );
    }

    private void addChild( Map<IInstallableUnit, InstallableUnitInfo> result, IInstallableUnit parent,
                           IInstallableUnit child )
    {
        InstallableUnitInfo parentInfo = getOrNew( result, parent );
        InstallableUnitInfo childInfo = getOrNew( result, child );
        parentInfo.addChild( childInfo );
    }

    private InstallableUnitInfo getOrNew( Map<IInstallableUnit, InstallableUnitInfo> result, IInstallableUnit parent )
    {
        InstallableUnitInfo info = result.get( parent );
        if ( info == null )
        {
            info = new InstallableUnitInfo( parent );
            result.put( parent, info );
        }
        return info;
    }

    @Override
    public void run( IProgressMonitor monitor )
        throws InvocationTargetException, InterruptedException
    {
        this.result = calculate( monitor );
    }

    public InstallableUnitDAG getHierarchy()
    {
        return result;
    }

    public List<IInstallableUnit> getList()
    {
        return new ArrayList<IInstallableUnit>( result.getInstallableUnits() );
    }

}
