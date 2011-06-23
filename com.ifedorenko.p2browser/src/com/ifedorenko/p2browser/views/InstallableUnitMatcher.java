package com.ifedorenko.p2browser.views;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;

interface InstallableUnitMatcher
{
    public boolean match( IInstallableUnit unit );
}