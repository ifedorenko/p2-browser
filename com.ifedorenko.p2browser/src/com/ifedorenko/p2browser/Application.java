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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * This class controls all aspects of the application's execution
 */
public class Application implements IApplication {
    private static final ILog LOGGER = Activator.getDefault().getLog();

    private static final String WS_PROX_TYPE_NONE = "0";
    private static final String WS_PROX_TYPE_MANUAL = "1";
    private static final String WS_PROX_TYPE_AUTO = "2";
    private static final String WS_PROX_TYPE_BROWSER = "3";

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.equinox.app.IApplication#start(org.eclipse.equinox.app.IApplicationContext)
     */
    @Override
    public Object start(final IApplicationContext context) {
        final Display display = PlatformUI.createDisplay();
        try {
            logSystemProperties();
            configureProxy();
            logProxySettings();

            final int returnCode = PlatformUI.createAndRunWorkbench(display, new ApplicationWorkbenchAdvisor());
            if (returnCode == PlatformUI.RETURN_RESTART) {
                return IApplication.EXIT_RESTART;
            }
            return IApplication.EXIT_OK;
        } catch (final CoreException e) {
            final IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Initialization failed", e);
            LOGGER.log(status);
            return IApplication.EXIT_OK;
        } finally {
            display.dispose();
        }
    }

    private static void configureProxy() throws CoreException {
        // get a copy of system props because IProxyService overwrites standard Java proxy properties
        final Map<Object, Object> systemProperties = new HashMap<Object, Object>(System.getProperties());

        final BundleContext bc = Activator.getDefault().getBundle().getBundleContext();
        final ServiceReference<?> serviceReference = bc.getServiceReference(IProxyService.class.getName());
        try {
            final IProxyService proxyService = (IProxyService) bc.getService(serviceReference);

            if (System.getProperty("deployment.proxy.type") != null) {
                configureProxyViaWebStart(proxyService);
            } else {
                configureProxyViaStandardSystemProperties(systemProperties, proxyService);
            }
        } finally {
            bc.ungetService(serviceReference);
        }
    }

