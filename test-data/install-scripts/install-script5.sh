
function plugin_install_artifact {
    echo WORKING
    id=$1
    installation_path=$2
    echo Installing ID=${id}

    case ${id} in

            'A' )

                touch ${installation_path}/installed-file-A
                return 0
                ;;

            *)  echo "Resource artifact id not recognized: "${id}
                return 99
                ;;

    esac

    return 1


}

