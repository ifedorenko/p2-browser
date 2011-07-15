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

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import com.ifedorenko.p2browser.model.match.InstallableUnitIDMatcher;
import com.ifedorenko.p2browser.model.match.ProvidedCapabilityMatcher;

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