    private static void configureProxyViaWebStart(final IProxyService proxyService) throws CoreException {
        final String proxyType = System.getProperty("deployment.proxy.type");
        if (WS_PROX_TYPE_NONE.equals(proxyType)) {
            proxyService.setProxiesEnabled(false);
        } else if (WS_PROX_TYPE_MANUAL.equals(proxyType)) {
            final boolean proxySame = Boolean.getBoolean(System.getProperty("deployment.proxy.same"));
            final String httpProxyHost = System.getProperty("deployment.proxy.http.host");
            final String httpProxyPort = System.getProperty("deployment.proxy.http.port");
            final String httpsProxyHost = proxySame ? httpProxyHost : System.getProperty("deployment.proxy.https.host");
            final String httpsProxyPort = proxySame ? httpProxyPort : System.getProperty("deployment.proxy.https.port");
            final String nonProxyHosts = System.getProperty("deployment.proxy.bypass.list");

            if (httpProxyHost != null) {
                setProxyData(proxyService, IProxyData.HTTP_PROXY_TYPE, httpProxyHost, httpProxyPort);
            }
            if (httpsProxyHost != null) {
                setProxyData(proxyService, IProxyData.HTTPS_PROXY_TYPE, httpsProxyHost, httpsProxyPort);
            }

            if (nonProxyHosts != null) {
                proxyService.setNonProxiedHosts(nonProxyHosts.trim().split(","));
            }

            proxyService.setSystemProxiesEnabled(false);
            proxyService.setProxiesEnabled(true);
        } else if (WS_PROX_TYPE_AUTO.equals(proxyType)) {
            final IStatus status =
                new Status(IStatus.WARNING, Activator.PLUGIN_ID,
                        "Java WebStart proxy type AUTO is not supported, falling back to system proxyies.");
            LOGGER.log(status);
            proxyService.setSystemProxiesEnabled(true);
            proxyService.setProxiesEnabled(true);
        } else if (WS_PROX_TYPE_BROWSER.equals(proxyType)) {
            proxyService.setSystemProxiesEnabled(true);
            proxyService.setProxiesEnabled(true);
        } else {
            final IStatus status =
                new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                        "Java WebStart proxy type unknown, deployment.proxy.type=" + proxyType);
            LOGGER.log(status);
        }
    }

    private static void configureProxyViaStandardSystemProperties(final Map<Object, Object> systemProperties,
            final IProxyService proxyService) throws CoreException {
        final String httpProxyHost = (String) systemProperties.get("http.proxyHost");
        final String httpProxyPort = (String) systemProperties.get("http.proxyPort");
        final String httpsProxyHost = (String) systemProperties.get("https.proxyHost");
        final String httpsProxyPort = (String) systemProperties.get("https.proxyPort");
        final String nonProxyHosts = (String) systemProperties.get("http.nonProxyHosts");

        if (httpProxyHost != null) {
            setProxyData(proxyService, IProxyData.HTTP_PROXY_TYPE, httpProxyHost, httpProxyPort);
        }
        if (httpsProxyHost != null) {
            setProxyData(proxyService, IProxyData.HTTPS_PROXY_TYPE, httpsProxyHost, httpsProxyPort);
        }

        if (nonProxyHosts != null) {
            proxyService.setNonProxiedHosts(nonProxyHosts.trim().split("\\|"));
        }
        proxyService.setSystemProxiesEnabled(false);
        proxyService.setProxiesEnabled(true);
    }

    private static void setProxyData(final IProxyService proxyService, final String proxyType, final String proxyHost,
            final String proxPort) throws CoreException {
        final IProxyData proxyData = proxyService.getProxyData(proxyType);
        proxyData.setHost(proxyHost);

        int port = -1;
        try {
            port = Integer.parseInt(proxPort);
        } catch (final NumberFormatException e) {
            // ignore and use default (port 8080)
        }
        if (port < 0 || port > 65535) {
            port = 8080;
        }
        proxyData.setPort(port);

        proxyService.setProxyData(new IProxyData[] { proxyData });
    }

    private static void logProxySettings() {
        final BundleContext bc = Activator.getDefault().getBundle().getBundleContext();

        final ServiceReference<?> serviceReference = bc.getServiceReference(IProxyService.class.getName());
        try {
            final IProxyService proxyService = (IProxyService) bc.getService(serviceReference);

            IStatus status =
                new Status(IStatus.INFO, Activator.PLUGIN_ID, "proxiesEnabled=" + proxyService.isProxiesEnabled());
            LOGGER.log(status);

            final boolean hasSystemProxies = proxyService.hasSystemProxies();
            final boolean systemProxiesEnabled = proxyService.isSystemProxiesEnabled();
            status =
                new Status((!hasSystemProxies && systemProxiesEnabled) ? IStatus.ERROR : IStatus.INFO,
                        Activator.PLUGIN_ID, "hasSystemProxies=" + hasSystemProxies + ", systemProxiesEnabled="
                                + systemProxiesEnabled);
            LOGGER.log(status);

            for (final IProxyData proxyData : proxyService.getProxyData()) {
                final String message =
                    "Type=" + proxyData.getType() + ", host=" + proxyData.getHost() + ", port=" + proxyData.getPort();
                status = new Status(IStatus.INFO, Activator.PLUGIN_ID, message);
                LOGGER.log(status);
            }
            status =
                new Status(IStatus.INFO, Activator.PLUGIN_ID, "nonProxiedHosts="
                        + Arrays.toString(proxyService.getNonProxiedHosts()));
            LOGGER.log(status);

        } finally {
            bc.ungetService(serviceReference);
        }
    }

    // just for debugging
    private static void logSystemProperties() {
        final StringBuffer msg = new StringBuffer();
        msg.append("System Properties\n");
        for (final Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
            msg.append("    ");
            msg.append(entry.getKey());
            msg.append("=");
            msg.append(entry.getValue());
            msg.append("\n");
        }
        final IStatus status = new Status(IStatus.INFO, Activator.PLUGIN_ID, msg.toString());
        LOGGER.log(status);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.equinox.app.IApplication#stop()
     */
    @Override
    public void stop() {
        if (!PlatformUI.isWorkbenchRunning()) {
            return;
        }
        final IWorkbench workbench = PlatformUI.getWorkbench();
        final Display display = workbench.getDisplay();
        display.syncExec(new Runnable() {
            @Override
            public void run() {
                if (!display.isDisposed()) {
                    workbench.close();
                }
            }
        });
    }
}
