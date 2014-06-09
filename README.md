Simple web app used to test jboss eap from the command line.

Overview
--------

The test application is a simple servlet which implements a data
cache. It can be deployed as is on an OpenShift developer JBossEAP
gear. It's purpose is to provider a test bed for investigating GC
performance in OpenShift.

The servlet maintains a hash table which associates names with data
elements. The data elements are stored as Java Strings and are
uploaded to the servlet by attaching the contents of a data file to a
PUT request. The file contents are delivered as a multi-part message.
The name of the data item may be supplied independent of the name of
the uploaded file so the same content may be uploaded under more than
one name.

The servlet cache does not simply associate a single version of the
data with each name. Each cache entry contains up to 10 versions of
the data. If more than 10 data items are uploaded under the same name
then the earliest loaded items are dropped from the cache in order to
ensure that no more than 10 copies are retained. This makes it easier
for tests to build up and retain a large data set which changes slowly
over time, a necessary circumstance for testing management of tenured
heaps.

Driving The Servlet
-------------------

A jsp page (input.jsp) provides the option to drive the servlet from a
web browser form. However, tests are expected to drive the servlet
from the command line using curl to perfom an HTTP POST request.
Script bin/runtest.sh defines various shell functions which provide an
example of how to drive the server (as well as a main function which
exercises the servlet with a variety of GC configurations). For all
valid commands the servlet reply is http/text formatted content
suitable for displaying in a web browser. The response may possibly
contain embedded pre-formatted text displaying the contents of a
previously uploaded file.

HTTP POST requests are posted to URL http://server-host/upload.
Requests must employ the -F option to curl to attach form field names
and values to the request. These field names and values configure the
operations the server is required to perform in response to the
request. The -F option is also used to attach file data when
performing an upload request.

The following options and associated values are recognised by the
server

    -F readonly=

    -F filename=foobar

    -F delete=

    -F file=@path/to/file

Only certain combinations of field/file data arguments are legitimate:

    -readonly=

     list all names. print a list of the names of all data elements
     currently in the hash table also showing the current number of
     copies for each entry. n.b. any value supplied after the = is
     ignored. this argument should only ever be supplied on its own.

    -F filename=foobar

    list file i.e. display the latest version of data item stored in
    the hashtable under the name supplied as the filename field value

    -F filename=foobar -delete=

    list and delete file. as above but also delete all copies of the
    named data item from the hash table.

    -F filename=foobar -F file=@/path/to/file

    upload and list. upload the contents of the file identified by the
    file field and add it to the set of data values stored under the
    supplied filename. print the supplied data in the reply.

    -F delete=

    delete all. delete all entries from the servlet hash table.

Preparation For Running The Test Script
---------------------------------------

The script bin/runtest.sh can be used to exercise the server. However,
before using it some configuration is required.

First, you need to edit the script in order to specify the server host
address and the user id which OpenShift created when you installed
JBossEAP. These are defined by the shell global variables SERVER and
USER declared at the top of the script.

Secondly, you will need to have enabled SSH login to your OpenShift
JBossEAP server by uploading ssh keys as explained during the
OpenShift installation process. This is necessary because script
runtest.sh needs to ssh into the server in order to configure the java
runtime environment (adding GC options to environment variable
JAVA_OPTS) and to start up/shut down the server.

Thirdly, you will need to modify the JBossEAP configuration files in
order to allow you to use the GC settings tested by the test script.
This is necessary to work around the fact that the current JBossEAP
configuration inserts -XX:+UseSerialGC into the options provided on
the java command line but does not allow those options to be modified
using a simple configuration change like an environment variable
setting. Not all tests want to use the serial garbage collector.

The workaround requires

    copying the JBossEAP standalone configuration script into the
    application data directory


    editing the copy to reomve the -XX:+UseSerialGC option


    setting an enviroment variable to point the JBossEAP startup
    script to use the copied configuration script


ssh into the server and from the home directory execute the following
commands:

    $ cat jbosseap/bin/standalone.conf | sed -e 's/-XX:+UseSerialGC -Xms40m //' > app-root/data/standalone.conf
    $ echo ${HOME}app-root/data/standalone.conf > .env/user_vars/RUN_CONF

Finally, you will need to install the balloon driver monitoring agent
into your JBossEAP server in order to collect statistics detailing GC
execution. The monitoring agent source code is available at the
following git repo

    https://github.com/adinn/balloon

It should build on any Linux OS installed with with gcc and
maven. n.b. if you are running on a 64 bit Linux OS then you need to
install packages which support building of 32 bit targets (e.g. on
Fedora install gcc-devel-i686). Clone the git repo and then build the
code by executing the following commands

    $ git clone https://github.com/adinn/balloon
    $ cd balloon
    $ make dist32

You need to insall the agent on your JBossEAP server under directory
app-root/data/balloon as follows

    $ cd ..
    $ tar -cvf - balloon/target/balloondriver-1.0.0.jar balloon/target/libballoon.so | ssh <user@server> "cd app-root/data


