#!/usr/bin/env sh

current_dir=$(dirname $(readlink ${0} || echo ${0}))
cd ${current_dir}

# --add-reads org.monarchinitiative.hca.app=ALL-UNNAMED \
# --add-opens javafx.graphics/com.sun.javafx.sg.prism=ALL-UNNAMED \
# --add-opens javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED \
# --add-opens javafx.graphics/com.sun.javafx.util=ALL-UNNAMED \
# --add-opens javafx.base/com.sun.javafx.logging=ALL-UNNAMED \

# current_dir=/Users/beckwm/IdeaProjects/L4CI
# Worked on CLI
# -p "${current_dir}/l2ci-gui/target/lib:${current_dir}/l2ci-gui/target/l2ci-gui-0.0.1.jar" -m l2ci.gui
# --module-path "${current_dir}/lib:${current_dir}/l2ci-gui-@project.version@.jar" --module l2ci.gui

# MP="${current_dir}/lib:${current_dir}/l2ci-gui-0.0.1.jar"
MP="${current_dir}:${current_dir}/lib"
printf "Module path: %s%n" ${MP}

java --add-exports=javafx.base/com.sun.javafx.event=org.controlsfx.controls \
  --module-path "${MP}" --module l2ci.gui
