package com.ifedorenko.p2browser.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;

import copied.org.eclipse.equinox.internal.p2.director.DependencyMesh;

public class InstallableUnitDependencyTree
    implements IGroupedInstallableUnits
{

    private final DependencyMesh dependencyTree;

    public InstallableUnitDependencyTree( DependencyMesh dependencyTree )
    {
        this.dependencyTree = dependencyTree;
    }

    @Override
    public int size()
    {
        return dependencyTree.getInstallableUnits().size();
    }

    @Override
    public Collection<IInstallableUnit> getInstallableUnits()
    {
        return toInstallableUnits( dependencyTree.getInstallableUnits() );
    }

    @Override
    public Collection<IInstallableUnit> getIncludedInstallableUnits( IInstallableUnit unit, boolean transitive )
    {
        InstallableUnitInfo parent = dependencyTree.getInstallableUnit( unit );

        Collection<InstallableUnitInfo> children = parent.getChildren();

        if ( !transitive )
        {
            return toInstallableUnits( children );
        }

        return addIncludedInstallableUnits( children, new LinkedHashSet<IInstallableUnit>() );
    }

    private Collection<IInstallableUnit> addIncludedInstallableUnits( Collection<InstallableUnitInfo> children,
                                                                      Set<IInstallableUnit> result )
    {
        for ( InstallableUnitInfo child : children )
        {
            IInstallableUnit iu = child.getInstallableUnit();
            if ( result.add( iu ) )
            {
                addIncludedInstallableUnits( dependencyTree.getInstallableUnit( iu ).getChildren(), result );
            }
        }
        return result;
    }

    private Collection<IInstallableUnit> toInstallableUnits( Collection<InstallableUnitInfo> units )
    {
        List<IInstallableUnit> result = new ArrayList<IInstallableUnit>();
        for ( InstallableUnitInfo unit : units )
        {
            result.add( unit.getInstallableUnit() );
        }
        return result;
    }

    @Override
    public Collection<IInstallableUnit> getRootIncludedInstallableUnits()
    {
        return toInstallableUnits( dependencyTree.getRootInstallableUnits() );
    }
}
