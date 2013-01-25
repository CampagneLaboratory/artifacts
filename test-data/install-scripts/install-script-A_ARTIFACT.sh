
function plugin_install_artifact {
    echo WORKING
    id=$1
    installation_path=$2
    echo Installing ID=${id}

    case ${id} in


            'ARTIFACT' )
                ATTRIBUTE=$3
                echo "get_attribute_values for ID=${id} ${ATTRIBUTE}"
                touch ${installation_path}/installed
                exit $?
                ;;


            *)  echo "Resource artifact id not recognized: "${id}
                return 99
                ;;

    esac

    return 1
}


function get_attribute_values() {

     id=$1
     out=$2
     case ${id} in
         'ARTIFACT' )

             echo "get_attribute_values for ID=${id}"

             echo "attribute-A=VA" >>${out}

         ;;

     esac
     return 0
}
