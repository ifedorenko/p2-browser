package com.ifedorenko.p2browser.model;

import java.util.Collection;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;

public interface IGroupedInstallableUnits
{
    public int size();

    public Collection<IInstallableUnit> getInstallableUnits();

    /**
     * Returns installable units that are directly only (transitive=false) or directly and indirectly (transitive=true)
     * included in the provided parent IU. Returns empty collection if no such units.
     */
    public Collection<IInstallableUnit> getIncludedInstallableUnits( IInstallableUnit unit, boolean transitive );

    public Collection<IInstallableUnit> getRootIncludedInstallableUnits();

}
