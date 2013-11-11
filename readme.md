## Features

* Browse contents of p2 repository as a list or grouped by features
* Filter view by installable feature id or provided capabilities
* See composite repositories structure
* Save repository metadata and individual artifacts locally
* See installable unit dependency tree and list (try it, highly recommended!)
* See installable unit direct and indirect references
* See features that include installable unit

## Building and running p2-browser locally

As of 2013-11-11, p2-browser Java WebStart Launcher appears to be broken on all recent versions of Java. To build and 
run p2-browser as a standalone RCP application

* Make sure a recent version of JDK is installed and available on PATH
* If you don't already have Maven, download [latest version of Maven](http://maven.apache.org/download.cgi) and
  untar/unzip it to a local directory
* Clone this git repository locally
* From base directory of the clone run the following command

```
    <path-to-maven-install>/bin/mvn clean package
```

* RCP applications for all enabled platforms will be created under com.ifedorenko.p2browser.rcp/target/products directory.
  Both packed and unpacked versions are created

```
    ├── com.ifedorenko.p2browser.rcp
    │   ├── linux
    │   │   └── gtk
    │   │       ├── x86
    │   │       └── x86_64
    │   ├── macosx
    │   │   └── cocoa
    │   │       └── x86_64
    │   └── win32
    │       └── win32
    │           ├── x86
    │           └── x86_64
    ├── com.ifedorenko.p2browser.rcp-linux.gtk.x86.tar.gz
    ├── com.ifedorenko.p2browser.rcp-linux.gtk.x86_64.tar.gz
    ├── com.ifedorenko.p2browser.rcp-macosx.cocoa.x86_64.tar.gz
    ├── com.ifedorenko.p2browser.rcp-win32.win32.x86.zip
    └── com.ifedorenko.p2browser.rcp-win32.win32.x86_64.zip
```

* To start p2-browser, execute p2browser (or p2browser.exe, if you happen to be on Windows) from the root of RCP app
  for your platform.

## Java WebStart Launcher

On some platforms, p2browser can be started by [following this link](http://ifedorenko.github.com/p2-browser/javaws/com.ifedorenko.p2browser.jnlp).

If the link above does not work, which appear to be the case on some/many Linux distributions, then
p2browser can be started using the following command on command line

    javaws  http://ifedorenko.github.com/p2-browser/javaws/com.ifedorenko.p2browser.jnlp

