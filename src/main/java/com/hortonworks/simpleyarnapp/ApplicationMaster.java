package com.hortonworks.simpleyarnapp;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.protocolrecords.AllocateResponse;
import org.apache.hadoop.yarn.api.records.*;
import org.apache.hadoop.yarn.client.api.AMRMClient;
import org.apache.hadoop.yarn.client.api.AMRMClient.ContainerRequest;
import org.apache.hadoop.yarn.client.api.NMClient;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.util.Apps;
import org.apache.hadoop.yarn.util.ConverterUtils;
import org.apache.hadoop.yarn.util.Records;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApplicationMaster {

  public static void main(String[] args) throws Exception {

    final String command = args[0];
    final int n = Integer.valueOf(args[1]);
    final Path appJarPath = new Path(args[2]);
    
    // Initialize clients to ResourceManager and NodeManagers
    Configuration conf = new YarnConfiguration();

    AMRMClient<ContainerRequest> rmClient = AMRMClient.createAMRMClient();
    rmClient.init(conf);
    rmClient.start();

    NMClient nmClient = NMClient.createNMClient();
    nmClient.init(conf);
    nmClient.start();

    // Register with ResourceManager
    System.out.println("registerApplicationMaster 0");
    rmClient.registerApplicationMaster("", 0, "");
    System.out.println("registerApplicationMaster 1");
    
    // Priority for worker containers - priorities are intra-application
    Priority priority = Records.newRecord(Priority.class);
    priority.setPriority(0);

    // Resource requirements for worker containers
    Resource capability = Records.newRecord(Resource.class);
    capability.setMemory(128);
    capability.setVirtualCores(1);

    // Make container requests to ResourceManager
    for (int i = 0; i < n; ++i) {
      ContainerRequest containerAsk = new ContainerRequest(capability, null, null, priority);
      System.out.println("Making res-req " + i);
      rmClient.addContainerRequest(containerAsk);
    }

    // Obtain allocated containers, launch and check for responses
    int responseId = 0;
    int completedContainers = 0;
    while (completedContainers < n) {
        AllocateResponse response = rmClient.allocate(responseId++);
        for (Container container : response.getAllocatedContainers()) {
            // Launch container by create ContainerLaunchContext
            ContainerLaunchContext ctx =
                    Records.newRecord(ContainerLaunchContext.class);
          HashMap res = new HashMap<Path, LocalResource>();
          LocalResource appHelloJar = Records.newRecord(LocalResource.class);
          setupAppJar(appJarPath, appHelloJar);
          res.put("app.jar", appHelloJar);
          ctx.setLocalResources(res);
          Map<String, String> appMasterEnv = new HashMap<String, String>();
          setupAppEnv(appMasterEnv);
          ctx.setEnvironment(appMasterEnv);

          List<String> commands = new ArrayList<String>();
          commands.add("date +%s" +
            " 1>>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/stdout" +
            " 2>>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/stderr");
          commands.add(";ls " +
            " 1>>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/stdout" +
            " 2>>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/stderr");
          commands.add(";date +%s" +
            " 1>>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/stdout" +
            " 2>>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/stderr");
          commands.add(";java -cp app.jar com.hortonworks.simpleyarnapp.HelloWorld  " +
            " 1>>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/stdout" +
            " 2>>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/stderr");
          ctx.setCommands(commands);

            System.out.println("Launching container " + container.getId());
            nmClient.startContainer(container, ctx);
        }
        for (ContainerStatus status : response.getCompletedContainersStatuses()) {
            ++completedContainers;
            System.out.println("Completed container " + status.getContainerId());
        }
        Thread.sleep(100);
    }

    // Un-register with ResourceManager
    rmClient.unregisterApplicationMaster(
        FinalApplicationStatus.SUCCEEDED, "", "");
  }

  private static void setupAppJar(Path jarPath, LocalResource appMasterJar) throws IOException {
    Configuration conf = new YarnConfiguration();
    FileStatus jarStat = FileSystem.get(conf).getFileStatus(jarPath);
    appMasterJar.setResource(ConverterUtils.getYarnUrlFromPath(jarPath));
    appMasterJar.setSize(jarStat.getLen());
    appMasterJar.setTimestamp(jarStat.getModificationTime());
    appMasterJar.setType(LocalResourceType.FILE);
    appMasterJar.setVisibility(LocalResourceVisibility.PUBLIC);
  }

  private static void setupAppEnv(Map<String, String> appMasterEnv) {
    Configuration conf = new YarnConfiguration();
    for (String c : conf.getStrings(
      YarnConfiguration.YARN_APPLICATION_CLASSPATH,
      YarnConfiguration.DEFAULT_YARN_APPLICATION_CLASSPATH)) {
      Apps.addToEnvironment(appMasterEnv, ApplicationConstants.Environment.CLASSPATH.name(),
        c.trim());
    }
    Apps.addToEnvironment(appMasterEnv,
      ApplicationConstants.Environment.CLASSPATH.name(),
      ApplicationConstants.Environment.PWD.$() + File.separator + "*");
  }
}
