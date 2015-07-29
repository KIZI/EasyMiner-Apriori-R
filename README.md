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
 
Next instructions were written only for the Debian distribution.
 
### R installation instructions with all required dependencies

First, all following commands should be run as root or with sudo prefix.

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
   ```
   
