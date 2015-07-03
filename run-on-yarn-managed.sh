hadoop fs -copyFromLocal simple-yarn-app-1.1.0.jar /user/root/.
hadoop jar simple-yarn-app-1.1.0.jar com.hortonworks.simpleyarnapp.Client /bin/date 2 hdfs://localhost:8020/user/root/simple-yarn-app-1.1.0.jar
