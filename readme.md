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

p2browser uses the proxy configuration from the Java Control Panel. You should configure the 
proxy server manually (option 'Use proxy server') because 'Use browser settings' doesn't work and 
'Use automatic proxy configuration script' is not supported.
