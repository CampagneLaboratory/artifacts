
function plugin_install_artifact {
    echo WORKING
    id=$1
    installation_path=$2
    echo Installing ID=${id}

    case ${id} in

            'RANDOM' )
                 echo "get_attribute_values for ID=${id}"

             if [ -e ${TMPDIR}/FLAG ]; then

                echo "DOING INSTALLATION"
                touch ${installation_path}/`date "+%m%d%H%M%Y.%S"`
                rm ${TMPDIR}/FLAG
                exit 0

             else

                echo "FAILING INSTALLATION"
                touch ${TMPDIR}/FLAG
                exit 1
             fi

                exit 1
                ;;


            *)  echo "Resource artifact id not recognized: "${id}
                return 99
                ;;

    esac

    return 1
}
