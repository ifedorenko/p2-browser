package com.ifedorenko.p2browser.views;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

class InstallableUnitFilter
    extends ViewerFilter
{
    static enum FilterMode
    {
        iu, capability;
    }

    private String pattern;

    private InstallableUnitFilter.FilterMode mode;

    @Override
    public boolean select( Viewer viewer, Object parentElement, Object element )
    {
        if ( pattern == null )
        {
            return true;
        }

        if ( !( element instanceof InstallableUnitNode ) )
        {
            return true;
        }

        InstallableUnitNode node = (InstallableUnitNode) element;

        switch ( mode )
        {
            case iu:
                return node.match( new InstallableUnitIDMatcher( pattern ) );
            case capability:
                return node.match( new ProvidedCapabilityMatcher( pattern ) );
            default:
        }

        return true;
    }

    public void setFilter( InstallableUnitFilter.FilterMode mode, String pattern )
    {
        this.mode = mode;
        this.pattern = pattern;
    }
}