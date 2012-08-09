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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import org.eclipse.equinox.internal.p2.director.QueryableArray;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryable;

import com.ifedorenko.p2browser.model.match.IInstallableUnitMatcher;

@SuppressWarnings( "restriction" )
public class InstallableUnitDAG
{
    private final IInstallableUnit[] rootIUs;

    private final Map<IInstallableUnit, InstallableUnitInfo> units;

    public InstallableUnitDAG( IInstallableUnit[] rootIUs, Map<IInstallableUnit, InstallableUnitInfo> units )
    {
        this.rootIUs = rootIUs;
        this.units = Collections.unmodifiableMap( new LinkedHashMap<IInstallableUnit, InstallableUnitInfo>( units ) );

        assertPreconditions();
    }

    public InstallableUnitDAG( Map<IInstallableUnit, InstallableUnitInfo> units )
    {
        this( getRootIUs( units ), units );
    }

    public Collection<InstallableUnitInfo> getRootInstallableUnits()
    {
        List<InstallableUnitInfo> result = new ArrayList<InstallableUnitInfo>();

        for ( IInstallableUnit root : rootIUs )
        {
            result.add( units.get( root ) );
        }

        return result;
    }

    public IQueryable<IInstallableUnit> toQueryable()
    {
        Set<IInstallableUnit> set = units.keySet();
        return new QueryableArray( set.toArray( new IInstallableUnit[set.size()] ) );
    }

    public InstallableUnitDAG filter( IInstallableUnitMatcher matcher, boolean includeParents )
    {
        Map<IInstallableUnit, InstallableUnitInfo> filtered =
            new LinkedHashMap<IInstallableUnit, InstallableUnitInfo>();

        for ( Map.Entry<IInstallableUnit, InstallableUnitInfo> entry : units.entrySet() )
        {
            if ( matcher.match( entry.getKey() ) )
            {
                filtered.put( entry.getKey(), new InstallableUnitInfo( entry.getValue().getInstallableUnit() ) );
            }
        }

        if ( includeParents )
        {
            Map<IInstallableUnit, Set<IInstallableUnit>> allparents = getParentMap( units );

            for ( InstallableUnitInfo info : new ArrayList<InstallableUnitInfo>( filtered.values() ) )
            {
                addNewParents( filtered, allparents, info );
            }
        }

        for ( InstallableUnitInfo unit : filtered.values() )
        {
            InstallableUnitInfo original = units.get( unit.getInstallableUnit() );

            for ( InstallableUnitInfo originalChild : original.getChildren() )
            {
                InstallableUnitInfo child = filtered.get( originalChild.getInstallableUnit() );
                if ( child != null )
                {
                    unit.addChild( child );
                }
            }
        }

        return new InstallableUnitDAG( filtered );
    }

    protected static IInstallableUnit[] getRootIUs( Map<IInstallableUnit, InstallableUnitInfo> units )
    {
        Map<IInstallableUnit, Set<IInstallableUnit>> allparents = getParentMap( units );

        Collection<IInstallableUnit> rootIUs = new LinkedHashSet<IInstallableUnit>();
        for ( InstallableUnitInfo info : units.values() )
        {
            Set<IInstallableUnit> parents = allparents.get( info.getInstallableUnit() );
            if ( parents == null || parents.isEmpty() )
            {
                rootIUs.add( info.getInstallableUnit() );
            }
        }

        return rootIUs.toArray( new IInstallableUnit[rootIUs.size()] );
    }

    protected static Map<IInstallableUnit, Set<IInstallableUnit>> getParentMap( Map<IInstallableUnit, InstallableUnitInfo> units )
    {
        Map<IInstallableUnit, Set<IInstallableUnit>> allparents =
            new HashMap<IInstallableUnit, Set<IInstallableUnit>>();
        for ( InstallableUnitInfo info : units.values() )
        {
            for ( InstallableUnitInfo childInfo : info.getChildren() )
            {
                Set<IInstallableUnit> parents = allparents.get( childInfo.getInstallableUnit() );
                if ( parents == null )
                {
                    parents = new HashSet<IInstallableUnit>();
                    allparents.put( childInfo.getInstallableUnit(), parents );
                }
                parents.add( info.getInstallableUnit() );
            }
        }
        return allparents;
    }

    private void addNewParents( Map<IInstallableUnit, InstallableUnitInfo> filtered,
                                Map<IInstallableUnit, Set<IInstallableUnit>> allparents, InstallableUnitInfo info )
    {
        Set<IInstallableUnit> parents = allparents.get( info.getInstallableUnit() );
        if ( parents == null )
        {
            return;
        }

        for ( IInstallableUnit parent : parents )
        {
            if ( !filtered.containsKey( parent ) )
            {
                InstallableUnitInfo parentInfo = units.get( parent );
                filtered.put( parent, new InstallableUnitInfo( parent ) );
                addNewParents( filtered, allparents, parentInfo );
            }
        }
    }

