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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;

public class InstallableUnitInfo
{
    private final IInstallableUnit unit;

    private final Map<IInstallableUnit, InstallableUnitInfo> children =
        new LinkedHashMap<IInstallableUnit, InstallableUnitInfo>();

    public InstallableUnitInfo( IInstallableUnit unit )
    {
        this.unit = unit;
    }

    public IInstallableUnit getInstallableUnit()
    {
        return unit;
    }

    public void addChild( InstallableUnitInfo child )
    {
        children.put( child.getInstallableUnit(), child );
    }

    public Collection<InstallableUnitInfo> getChildren()
    {
        return children.values();
    }
}
