package com.ifedorenko.p2browser.views;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.ifedorenko.p2browser.model.IGroupedInstallableUnits;

abstract class InstallableUnitContentProvider
    implements ITreeContentProvider
{

    @Override
    public void dispose()
    {
    }

    @Override
    public void inputChanged( Viewer viewer, Object oldInput, Object newInput )
    {
    }

    @Override
    public Object[] getChildren( Object parentElement )
    {
        if ( parentElement instanceof IGroupedInstallableUnits )
        {
            IGroupedInstallableUnits metadata = (IGroupedInstallableUnits) parentElement;
            return toViewNodes( metadata, metadata.getRootIncludedInstallableUnits() );
        }
        else if ( parentElement instanceof InstallableUnitNode )
        {
            InstallableUnitNode node = (InstallableUnitNode) parentElement;
            IGroupedInstallableUnits metadata = node.getMetadata();
            return toViewNodes( metadata, metadata.getIncludedInstallableUnits( node.getInstallableUnit(), false ) );
        }
        return null;
    }

    @Override
    public Object getParent( Object element )
    {
        return null;
    }

    @Override
    public boolean hasChildren( Object element )
    {
        Object[] children = getChildren( element );
        return children != null && children.length > 0;
    }

    protected Object[] toViewNodes( IGroupedInstallableUnits metadata, Collection<IInstallableUnit> units )
    {
        ArrayList<InstallableUnitNode> nodes = new ArrayList<InstallableUnitNode>();
        for ( IInstallableUnit unit : units )
        {
            nodes.add( new InstallableUnitNode( metadata, unit ) );
        }
        return nodes.toArray();
    }

}
