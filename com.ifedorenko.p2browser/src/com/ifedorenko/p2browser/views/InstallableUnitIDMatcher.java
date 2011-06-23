package com.ifedorenko.p2browser.views;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;

class InstallableUnitIDMatcher
    extends AbstractPatternMatcher
    implements InstallableUnitMatcher
{
    public InstallableUnitIDMatcher( String pattern )
    {
        super( pattern );
    }

    @Override
    public boolean match( IInstallableUnit unit )
    {
        return match( unit.getId() );
    }

}
