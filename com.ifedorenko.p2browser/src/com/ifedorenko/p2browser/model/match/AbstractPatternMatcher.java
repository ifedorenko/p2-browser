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

package com.ifedorenko.p2browser.model.match;


abstract class AbstractPatternMatcher
    implements IInstallableUnitMatcher
{

    private final IMatchStrategy strategy;

    private final String pattern;

    protected AbstractPatternMatcher( IMatchStrategy strategy, String pattern )
    {
        this.strategy = strategy;
        this.pattern = pattern;
    }

    protected boolean match( String string )
    {
        return strategy.match( string, pattern );
    }
}
