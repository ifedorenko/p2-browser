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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;

public class UngroupedInstallableUnits
    implements IGroupedInstallableUnits
{
    private Set<IInstallableUnit> units;

    public UngroupedInstallableUnits( Collection<IInstallableUnit> units )
    {
        this.units = Collections.unmodifiableSet( new LinkedHashSet<IInstallableUnit>( units ) );
    }

    @Override
    public int size()
    {
        return units.size();
    }

    @Override
    public Collection<IInstallableUnit> getInstallableUnits()
    {
        return units;
    }

    @Override
    public Collection<IInstallableUnit> getIncludedInstallableUnits( IInstallableUnit unit, boolean transitive )
    {
        return Collections.emptySet();
    }

    @Override
    public Collection<IInstallableUnit> getRootIncludedInstallableUnits()
    {
        return units;
    }

    public static UngroupedInstallableUnits getInstallableUnits( IQueryable<IInstallableUnit> queryable,
                                                                 IProgressMonitor monitor )
    {
        return new UngroupedInstallableUnits( queryable.query( QueryUtil.ALL_UNITS, monitor ).toUnmodifiableSet() );
    }

}
