
function plugin_install_artifact {
    echo WORKING
    id=$1
    installation_path=$2
    echo Installing ID=${id}

    case ${id} in

            'ARTIFACT' )
                 echo "get_attribute_values for ID=${id}"
                 set -xv
                 env
                 if [ -z "${RESOURCES_ARTIFACTS_A_ARTIFACT+xxx}"  ]; then

                    exit 1;
                 fi
                 if [ -z "${RESOURCES_ARTIFACTS_B_ARTIFACT+xxx}"  ]; then

                    exit 1;
                 fi
                 if [ -z "${RESOURCES_ARTIFACTS_C_ARTIFACT+xxx}"  ]; then

                    exit 1;

                 else
                   if [ ! -f  ${RESOURCES_ARTIFACTS_C_ARTIFACT}/installed ]    ]; then

                        exit 1;
                   fi
                   touch ${installation_path}/installed
                   exit 0;
                 fi

                ;;


            *)  echo "Resource artifact id not recognized: "${id}
                return 99
                ;;

    esac

    return 1
}
