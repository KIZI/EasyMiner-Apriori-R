# EasyMiner-Apriori-R
EasyMiner Core apriori version with R and MySQL 

## Installation


Pull the latest version from github

```
> git clone https://github.com/KIZI/EasyMiner-Apriori-R.git
```


Install SBT if not already installed: http://www.scala-sbt.org/0.13/tutorial/Installing-sbt-on-Linux.html

Run SBT with the following command in the project directory 

```
cd EasyMiner-Apriori-R/
sbt
```

Make a one jar file by this sbt command:
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

1. To get up-to-date  R version  add this line to /etc/apt/sources.list
   
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
7. Install devtools by this command: ```install.packages("devtools")```
8. If the last step fails install these dependencies:
   
   ```
   apt-get install libcurl4-openssl-dev
   apt-get install libxml2-dev
   ```
9. Install other libraries:
   
   ```
   install.packages("RJDBC")
   install.packages("Rserve")
   install.packages("arules")
   install.packages("devtools")
   library("devtools")
   devtools::install_github("jaroslav-kuchar/rCBA")
   ```

10. Install RSclient
 ```
 apt-get install r-cran-rsclient
 ```
### Start/Stop service for EasyMiner-Apriori-R

On the server side create some folder where this application will be located and copy the one jar file (target/scala-2.10/easyminer-apriori-r_2.10-1.0-one-jar) to this folder with name: easyminer-apriori-r.jar

In this folder create a ```jdbc``` folder and download mysql jdbc connector (from https://dev.mysql.com/downloads/connector/j/5.1.html) to this directory. 

After this, create ```rserve-start.R```, ```rserve-stop.R``` and ```run``` files with these contents:

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

In ```run``` specify absolute path to the application directory (at two places).

``` 
chmod +x run
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

In ```easyminer-apriori-r``` specify absolute path to the application directory (at two places).

``` 
chmod +x run
```

After these steps you should be able to start/stop the rest service by these commands:

```
service easyminer-apriori-r start
service easyminer-apriori-r stop
```

## Service description

The basic API path is: /api/v1

There are only two REST operations within this service:

1. Path: /api/v1/mine
   * Description: Create a mining task by some PMML task definition.
   * Method: POST
   * Required headers:
      * Accept: application/xml
      * Content-Type: application/xml; charset=UTF-8
   * Content body: PMML document with a task definition
   * Possible response codes:
      * 202: Task was accepted and is in progress. There is a path to the result page in the Location header.
      * 400: Wrong input task data.
      * 500: Wrong input task data or another internal error.
2. Path: /api/v1/result/{taskId}
   * Description: Return some result of the mining task.
   * Method: GET
   * Possible response codes:
      * 200: The task has been finished. It returns a result in the PMML format.
      * 202: This task is still in progress.
      * 404: The task is not exist or has been picked up.
      * 500: An error during mining process.

### Example use of the service

Some examples of input PMML files are in the [examples resource folder](https://github.com/KIZI/EasyMiner-Apriori-R/tree/master/examples). 

1. Replace the {{dbserver}}, {{dbname}} and {{dbpassword}} placeholders in test.sql

2. Import test.sql to mysql, e.g. using these mysql commands

3. Assuming that you have replaced ```{{dbname}}``` with ```experiments``` (your new database name):
```
create database experiments;
use experiments;
source test.sql;
```
4. Send HTTP POST request containing test.pmml to the  ```/api/v1/mine``` endpoint
```
 http://localhost:8888/api/v1/mine
```
The endpoint returns 202 if all went down well. Note the value of the Location header.

This might look as follows:
```
/api/v1/result/cd874827-e413-46d0-95a4-66379d13101a
```
5. Send HTTP GET request to the  ```/api/v1/result``` endpoint

 The query URL might look as follows:
```
http://localhost:8888/api/v1/result/cd874827-e413-46d0-95a4-66379d13101a
```

6. The output should contain 11 AssociationRule elements
 

A detailed description of the modified PMML model is contained here:
> Kliegr, Tomáš, and Jan Rauch. "An XML format for association rule models based on the GUHA method." Semantic Web Rules. Springer Berlin Heidelberg, 2010. 273-288.  
