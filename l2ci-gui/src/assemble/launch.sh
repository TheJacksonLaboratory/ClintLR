#!/usr/bin/env sh

##
# Launch L4CI either by double-clicking on the script in file browser
# or by opening the Terminal and running the script manually.

#current_dir=$(dirname $(readlink ${0} || echo ${0}))

java --add-exports=javafx.base/com.sun.javafx.event=org.controlsfx.controls \
  --module-path "lib" --module l2ci.gui
