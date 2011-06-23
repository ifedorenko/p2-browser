package com.ifedorenko.p2browser;

import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

public class ApplicationWorkbenchAdvisor
    extends WorkbenchAdvisor
{

    private static final String PERSPECTIVE_ID = "com.ifedorenko.p2browser.perspective";

    public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor( IWorkbenchWindowConfigurer configurer )
    {
        return new ApplicationWorkbenchWindowAdvisor( configurer );
    }

    public String getInitialWindowPerspectiveId()
    {
        return PERSPECTIVE_ID;
    }

}
