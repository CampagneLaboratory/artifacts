
function plugin_install_artifact {
    echo WORKING
    id=$1
    installation_path=$2
    echo Installing ID=${id}

    case ${id} in

            'ARTIFACT' )
                 echo "get_attribute_values for ID=${id}"
             # The next command will generate a command not found event, which must result in an installation failure.
             SJdksjdkjs

                exit 0
                ;;


            *)  echo "Resource artifact id not recognized: "${id}
                return 99
                ;;

    esac

    return 1
}
