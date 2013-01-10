
function plugin_install_artifact {
    echo WORKING
    id=$1
    installation_path=$2
    echo Installing ID=${id}

    case ${id} in

            'B' )
                env

                if [ -z "${RESOURCES_ARTIFACTS_PLUGIN1_A}" ]; then
                       echo "Failing to install since PLUGIN1/A did not install."
                       return 1;
                fi

                touch ${installation_path}/installed-file-B
                return 0
                ;;

            *)  echo "Resource artifact id not recognized: "${id}
                return 99
                ;;

    esac

    return 1


}

