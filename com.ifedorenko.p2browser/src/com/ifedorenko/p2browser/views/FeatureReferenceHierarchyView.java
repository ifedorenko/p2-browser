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

package com.ifedorenko.p2browser.views;

import java.util.Collection;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryable;

import com.ifedorenko.p2browser.director.IInstallableUnitHierarchyCalculator;
import com.ifedorenko.p2browser.director.IncludingFeaturesCalculator;

public class FeatureReferenceHierarchyView
    extends AbstractInstallableUnitHierarchyView
{
    public static final String ID = "com.ifedorenko.p2browser.views.FeatureReferenceHierarchyView"; //$NON-NLS-1$

    @Override
    String getListSectionTitle()
    {
        return "All Including Features";
    }

    @Override
    String getHierarchySectionTitle()
    {
        return "Including Feature Hierarchy";
    }

    @Override
    protected IInstallableUnitHierarchyCalculator getCalculator( IQueryable<IInstallableUnit> units,
                                                                 Collection<IInstallableUnit> roots )
    {
        return new IncludingFeaturesCalculator( units, roots );
    }
}
