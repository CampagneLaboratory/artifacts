
function plugin_install_artifact {
    echo WORKING
    id=$1
    installation_path=$2
    echo Installing ID=${id}

    case ${id} in

            'RANDOM' )

                touch ${installation_path}/`date "+%m%d%H%M%Y.%S"`
                return 0
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
         'RANDOM' )

             echo get_attribute_values for ID=${id}

             echo "attribute-A=VA" >>${out}
             echo "attribute-B=VB" >>${out}
         ;;

     esac
     return 0
}
