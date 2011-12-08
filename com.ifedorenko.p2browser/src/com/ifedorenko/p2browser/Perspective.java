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

package com.ifedorenko.p2browser;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import com.ifedorenko.p2browser.views.MetadataRepositoryView;

public class Perspective
    implements IPerspectiveFactory
{
    public void createInitialLayout( IPageLayout layout )
    {
        layout.setEditorAreaVisible( false );

        String editorArea = layout.getEditorArea();
        IFolderLayout left = layout.createFolder( "left", IPageLayout.LEFT, IPageLayout.RATIO_MAX, editorArea );
        left.addView( MetadataRepositoryView.ID );
    }
}
