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
import com.ifedorenko.p2browser.director.ReferencesCalculator;

public class ReferenceHierarchyView
    extends AbstractInstallableUnitHierarchyView
{
    public static final String ID = "com.ifedorenko.p2browser.views.ReferenceHierarchyView"; //$NON-NLS-1$

    @Override
    String getListSectionTitle()
    {
        return "All References";
    }

    @Override
    String getHierarchySectionTitle()
    {
        return "Reference Hierarchy (unique paths only)";
    }

    @Override
    protected IInstallableUnitHierarchyCalculator getCalculator( IQueryable<IInstallableUnit> units,
                                                                 Collection<IInstallableUnit> roots )
    {
        return new ReferencesCalculator( units, roots );
    }
}
