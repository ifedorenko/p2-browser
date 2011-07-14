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
