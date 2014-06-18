#!/bin/bash -e
#
# run_test file_count thread_count iteration_count update_cycle [kill_cycle]"
#
# script which exercises the eaptest servlet
#
#   uploads file_count files
#   using thread_count parallel threads
#     lists a file iteration_count times
#     re-uploads single file content every update_cycle steps
#     re-uploads current file every kill_cycle steps (optional)
#
# file_count default == $FILE_COUNT_DEFAULT
# thread_count default == $THREAD_COUNT_DEFAULT
# iteration_count default == $ITERATION_COUNT_DEFAULT
# update_cycle default == $UPDATE_CYCLE_DEFAULT
# kill_cycle default == $KILL_CYCLE_DEFAULT

FILE_COUNT_DEFAULT=200
THREAD_COUNT_DEFAULT=200
ITERATION_COUNT_DEFAULT=2000
UPDATE_CYCLE_DEFAULT=10
KILL_CYCLE_DEFAULT=0

# the name of the server to use and the user id of the owner
# these are used to ssh into the server

SERVER=eaptest-andrewdinn.rhcloud.com
USER=538c5d065973ca7c780003a1

function post()
{
    curl -s $* http://$SERVER/upload > /dev/null
}

function upload()
{
  post -F "filename=$1" -F "file=@$2"
}

function list()
{
    if [ -z "$1" ]; then
        post -F "readonly="
    else
        post -F "filename=$1"
    fi
}

function delete()
{
    if [ -z "$1" ]; then
        post -F "delete="
    else
        post -F "delete=" -F "filename=$1"
    fi
}

function delete_all_files()
{
    delete
}

function upload_all_files()
{
    local -i k=0
    local -i count=$1

    while [ $k -lt $count ]; do
        upload data$k data
        k=k+1
    done
}

function exercise_server
{
    local -i i=0

    #echo "spawning $(date)"
    while [ $i -lt $2 ] ; do
        exercise_server_sub $1 $3 $4 $5 $i &
        i=i+1
    done

    #echo "joining $(date)"
    wait
    #echo "joined $(date)"
}

function exercise_server_sub
{
    local -i i=0                # i = iteration var
    local -i j=0                # j = i % update_cycle
    local -i k=0                # k = skew + (i / update_cycle) % file count
    local -i l=0                # l =  i % kill_cycle

    local -i count=$1
    local -i max=$2
    local -i update_cycle=$3
    local -i kill_cycle=$4
    local -i skew=$5

    #echo "starting subshell $skew"

    # k starts off skewed by the current shell number
    # to minimise contention for access to the same file
    # and also spread the updates across the whole file set
    # but we need to reset it to be in [0, file count)

    while [ $skew -ge $count ]; do
        $skew=$skew-$count;
    done

    k=$skew

    # do max operations
    while [ $i -lt $max ]; do
        # we increment the cycle counters here
        # but keep the current loop and file indices
        j=j+1
        l=l+1
        # delete old versions every kill cycle steps
        if [ $l -eq $kill_cycle ]; then
            # get rid of all versions
            delete data$k
            # and restart kill cycle
            l=0
        fi
        # do an update 1 in every cycle steps
        if [ $j -eq $update_cycle ]; then
            j=0
            upload data$k data
        else
            list data$k
        fi
        # move on to the next file
        k=k+1
        if [ $k -eq $count ]; then
            k=0
        fi
        # iterate
        i=i+1
    done
}

function remote()
{
    ssh $USER@$SERVER $*
}

function setup()
{
    # clear out the logs
    remote "rm -f app-root/logs/jbosseap.log app-root/data/.balloonstats.log"

    # stop the server
    remote "jbosseap/bin/control stop"

    # install the necessary java options

    if [ -z "$1" ]; then
        remote "rm -f .env/user_vars/JAVA_OPTS_EXT"
    else
        cat $1 | remote "cat > .env/user_vars/JAVA_OPTS_EXT"
    fi

    # record the JAVA_OPTS settings

    echo -n "JAVA_OPTS_EXT="

    remote 'echo $JAVA_OPTS_EXT'

    # start the server

    remote "jbosseap/bin/control start"
}

function grab_log()
{
    remote "cd app-root/data ; cat .balloonstats.log"
}

