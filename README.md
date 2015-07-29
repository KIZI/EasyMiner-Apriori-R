# EasyMiner-Apriori-R
EasyMiner Core apriori version with R and MySQL 

## Installation

First pull the latest version from github and make a one jar file by this sbt command:
```
> one-jar
```

On the server this service requires these dependecies:
 * Java 8
 * R 3.2.x (with arules, rJava, RJDBC, Rserve, rCBA)
 * MySQL Java JDBC Connector
 
Next instructions have been written only for the Debian distribution.
 
### R installation instructions with all required dependencies

First, all following commands should be run as the root or with the sudo prefix.

1. Add the R repository by adding this line to /etc/apt/sources.list
   
   ```deb http://cran.r-project.org/bin/linux/debian wheezy-cran3/```
   
2. Install R

   ```
   apt-cache search ^r-.*
   apt-get update
   dpkg --get-selections | grep r-cran
   apt-get install r-base r-base-dev
   ```
   
3. Export JAVA_HOME environment variable with a path to the JDK folder
4. Run R with command: R
5. Install rJava by this command: ```install.packages("rJava")```
6. If the last step fails you should configure Java variables by this shell command: ```R CMD javareconf```. Then try to repeat the previous step.
7. Install other libraries:
   
   ```
   install.packages("RJDBC")
   install.packages("Rserve")
   install.packages("arules")
   install.packages("devtools")
   library("devtools")
   devtools::install_github("jaroslav-kuchar/rCBA")
   ```

### Start/Stop service for EasyMiner-Apriori-R

On the server side create some folder where this application will be located and copy the one jar file to this folder with name: easyminer-apriori-r.jar. In this folder create a jdbc folder and download mysql jdbc connector to this directory. After this, create rserve-start.R, rserve-stop.R and run files with these contents:

rserve-start.R
```
library(Rserve)
Rserve()
```

rserve-stop.R
```
library(RSclient)
rsc <- RSconnect()
RSshutdown(rsc)
```

run
```
#!/bin/bash  
# Script for running Rest Easyminer

export R_SERVER=127.0.0.1
export R_JDBC=/path/to/mysql-jdbc-connector/folder
export REST_ADDRESS=localhost
export REST_PORT=8888
cd /path/to/easyminer-apriori-r/folder
java -Duser.country=US -Duser.language=en -Dfile.encoding=UTF-8 -Djava.net.preferIPv4Stack=true -jar easyminer-apriori-r.jar > rest.log 2>&1
```

The final easyminer-apriori-r folder should look like this:

* jdbc
   * mysql-connector-java-5.1.34-bin.jar 
* easyminer-apriori-r.jar
* rserve-start.R
* rserve-stop.R
* run

Finally create the easyminer-apriori-r file in /etc/init.d with this content:

```
#!/bin/bash
### BEGIN INIT INFO
# Provides:          easyminer-apriori-r
# Required-Start:    $remote_fs $syslog
# Required-Stop:     $remote_fs $syslog
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: Easyminer apriori R rest
# Description:       Easyminer apriori R rest
### END INIT INFO

set -e

start() {
        echo "Starting easyminer apriori R rest service..."
        R CMD Rserve
        start-stop-daemon --start --make-pidfile --pidfile /var/run/easyminer-apriori-r.pid --background --exec /path/to/easyminer-apriori-r/folder/run
}

stop() {
        echo "Stopping easyminer apriori R rest service..."
        pkill -TERM -P $(cat /var/run/easyminer-apriori-r.pid)
        start-stop-daemon --stop --quiet --oknodo --pidfile /var/run/easyminer-apriori-r.pid
        Rscript /path/to/easyminer-apriori-r/folder/rserve-stop.R
}

#
# main()
#

case "$1" in
  start)
        start
        ;;
  stop)
        stop
        ;;
  restart|reload|condrestart)
        stop
        start
        ;;
  *)
        echo $"Usage: $0 {start|stop|restart|reload}"
        exit 1
esac
exit 0
```

After these steps you should be able to start/stop the rest service by these commands:

```
service easyminer-apriori-r start
service easyminer-apriori-r stop
```
