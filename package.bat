
: :: :
: : Run this script as `package.bat` to build a native app.
: : The script works on Windows platform.


: :: :
: : Static variables
SET SRC_DIR=l4ci-gui
SET APP_NAME="L4CI"
SET CMD_NAME="l4ci-gui"
SET VERSION=1.0.0
SET JAR_VERSION=0.1.0

SET BUILD_DIR=%SRC_DIR%\target
SET PACKAGE_DIR=%SRC_DIR%\pkg
SET JAR_NAME=l4ci-gui-%JAR_VERSION%.jar
: :JAR_NAME="l4ci-gui.jar"
SET MAIN_CLASS=org.monarchinitiative.l4ci.gui.StockUIApp
SET VENDOR="The Jackson Laboratory"
SET DESCRIPTION="L4CI is a Java application for incorporating clinical intuition into LIRICAL analysis."
SET COPYRIGHT="Copyright 2022, All rights reserved"
: :ICON="%BUILD_DIR%/classes/img/dna_rna_icon"

GOTO:MAIN

:detect_platform
    for /f "tokens=4-7 delims=[.] " %%i in ('ver') do (if %%i==Version (set %~1=%%j.%%k) else (set %~1=%%i.%%j))
EXIT /B 0

: : The following is not used since l4ci is not yet modular
:build_for_module_path
    : : copy the JAR and lib (dependencies)
    COPY %BUILD_DIR%\%JAR_NAME% %PACKAGE_DIR%
    COPY -r %BUILD_DIR%\lib %PACKAGE_DIR%\lib
    ECHO "Unknown platform Windows %v%\n. Abort."
EXIT /B 0

:build_for_class_path
  : : copy the fat JAR
  COPY %BUILD_DIR%\%JAR_NAME% %PACKAGE_DIR%
: :  COPY $BUILD_DIR/lib/* $PACKAGE_DIR%
  ECHO "Platform version is %~1"
  if "%~1" == "10.0" (
        jpackage --name %APP_NAME% ^
        --verbose ^
        --input %PACKAGE_DIR% ^
        --main-jar %JAR_NAME% ^
        --main-class %MAIN_CLASS% ^
        --app-version "%VERSION%" ^
        --win-console
: :        --license-file %LICENSE% ^
    : :    --icon "%ICON%.icns" ^
    : :--description %DESCRIPTION% ^
    : :--vendor %VENDOR% ^
    : :--copyright %COPYRIGHT% ^
  ) else (
    ECHO "Unknown platform %~1\n. Abort."
  )
EXIT /B 0

:MAIN
: :: :
: : Dynamic variables
: :

call:detect_platform PLATFORM
ECHO "Platform Version %PLATFORM%"
SET PACKAGE="classpath"
: :PACKAGE="modular"


: : 1. Build
ECHO "Building l4ci-gui\n"
: :mvn clean package


: : 2. Prepare packaging folder
SET PACKAGE_DIR=\tmp\l4ci_app\
ECHO "Creating temporary directory at %PACKAGE_DIR%"
mkdir %PACKAGE_DIR%

: : 3. Package for platform and package type
if %PACKAGE% == "modular" (
  ECHO "Packaging modular l4ci-gui for Windows %PLATFORM%"
  call:build_for_module_path
) else if %PACKAGE% == "classpath" (
  ECHO "Packaging classpath l4ci-gui for Windows %PLATFORM%"
  call:build_for_class_path %PLATFORM%
) else (
  ECHO "\nUnknown packaging type %PACKAGE%"
)

: : 4. Clean up the packaging folder
ECHO "Removing the temporary directory %PACKAGE_DIR%"
del %PACKAGE_DIR%
ECHO "Done!\n"