Running The Test Script
-----------------------

The test script exercises the server in four different GC
configurations, PARDEF, PAROPT, SERDEF and SEROPT. The java command
options which are used for each of these tests are defined in the
corresponding files JAVA_OPTS_EXT_PARDEF, etc as follows

    PARDEF
    -Xms40m -Xbootclasspath/a:${HOME}/app-root/data/balloon/target/balloondriver-1.0.0.jar -agentpath:${HOME}/app-root/data/balloon/target/libballoon.so=approot


    PAROPT
    -XX:GCTimeRatio=4 -XX:AdaptiveSizePolicyWeight=90 -XX:MinHeapFreeRatio=20 -XX:MaxHeapFreeRatio=40 -Xms40m -Xbootclasspath/a:${HOME}/app-root/data/balloon/target/balloondriver-1.0.0.jar -agentpath:${HOME}/app-root/data/balloon/target/libballoon.so=approot


    SERDEF
    -XX:+UseSerialGC -Xms40m -Xbootclasspath/a:${HOME}/app-root/data/balloon/target/balloondriver-1.0.0.jar -agentpath:${HOME}/app-root/data/balloon/target/libballoon.so=approot


    SEROPT
    -XX:+UseSerialGC -XX:MinHeapFreeRatio=20 -XX:MaxHeapFreeRatio=40 -Xms40m -Xbootclasspath/a:${HOME}/app-root/data/balloon/target/balloondriver-1.0.0.jar -agentpath:${HOME}/app-root/data/balloon/target/libballoon.so=approot


Before running each test the script uploads the associated file to the
server env directory under name

    .env/user_vars/JAVA_OPTS_EXT

This file sets the value of environment variable JAVA_OPTS_EXT,
ensuring that the relevant java command line options are used when the
server is started.

The test script uses a single data file (called data) for all upload
requests. Uploads use a series ofdifferent names, data1, data2, ... to
upload the same content into different cache entries. This file makes
the uploaded data size (and also the downloaded data size of file list
requests) approximately 2K.

Foe each GC configuration the test script uses ssh commands to

    stop the server


    delete the server log (app-root/log/jbosseap.log) and any previous
    balloon driver log (app-root/data/.balloonstats.log).


    upload the java GC options file to .env/user_vars/JAVA_OPTS_EXT


    start the server


    exercise the server


The test script is parameterised by 5 arguments all of which may be
defaulted.

    runtest file_count thread_count iteration_count update_cycle kill_cycle

The default values are

    file_count=200
    thread_count=200
    iteration_count=2000
    update_cycle=10
    kill_cycle=-1

The script starts by populating the servlet cache, uploading
file_count copies of the data file under names data0, data1 ...

It then spawns thread_count threads which perform a sequence of list
and upload commands on different files. Threads are skewed to start
operating on different files in the file set. Thread 1 rotates through
the files starting from data1, thread 2 starts from data2 and so on.

update_cycle determines the ratio of list to upload operations. So,
for example, with the default setting, 10, a thread will perform 9
list operations for the current file and then upload new contents on
the 10th operation. It then moves on to the next file in the set to do
9 more lists and an upload and so on. Each thread performs
iteration_count operations in all before it terminates. So, for
example, with file_count 200, iteration_count 2000 and update_count
10, each thread will read every file 9 times and update each file
once.

kill_cycle can be used to configure periodic purges of the cache. For
example, if it is supplied as 500 and all the other arguments take
their defaults then after a thread has performed 450 list operations
and 50 upload operations on its first 50 files it will delete the
whole cache, upload every single file and then continue for the next
500 operations before performing another purge and so on. The default
setting, -1, means that no purges are performed.

When all threads have completed execution the test script shuts down
the JBossEAP server collects the results from the test run. It logs
output printed by the JBossEAP server during startup and prints wall
clock times for execution of the initali upload and for execution of
all child thread requests. Finally, it copies the ballon driver log
file from the JBossEAP server to the local machine.

Results are ouput to files located in subdirectory

    results/YYYY-MM-DD-HH:MM:SS

The output files appear as

    pardef-setup
    pardef-stats
    pardef-times
    paropt-setup
    . . .

For each of the four cases the xxx-setup file lists the server startup
ouptut. It is retained just in case anything goes wrong during
startup.


The xxx-times file lists the values of the 5 script arguments and shows
the wallclock times for uploading the data files and for exercising
the server.

The xxx-stats file contains the contents of the balloon driver log
genertaed during the test run. This includes formatted reports of the
state of the server heap when selected young and old GCs occur. This
includes:

    individual and cunulative times spent in mutators and GC threads


    live and commited data sizes for the tenured heap at each GC


    cunulative average live and commited data sizes for the tenured
    heap measured across both the full run and the last 10 GCs.


    
Acknowledgements ----------------

Thanks to Eric Schabell for providing a previous JBossEAP app from
which this one was cloned.
