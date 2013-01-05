
function plugin_install_artifact {
    echo WORKING
    id=$1
    installation_path=$2
    echo Installing ID=${id}

    case ${id} in

            'FILE1' )
                # Create a file of size 1001, just beyond the quota to force FILE1 to be removed when we install FILE2
                dd if=/dev/zero  bs=1 count=1 seek=1001 of=${installation_path}/installed-file-1
                return 0
                ;;

            'FILE2' )
                 # Create a file of size 999, just before the quota. FILE2 should remain
                dd if=/dev/zero  bs=1 count=1 seek=999 of=${installation_path}/installed-file-2
                return 0
                ;;

            *)  echo "Resource artifact id not recognized: "${id}
                return 99
                ;;

    esac

    return 1


}