    public InstallableUnitDAG sort( final Comparator<IInstallableUnit> comparator )
    {
        IInstallableUnit[] rootIUs = new IInstallableUnit[this.rootIUs.length];
        System.arraycopy( this.rootIUs, 0, rootIUs, 0, this.rootIUs.length );
        Arrays.sort( rootIUs, comparator );

        Map<IInstallableUnit, InstallableUnitInfo> sorted = new LinkedHashMap<IInstallableUnit, InstallableUnitInfo>();

        for ( Map.Entry<IInstallableUnit, InstallableUnitInfo> entry : this.units.entrySet() )
        {
            InstallableUnitInfo info = getOrNew( sorted, entry.getKey() );

            List<InstallableUnitInfo> children = new ArrayList<InstallableUnitInfo>( entry.getValue().getChildren() );
            Collections.sort( children, new Comparator<InstallableUnitInfo>()
            {
                @Override
                public int compare( InstallableUnitInfo a, InstallableUnitInfo b )
                {
                    return comparator.compare( a.getInstallableUnit(), b.getInstallableUnit() );
                }
            } );

            for ( InstallableUnitInfo child : children )
            {
                info.addChild( getOrNew( sorted, child.getInstallableUnit() ) );
            }
        }

        return new InstallableUnitDAG( rootIUs, sorted );
    }

    private static InstallableUnitInfo getOrNew( Map<IInstallableUnit, InstallableUnitInfo> dag, IInstallableUnit unit )
    {
        InstallableUnitInfo info = dag.get( unit );
        if ( info == null )
        {
            info = new InstallableUnitInfo( unit );
            dag.put( unit, info );
        }
        return info;
    }

    public InstallableUnitInfo getInstallableUnit( IInstallableUnit unit )
    {
        return units.get( unit );
    }

    public Collection<IInstallableUnit> getInstallableUnits()
    {
        return units.keySet();
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }

        if ( !( obj instanceof InstallableUnitDAG ) )
        {
            return false;
        }

        InstallableUnitDAG other = (InstallableUnitDAG) obj;
        return Arrays.equals( rootIUs, other.rootIUs ) && units.equals( other.units );
    }

    public int getNodeCount()
    {
        int count = 0;
        Set<InstallableUnitInfo> visited = new HashSet<InstallableUnitInfo>();
        for ( IInstallableUnit root : rootIUs )
        {
            count += getNodeCount( units.get( root ), visited );
        }
        return count;
    }

    private int getNodeCount( InstallableUnitInfo root, Set<InstallableUnitInfo> visited )
    {
        int count = 1; // root itself
        for ( InstallableUnitInfo child : root.getChildren() )
        {
            count += getNodeCount( child, visited );
        }
        return count;
    }

    private void assertPreconditions()
    {
        for ( InstallableUnitInfo info : units.values() )
        {
            for ( InstallableUnitInfo child : info.getChildren() )
            {
                if ( child != units.get( child.getInstallableUnit() ) )
                {
                    throw new IllegalStateException( "Inconsistent/duplicate IU information "
                        + child.getInstallableUnit() );
                }
            }
        }

        for ( IInstallableUnit rootIU : rootIUs )
        {
            InstallableUnitInfo rootInfo = units.get( rootIU );
            LinkedList<InstallableUnitInfo> backtrace = new LinkedList<InstallableUnitInfo>();
            backtrace.add( rootInfo ); // seed backtrace
            assertNoCycles( rootInfo, backtrace, new HashSet<InstallableUnitInfo>() );
        }
    }

    private void assertNoCycles( InstallableUnitInfo root, LinkedList<InstallableUnitInfo> backtrace,
                                 Set<InstallableUnitInfo> visited )
    {
        for ( InstallableUnitInfo child : root.getChildren() )
        {
            if ( backtrace.contains( child ) )
            {
                StringBuilder msg = new StringBuilder( "Cycle has been detected " );
                for ( InstallableUnitInfo node : backtrace )
                {
                    msg.append( " [" ).append( node.getInstallableUnit().toString() ).append( "]" );
                }
                // child closes the cycle
                msg.append( " [" ).append( child.getInstallableUnit().toString() ).append( "]" );
                throw new IllegalStateException( msg.toString() );
            }
            if ( visited.add( child ) )
            {
                backtrace.addLast( child );
                assertNoCycles( child, backtrace, visited );
                backtrace.removeLast();
            }
        }
    }

    public int getEdgeCount()
    {
        int result = 0;
        for ( InstallableUnitInfo info : units.values() )
        {
            result += info.getChildren().size();
        }
        return result;
    }

    public void print( PrintStream out )
    {
        out.format( "root#=%d unit#=%d edge%s\n", rootIUs.length, units.size(), getEdgeCount() );
        // TODO number of paths
        for ( IInstallableUnit root : rootIUs )
        {
            print( out, root, 1 );
        }
    }

    private void print( PrintStream out, IInstallableUnit unit, int level )
    {
        StringBuilder sb = new StringBuilder();
        for ( int i = 0; i < level; i++ )
        {
            sb.append( "  " );
        }
        sb.append( unit.toString() );
        out.println( sb.toString() );
        InstallableUnitInfo info = units.get( unit );
        for ( InstallableUnitInfo child : info.getChildren() )
        {
            print( out, child.getInstallableUnit(), level + 1 );
        }
    }

    public void print( File file )
    {
        try
        {
            PrintStream out = new PrintStream( new GZIPOutputStream( new FileOutputStream( file ) ) );
            try
            {
                print( out );
            }
            finally
            {
                out.close();
            }
        }
        catch ( IOException e )
        {
            e.printStackTrace();
        }
    }

}
