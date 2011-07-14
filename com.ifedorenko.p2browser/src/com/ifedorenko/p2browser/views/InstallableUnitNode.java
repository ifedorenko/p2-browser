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
import java.util.List;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;

import com.ifedorenko.p2browser.model.IGroupedInstallableUnits;

class InstallableUnitNode
{
    private final IGroupedInstallableUnits metadata;

    private final IInstallableUnit unit;

    private Collection<IInstallableUnit> transitiveChildren;

    public InstallableUnitNode( IGroupedInstallableUnits metadata, IInstallableUnit unit )
    {
        this.metadata = metadata;
        this.unit = unit;
    }

    public IGroupedInstallableUnits getMetadata()
    {
        return metadata;
    }

    public IInstallableUnit getInstallableUnit()
    {
        return unit;
    }

    public static Collection<IInstallableUnit> toInstallableUnits( Collection<InstallableUnitNode> nodes )
    {
        List<IInstallableUnit> result = new ArrayList<IInstallableUnit>();

        for ( InstallableUnitNode node : nodes )
        {
            result.add( node.getInstallableUnit() );
        }
        return result;
    }

    public boolean match( InstallableUnitMatcher matcher )
    {
        if ( matcher.match( unit ) )
        {
            return true;
        }

        if ( transitiveChildren == null )
        {
            transitiveChildren = metadata.getIncludedInstallableUnits( unit, true );
        }

        for ( IInstallableUnit child : transitiveChildren )
        {
            if ( matcher.match( child ) )
            {
                return true;
            }
        }

        return false;
    }

}
