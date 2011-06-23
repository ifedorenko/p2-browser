package com.ifedorenko.p2browser.views;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

class InstallableUnitSorter
    extends ViewerSorter
{
    public int compare( Viewer viewer, Object e1, Object e2 )
    {
        IInstallableUnit u1 = null, u2 = null;
        if ( e1 instanceof InstallableUnitNode )
        {
            u1 = ( (InstallableUnitNode) e1 ).getInstallableUnit();
        }
        if ( e2 instanceof InstallableUnitNode )
        {
            u2 = ( (InstallableUnitNode) e2 ).getInstallableUnit();
        }
        if ( u1 != null && u2 != null )
        {
            if ( u1.getId().equals( u2.getId() ) )
            {
                return u1.getVersion().compareTo( u2.getVersion() );
            }
            return u1.getId().compareTo( u2.getId() );
        }
        return super.compare( viewer, e1, e2 );
    }
}