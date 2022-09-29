@ECHO OFF

:: ================================================================================================================== ::
::            Launch L4CI app either by double-clicking on the batch script script in the file browser.               ::
:: ================================================================================================================== ::

%JAVA_HOME%\bin\java^
 --add-exports=javafx.base/com.sun.javafx.event=org.controlsfx.controls^
 --module-path "lib" --module l2ci.gui
