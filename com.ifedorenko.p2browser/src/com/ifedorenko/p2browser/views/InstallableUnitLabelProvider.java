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

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;

class InstallableUnitLabelProvider
    implements ILabelProvider
{
    @Override
    public void removeListener( ILabelProviderListener listener )
    {
    }

    @Override
    public boolean isLabelProperty( Object element, String property )
    {
        return false;
    }

    @Override
    public void dispose()
    {
    }

    @Override
    public void addListener( ILabelProviderListener listener )
    {
    }

    @Override
    public String getText( Object element )
    {
        IInstallableUnit iu = toInstallableUnit( element );

        if ( iu != null )
        {
            StringBuilder sb = new StringBuilder();
            if ( Boolean.parseBoolean( iu.getProperty( IInstallableUnit.PROP_PARTIAL_IU ) ) )
            {
                sb.append( "[PARTIAL] " );
            }
            sb.append( iu.getId() ).append( ' ' ).append( iu.getVersion().toString() );
            return sb.toString();
        }
        return element != null ? element.toString() : "<null>";
    }

    protected IInstallableUnit toInstallableUnit( Object element )
    {
        IInstallableUnit iu = null;
        if ( element instanceof InstallableUnitNode )
        {
            iu = ( (InstallableUnitNode) element ).getInstallableUnit();
        }
        else if ( element instanceof IInstallableUnit )
        {
            iu = (IInstallableUnit) element;
        }
        return iu;
    }

    @Override
    public Image getImage( Object element )
    {
        return null;
    }
}