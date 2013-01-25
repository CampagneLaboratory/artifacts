
function plugin_install_artifact {
    echo WORKING
    id=$1
    installation_path=$2
    echo Installing ID=${id}

    case ${id} in

            'RANDOM' )
                sleep 3
                touch ${installation_path}/`date "+%m%d%H%M%Y.%S"`
                                return 0
                ;;


            *)  echo "Resource artifact id not recognized: "${id}
                return 99
                ;;

    esac

    return 1


}

