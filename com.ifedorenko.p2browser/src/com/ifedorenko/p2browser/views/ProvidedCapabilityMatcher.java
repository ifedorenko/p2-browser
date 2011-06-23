package com.ifedorenko.p2browser.views;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;

class ProvidedCapabilityMatcher
    extends AbstractPatternMatcher
    implements InstallableUnitMatcher
{
    public ProvidedCapabilityMatcher( String pattern )
    {
        super( pattern );
    }

    @Override
    public boolean match( IInstallableUnit unit )
    {
        for ( IProvidedCapability cap : unit.getProvidedCapabilities() )
        {
            if ( match( cap.getName() ) )
            {
                return true;
            }
        }
        return false;
    }

}
