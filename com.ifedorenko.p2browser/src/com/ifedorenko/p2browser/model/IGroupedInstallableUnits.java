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

import org.eclipse.equinox.p2.metadata.IInstallableUnit;

public interface IGroupedInstallableUnits
{
    public int size();

    public int getNodeCount();

    public Collection<IInstallableUnit> getInstallableUnits();

    /**
     * Returns installable units that are directly only (transitive=false) or directly and indirectly (transitive=true)
     * included in the provided parent IU. Returns empty collection if no such units.
     */
    public Collection<IInstallableUnit> getIncludedInstallableUnits( IInstallableUnit unit, boolean transitive );

    public Collection<IInstallableUnit> getRootIncludedInstallableUnits();

}
