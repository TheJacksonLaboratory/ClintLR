# Package ClintLR as a self-contained Java application

This file explains how we have packaged ClintLR as a standalone application.
This is an alternative to the traditional method for creating executable jar files for
Java applications (this is also possible using the standard ``mvn package`` command).

The packaging must be done on the target platform. For instance, to create the native app for Linux,
the packaging must be done on Linux, using the Linux JDK.


## Linux

Run the following to create a deb installer (for Debian, Ubuntu, and related systems).

```shell
bash package.sh
```

This will create a deb file called ``clintlr-gui_0.3.0_amd64.deb``. On some
systems, the file can be installed with a right-click. From the command line,
the following installs the package

```shell
sudo dpkg -i clintlr-gui_0.3.0_amd64.deb
```

This command will have the effect of installing the application in ``/opt/clintlr-gui``.
The application will not be on the system path unless you add it manually, but you should be able to start the app via
the ``Show Applications`` or ``Main Menu`` apps. Alternatively, the app can be started
with
```shell
 /opt/clintlr-gui/bin/clintlr-gui 
```
to remove the app, enter the following command
```shell
sudo dpkg remove clintlr-gui_0.3.0_amd64.deb
```

## Mac

The command
```shell
bash package.sh
```
will generate a file called ``clintlr-gui-0.3.0.dmg``. This file can
be used to install the package as usual (start with a double-click)

Note that the JAR files and the dmg installation files for M1 and intel macs are not
compatible with each other. We have uploaded one version of the
app for M1 Macs (``clintlr-gui-0.3.0-M1.dmg``) and one for older (intel)
Macs (````clintlr-gui-0.3.0-intel.dmg``). We produced the files on an M1 and an Intel mac and
changed the file names by hand.

## Windows

At present, we do not offer a Windows installation file, please use the JAR file as described in the README.