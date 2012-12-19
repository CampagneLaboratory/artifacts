
function plugin_install_artifact {
    echo WORKING
    id=$1
    installation_path=$2
    echo Installing ID=${id}

    case $1 in

            'FILE1' )
                touch ${installation_path}/installed-file-1
                return 0
                ;;

            'FILE2' )
                touch ${installation_path}/installed-file-2
                return 0
                ;;

            *)  echo "Resource artifact id not recognized: "${id}
                return 99
                ;;

    esac

    return 1


}

