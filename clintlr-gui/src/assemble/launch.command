#!/usr/bin/env sh

##
# Launch ClintLR either by double-clicking on the script in file browser
# or by opening the Terminal and running the script manually.

current_dir=$(dirname $(readlink ${0} || echo ${0}))
cd ${current_dir}


# MP="${current_dir}/lib:${current_dir}/clintlr-gui-0.0.1.jar"
MP="${current_dir}:${current_dir}/lib"
printf "Module path: %s\n" ${MP}

java --module-path "${MP}" --module clintlr.gui