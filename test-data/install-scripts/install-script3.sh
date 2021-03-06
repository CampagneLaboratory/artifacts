
function plugin_install_artifact {
    echo WORKING
    id=$1
    installation_path=$2
    echo Installing ID=${id}

    case ${id} in

            'INDEX' )
                echo "Organism=$3 Reference-build=$4"

                touch ${installation_path}/index-installed
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

    echo "get_attribute_values for ID=${id}"

cat >${out} << EOT
undefined-key=Hello World!
EOT
    return 0
}
