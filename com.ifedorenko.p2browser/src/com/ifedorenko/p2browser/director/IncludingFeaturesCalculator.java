/*******************************************************************************
 * Copyright (c) 2012 Igor Fedorenko
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

import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.query.IQueryable;

import com.ifedorenko.p2browser.model.IncludedInstallableUnits;

/**
 * @see IncludedInstallableUnits
 */
@SuppressWarnings( "restriction" )
public class IncludingFeaturesCalculator
    extends ReferencesCalculator
{

    public IncludingFeaturesCalculator( IQueryable<IInstallableUnit> units, Collection<IInstallableUnit> roots )
    {
        super( units, roots, true );
    }

    @Override
    protected boolean isInteresting( IInstallableUnit unit )
    {
        if ( !super.isInteresting( unit ) )
        {
            return false;
        }

        return Boolean.parseBoolean( unit.getProperty( InstallableUnitDescription.PROP_TYPE_GROUP ) );
    };

    @Override
    protected boolean isInteresting( IRequirement requirement )
    {
        if ( !super.isInteresting( requirement ) )
        {
            return false;
        }

        if ( requirement instanceof IRequiredCapability )
        {
            return IncludedInstallableUnits.isSingleVersion( ( (IRequiredCapability) requirement ).getRange() );
        }

        return false;
    }
}
