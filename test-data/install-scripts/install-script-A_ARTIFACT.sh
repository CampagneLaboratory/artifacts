
function plugin_install_artifact {
    echo WORKING
    id=$1
    installation_path=$2
    echo Installing ID=${id}

    case ${id} in

            'ARTIFACT' )
                 echo "get_attribute_values for ID=${id}"
                sdsd
                 touch ${installation_path}/installed
                exit $?
                ;;


            *)  echo "Resource artifact id not recognized: "${id}
                return 99
                ;;

    esac

    return 1
}