function do_test()
{
    local -i file_count=$1
    local -i thread_count=$2
    local -i max=$3
    local -i update_cycle=$4
    local -i kill_cycle=$5

    echo "file count $file_count"
    echo "thread count $thread_count"
    echo "iterations $max"
    echo "update every $update_cycle steps"
    echo "kill every $kill_cycle steps"

    # delete any files already on the server
    delete_all_files

    echo "deleted"

    # upload file_count files

    time upload_all_files $file_count

    echo "uploaded"


    time exercise_server $file_count $thread_count $max $update_cycle $kill_cycle
}

function usage()
{
    echo "usage  : $0 file_count iteration_count update_cycle [kill_cycle]"
    echo ""
    echo "    exercises the eaptest servlet"
    echo ""
    echo "      uploads file_count files"
    echo "      using thread_count parallel threads"
    echo "        lists a file iteration_count times"
    echo "        re-uploads single file content every update_cycle steps"
    echo "        kills all versions of current file every kill_cycle steps (optional)"
    echo ""
    echo "    file_count default == $FILE_COUNT_DEFAULT"
    echo "    thread_count default == $THREAD_COUNT_DEFAULT"
    echo "    iteration_count default == $ITERATION_COUNT_DEFAULT"
    echo "    update_cycle default == $UPDATE_CYCLE_DEFAULT"
    echo "    kill_cycle default == $KILL_CYCLE_DEFAULT"
}

function main()
{
    local -i file_count=$FILE_COUNT_DEFAULT
    local -i thread_count=$THREAD_COUNT_DEFAULT
    local -i iteration_count=$ITERATION_COUNT_DEFAULT
    local -i update_cycle=$UPDATE_CYCLE_DEFAULT
    local -i kill_cycle=$KILL_CYCLE_DEFAULT

    if [ $# -gt 5 ]; then
        usage
        exit 1
    fi

    if [ $# -eq 5 ]; then
        kill_cycle=$5
    fi

    if [ $# -ge 4 ]; then
        update_cycle=$4
    fi

    if [ $# -ge 3 ]; then
        iteration_count=$3
    fi

    if [ $# -ge 2 ]; then
        thread_count=$2
    fi

    if [ $# -ge 1 ]; then
        file_count=$1
    fi

    if [ $file_count -le 0 ]; then
        echo "error : invalid file_count"
        usage
        exit 1
    fi

    if [ $thread_count -le 0 ]; then
        echo "error : invalid thread_count"
        usage
        exit 1
    fi

    if [ $iteration_count -le 0 ]; then
        echo "error : invalid iteration_count"
        usage
        exit 1
    fi

    if [ $update_cycle -le 0 ]; then
        echo "error : invalid update_cycle"
        usage
        exit 1
    fi

    if [ $kill_cycle -lt 0 ]; then
        echo "error : invalid kill_cycle"
        usage
        exit 1
    fi

    local LOGDIR=results/`date +"%F-%T"`
    
    mkdir -p $LOGDIR

    setup JAVA_OPTS_EXT_PARDEF > $LOGDIR/pardef-setup 2>&1
    do_test $file_count $thread_count $iteration_count $update_cycle $kill_cycle >>$LOGDIR/pardef-times 2>&1
    grab_log >> $LOGDIR/pardef-stats 2>&1

    setup JAVA_OPTS_EXT_PAROPT > $LOGDIR/paropt-setup 2>&1
    do_test  $file_count $thread_count $iteration_count $update_cycle $kill_cycle >> $LOGDIR/paropt-times 2>&1
    grab_log >> $LOGDIR/paropt-stats 2>&1

    setup JAVA_OPTS_EXT_SERDEF > $LOGDIR/serdef-setup 2>&1
    do_test  $file_count $thread_count $iteration_count $update_cycle $kill_cycle >> $LOGDIR/serdef-times 2>&1
    grab_log >> $LOGDIR/serdef-stats 2>&1

    setup JAVA_OPTS_EXT_SEROPT > $LOGDIR/seropt-setup 2>&1
    do_test  $file_count $thread_count $iteration_count $update_cycle $kill_cycle >> $LOGDIR/seropt-times 2>&1
    grab_log >> $LOGDIR/seropt-stats 2>&1
}

main $*
