package com.ifedorenko.p2browser.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;

public class InstallableUnitInfo
{
    private final IInstallableUnit unit;

    private final Set<InstallableUnitInfo> parents = new HashSet<InstallableUnitInfo>();

    private final Set<InstallableUnitInfo> children = new LinkedHashSet<InstallableUnitInfo>();

    public InstallableUnitInfo( IInstallableUnit unit )
    {
        this.unit = unit;
    }

    public IInstallableUnit getInstallableUnit()
    {
        return unit;
    }

    public Collection<InstallableUnitInfo> getParents()
    {
        return parents;
    }

    public void addParent( InstallableUnitInfo parent )
    {
        parents.add( parent );
    }

    @Override
    public int hashCode()
    {
        int hash = 17;
        hash = hash * 31 + unit.hashCode();
//        hash = hash * 31 + parents.hashCode();
//        hash = hash * 31 + children.hashCode();
        return hash;
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }

        if ( !( obj instanceof InstallableUnitInfo ) )
        {
            return false;
        }

        InstallableUnitInfo other = (InstallableUnitInfo) obj;

        return unit.equals( other.unit ) && parents.equals( other.parents ) && children.equals( other.children );
    }

    public void addChild( InstallableUnitInfo child )
    {
        children.add( child );
    }

    public Collection<InstallableUnitInfo> getChildren()
    {
        return children;
    }
}
