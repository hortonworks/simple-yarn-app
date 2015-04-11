simple-yarn-app
===============

Simple YARN application to run n copies of a unix command - deliberately kept simple (with minimal error handling etc.)

Usage:
======
$cluster_name=$(hdfs getconf -confKey fs.defaultFS) 

$sample_command=$(which date)

### Unmanaged mode

$ bin/hadoop jar $HADOOP_YARN_HOME/share/hadoop/yarn/hadoop-yarn-applications-unmanaged-am-launcher-2.1.1-SNAPSHOT.jar Client -classpath simple-yarn-app-1.0-SNAPSHOT.jar -cmd "java com.hortonworks.simpleyarnapp.ApplicationMaster $sample_command 2"

### Managed mode

$ bin/hadoop fs -copyFromLocal simple-yarn-app-1.0-SNAPSHOT.jar $cluster_name/apps/simple/simple-yarn-app-1.0-SNAPSHOT.jar

$ bin/hadoop jar simple-yarn-app-1.0-SNAPSHOT.jar com.hortonworks.simpleyarnapp.Client $sample_command 2 $cluster_name/apps/simple/simple-yarn-app-1.0-SNAPSHOT.jar

    
