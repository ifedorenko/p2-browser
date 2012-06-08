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
package com.ifedorenko.p2browser.model.match;

public interface IMatchStrategy
{
    public static final IMatchStrategy PREFIX = new IMatchStrategy()
    {
        @Override
        public boolean match( String string, String pattern )
        {
            return string != null && string.contains( pattern );
        }
    };

    public static final IMatchStrategy EXACT = new IMatchStrategy()
    {
        @Override
        public boolean match( String string, String pattern )
        {
            return string != null && string.equals( pattern );
        }
    };

    public boolean match( String string, String pattern );
}
