
function plugin_install_artifact {
    echo WORKING
    id=$1
    installation_path=$2
    echo Installing ID=${id}

    case ${id} in

            'FILE1' )

                touch ${installation_path}/installed-file-1
                return 0
                ;;

            'NO-ATTRIBUTE' )
                touch ${installation_path}/no-attribute
                return 0
                ;;

             'FILE2' )
                 touch ${installation_path}/installed-file-2
                 return 0
                 ;;

            *)  echo "Resource artifact id not recognized: ${id}"
                return 99
                ;;


    esac

    return 1


}

function get_attribute_values() {

     id=$1
     out=$2
     case ${id} in
         'FILE1' )

             echo "get_attribute_values for ID=${id}"

             echo "attribute-A=${ENV_VA}" >>${out}
             echo "attribute-B=${ENV_VB}" >>${out}
         ;;
         'FILE2' )
              echo "get_attribute_values for ID=${id}"

              echo "attribute-A=${ENV_VA}" >>${out}
              echo "attribute-B=${ENV_VB}" >>${out}
          ;;

        'NO-ATTRIBUTE' )
         ;;
     esac
     return 0
}
