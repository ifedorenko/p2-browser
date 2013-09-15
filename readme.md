## Features

* Browse contents of p2 repository as a list or grouped by features
* Filter view by installable feature id or provided capabilities
* See composite repositories structure
* Save repository metadata and individual artifacts locally
* See installable unit dependency tree and list (try it, highly recommended!)
* See installable unit direct and indirect references
* See features that include installable unit

## Java WebStart Launcher

On some platforms, p2browser can be started by [following this link](http://ifedorenko.github.com/p2-browser/javaws/com.ifedorenko.p2browser.jnlp).

If the link above does not work, which appear to be the case on some/many Linux distributions, then
p2browser can be started using the following command on command line

    javaws  http://ifedorenko.github.com/p2-browser/javaws/com.ifedorenko.p2browser.jnlp

## Build and run locally

First, clone the project, then build it:

    git clone git@github.com:ifedorenko/p2-browser.git
    cd p2-browser

    mvn install

If on Linux or OSX, find the executable for your OS, and run it:

    ls com.ifedorenko.p2browser.rcp/target/products/com.ifedorenko.p2browser.rcp/*/*/${HOSTTYPE}/p2browser
    ./com.ifedorenko.p2browser.rcp/target/products/com.ifedorenko.p2browser.rcp/linux/gtk/x86_64/p2browser

For Windows, double-click the executable one of these:

    com.ifedorenko.p2browser.rcp/target/products/com.ifedorenko.p2browser.rcp/win32/win32/x86_64/p2browser.exe
    com.ifedorenko.p2browser.rcp/target/products/com.ifedorenko.p2browser.rcp/win32/win32/x86/p2browser.exe
