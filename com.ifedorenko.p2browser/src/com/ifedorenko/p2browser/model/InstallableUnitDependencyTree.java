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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;

import com.ifedorenko.p2browser.director.InstallableUnitDAG;
import com.ifedorenko.p2browser.director.InstallableUnitInfo;

public class InstallableUnitDependencyTree
    implements IGroupedInstallableUnits
{

    private final InstallableUnitDAG dependencyDAG;

    public InstallableUnitDependencyTree( InstallableUnitDAG dependencyTree )
    {
        this.dependencyDAG = dependencyTree;
    }

    @Override
    public int size()
    {
        return dependencyDAG.getInstallableUnits().size();
    }

    @Override
    public int getNodeCount()
    {
        return dependencyDAG.getNodeCount();
    }

    @Override
    public Collection<IInstallableUnit> getInstallableUnits()
    {
        return dependencyDAG.getInstallableUnits();
    }

    @Override
    public Collection<IInstallableUnit> getIncludedInstallableUnits( IInstallableUnit unit, boolean transitive )
    {
        InstallableUnitInfo parent = dependencyDAG.getInstallableUnit( unit );

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
                addIncludedInstallableUnits( dependencyDAG.getInstallableUnit( iu ).getChildren(), result );
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
        return toInstallableUnits( dependencyDAG.getRootInstallableUnits() );
    }
}
