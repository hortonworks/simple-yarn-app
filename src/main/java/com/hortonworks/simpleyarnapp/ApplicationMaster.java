package com.hortonworks.simpleyarnapp;

import java.util.Collections;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.protocolrecords.AllocateResponse;
import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.ContainerLaunchContext;
import org.apache.hadoop.yarn.api.records.ContainerStatus;
import org.apache.hadoop.yarn.api.records.FinalApplicationStatus;
import org.apache.hadoop.yarn.api.records.Priority;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.client.api.AMRMClient;
import org.apache.hadoop.yarn.client.api.AMRMClient.ContainerRequest;
import org.apache.hadoop.yarn.client.api.NMClient;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.util.Records;

public class ApplicationMaster {

  public static void main(String[] args) throws Exception {

    final String command = args[0];
    final int n = Integer.valueOf(args[1]);
    
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

    // Obtain allocated containers and launch 
    int allocatedContainers = 0;
    // We need to start counting completed containers while still allocating
    // them since intial ones may complete while we're allocating subsequent
    // containers and if we miss those notifications, we'll never see them again
    // and this ApplicationMaster will hang indefinitely.
    int completedContainers = 0;
    while (allocatedContainers < n) {
      AllocateResponse response = rmClient.allocate(0);
      for (Container container : response.getAllocatedContainers()) {
        ++allocatedContainers;

        // Launch container by create ContainerLaunchContext
        ContainerLaunchContext ctx = 
            Records.newRecord(ContainerLaunchContext.class);
        ctx.setCommands(
            Collections.singletonList(
                command + 
                " 1>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/stdout" + 
                " 2>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/stderr" 
                ));
        System.out.println("Launching container " + allocatedContainers);
        nmClient.startContainer(container, ctx);
      }
      for (ContainerStatus status : response.getCompletedContainersStatuses()) {
        ++completedContainers;
        System.out.println("Completed container " + completedContainers);
      }
      Thread.sleep(100);
    }

    // Now wait for the remaining containers to complete 
    while (completedContainers < n) {
      AllocateResponse response = rmClient.allocate(completedContainers/n);
      for (ContainerStatus status : response.getCompletedContainersStatuses()) {
        ++completedContainers;
        System.out.println("Completed container " + completedContainers);
      }
      Thread.sleep(100);
    }

    // Un-register with ResourceManager
    rmClient.unregisterApplicationMaster(
        FinalApplicationStatus.SUCCEEDED, "", "");
  }
}
