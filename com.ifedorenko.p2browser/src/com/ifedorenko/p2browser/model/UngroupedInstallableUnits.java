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
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;

import com.ifedorenko.p2browser.director.InstallableUnitDAG;
import com.ifedorenko.p2browser.director.InstallableUnitInfo;

public class UngroupedInstallableUnits
{

    public InstallableUnitDAG toInstallableUnitDAG( Iterator<IInstallableUnit> iter )
    {
        Collection<IInstallableUnit> collection = new ArrayList<IInstallableUnit>();
        Map<IInstallableUnit, InstallableUnitInfo> map = new LinkedHashMap<IInstallableUnit, InstallableUnitInfo>();

        while ( iter.hasNext() )
        {
            IInstallableUnit unit = iter.next();
            InstallableUnitInfo info = new InstallableUnitInfo( unit );
            collection.add( unit );
            map.put( unit, info );
        }

        return new InstallableUnitDAG( toArray( collection ), map );
    }

    public InstallableUnitDAG toInstallableUnitDAG( IQueryable<IInstallableUnit> queryable, IProgressMonitor monitor )
    {
        Iterator<IInstallableUnit> iter = queryable.query( QueryUtil.ALL_UNITS, monitor ).iterator();
        return toInstallableUnitDAG( iter );
    }

    private static IInstallableUnit[] toArray( Collection<IInstallableUnit> units )
    {
        return units.toArray( new IInstallableUnit[units.size()] );
    }

}
