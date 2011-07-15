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

import java.util.Comparator;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;

class InstallableUnitSorter
    implements Comparator<IInstallableUnit>
{
    @Override
    public int compare( IInstallableUnit u1, IInstallableUnit u2 )
    {
        if ( u1 != null && u2 != null )
        {
            if ( u1.getId().equalsIgnoreCase( u2.getId() ) )
            {
                return u1.getVersion().compareTo( u2.getVersion() );
            }
            return u1.getId().compareToIgnoreCase( u2.getId() );
        }
        throw new IllegalArgumentException();
    }
}