simple-yarn-app
===============

Simple YARN application to run n copies of a unix command - deliberately kept simple (with minimal error handling etc.)

Usage:
======

### Unmanaged mode

$ bin/hadoop jar $HADOOP_YARN_HOME/share/hadoop/yarn/hadoop-yarn-applications-unmanaged-am-launcher-2.1.1-SNAPSHOT.jar Client -classpath simple-yarn-app-1.1.0.jar -cmd "java com.hortonworks.simpleyarnapp.ApplicationMaster /bin/date 2"

### Managed mode

$ bin/hadoop fs -copyFromLocal simple-yarn-app-1.1.0.jar /apps/simple/simple-yarn-app-1.1.0.jar

$ bin/hadoop jar simple-yarn-app-1.1.0.jar com.hortonworks.simpleyarnapp.Client /bin/date 2 hdfs://localhost:8020/apps/simple/simple-yarn-app-1.1.0.jar
  
    
