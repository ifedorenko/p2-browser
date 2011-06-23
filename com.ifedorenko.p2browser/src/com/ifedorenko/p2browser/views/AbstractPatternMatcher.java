package com.ifedorenko.p2browser.views;

abstract class AbstractPatternMatcher
    implements InstallableUnitMatcher
{

    private final String pattern;

    protected AbstractPatternMatcher( String pattern )
    {
        this.pattern = pattern;

    }

    protected boolean match( String string )
    {
        return string != null && string.contains( pattern );
    }
}
