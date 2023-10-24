@ECHO OFF

:: ================================================================================================================== ::
::                  Launch ClintLR app by double-clicking on the batch script in the file browser.                       ::
:: ================================================================================================================== ::

%JAVA_HOME%\bin\java^
 --module-path "lib" --module clintlr.gui
