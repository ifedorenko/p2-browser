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

import java.util.ArrayList;
import java.util.EventListener;
import java.util.EventObject;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import com.ifedorenko.p2browser.model.match.IInstallableUnitMatcher;
import com.ifedorenko.p2browser.model.match.InstallableUnitIDMatcher;
import com.ifedorenko.p2browser.model.match.ProvidedCapabilityMatcher;

public class InstallableUnitFilterComposite
    extends Composite
{
    public static interface IFilterChangeListener
        extends EventListener
    {
        public void filterChanged( EventObject event );
    }

    private List<IFilterChangeListener> listeners = new ArrayList<IFilterChangeListener>();

    private IInstallableUnitMatcher unitMatcher;

    public InstallableUnitFilterComposite( Composite parent, int style )
    {
        super( parent, style );
        GridLayout gridLayout = new GridLayout( 2, false );
        gridLayout.verticalSpacing = 0;
        gridLayout.marginWidth = 0;
        gridLayout.marginHeight = 0;
        setLayout( gridLayout );

        final Combo filterType = new Combo( this, SWT.READ_ONLY );
        filterType.setItems( new String[] { "Filter by IU", "Filter by capability" } );
        filterType.setText( filterType.getItem( 0 ) );

        final Text filterText = new Text( this, SWT.BORDER | SWT.H_SCROLL | SWT.SEARCH | SWT.CANCEL );
        filterText.setLayoutData( new GridData( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );

        ModifyListener filterChangeListener = new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                String pattern = trim( filterText.getText() );
                if ( pattern != null )
                {
                    switch ( filterType.getSelectionIndex() )
                    {
                        case 1:
                            unitMatcher = new ProvidedCapabilityMatcher( pattern );
                            break;
                        case 0:
                        default:
                            unitMatcher = new InstallableUnitIDMatcher( pattern );
                            break;
                    }
                }
                else
                {
                    unitMatcher = null;
                }
                EventObject e2 = new EventObject( InstallableUnitFilterComposite.this );
                for ( IFilterChangeListener listener : listeners )
                {
                    listener.filterChanged( e2 );
                }
            }
        };

        filterText.addModifyListener( filterChangeListener );
        filterType.addModifyListener( filterChangeListener );
    }

    @Override
    protected void checkSubclass()
    {
        // Disable the check that prevents subclassing of SWT components
    }

    public IInstallableUnitMatcher getMatcher()
    {
        return unitMatcher;
    }

    static String trim( String str )
    {
        if ( str == null )
        {
            return null;
        }
        str = str.trim();
        if ( "".equals( str ) )
        {
            return null;
        }
        return str;
    }

    public void addFilterChangeListener( IFilterChangeListener listener )
    {
        listeners.add( listener );
    }
}
