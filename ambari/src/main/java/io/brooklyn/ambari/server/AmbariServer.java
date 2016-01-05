/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.brooklyn.ambari.server;

import java.util.List;
import java.util.Map;

import org.apache.brooklyn.api.catalog.Catalog;
import org.apache.brooklyn.api.entity.ImplementedBy;
import org.apache.brooklyn.api.sensor.AttributeSensor;
import org.apache.brooklyn.core.annotation.Effector;
import org.apache.brooklyn.core.annotation.EffectorParam;
import org.apache.brooklyn.core.sensor.PortAttributeSensorAndConfigKey;
import org.apache.brooklyn.core.sensor.Sensors;

import com.google.common.reflect.TypeToken;

import io.brooklyn.ambari.AmbariNode;
import io.brooklyn.ambari.rest.AmbariApiException;
import io.brooklyn.ambari.rest.domain.RecommendationWrapper;
import io.brooklyn.ambari.rest.domain.RecommendationWrappers;
import io.brooklyn.ambari.rest.domain.Request;

@Catalog(name = "Ambari Server", description = "Ambari Server: part of an ambari cluster used to install and monitor a hadoop cluster.")
@ImplementedBy(AmbariServerImpl.class)
public interface AmbariServer extends AmbariNode {

    // TODO this value is read-only; changing its config value is not reflected in the deployed artifacts!
    PortAttributeSensorAndConfigKey HTTP_PORT =
            new PortAttributeSensorAndConfigKey("ambari.server.httpPort", "HTTP Port", "8080");

    AttributeSensor<List<String>> REGISTERED_HOSTS = Sensors.newSensor(
            new TypeToken<List<String>>() {
            },
            "ambari.server.registeredHosts",
            "List of registered agent names");

    AttributeSensor<Boolean> URL_REACHABLE = Sensors.newBooleanSensor("ambari.server.urlReachable");

    AttributeSensor<String> CLUSTER_STATE = Sensors.newStringSensor("ambari.server.clusterState");

    /**
     * @throws IllegalStateException if times out.
     */
    public void waitForServiceUp();

    /**
     * Retrieves the Ambari recommendations for the given hosts / services from the REST API.
     *
     * @param stackName    the stack name to use.
     * @param stackVersion the stack version to use.
     * @param hosts        a list of registered hosts to the Ambari server.
     * @param services     a list of services to install.
     * @return the recommendations formulated by Ambari.
     */
    public RecommendationWrappers getRecommendations(String stackName, String stackVersion, List<String> hosts, List<String> services);

    /**
     * Creates a new blueprint and deploys it, based on the Ambari recommendations. If an error occurred, the method
     * will throw an {@link AmbariApiException} for the error to be propagated properly to the tree.
     *
     * @param clusterName           the cluster name to use.
     * @param blueprintName         the blueprint name to use.
     * @param recommendationWrapper the Ambari recommendation to create the blueprint and deploy it.
     * @param config                the additional configuration for the Hadoop services.
     * @return a request corresponding to the Ambari's operation.
     */
    public Request deployCluster(String clusterName, String blueprintName, RecommendationWrapper recommendationWrapper, Map config) throws AmbariApiException;

    @Effector(description = "Adds a new host to a cluster")
    public void addHostToCluster(@EffectorParam(name = "Cluster name") String cluster,
                                 @EffectorParam(name = "Host FQDN") String hostName);

    @Effector(description = "Create, configure and install a cluster, based on Ambari recommendation from the given hosts and services")
    public void createCluster(@EffectorParam(name = "Cluster Name") String clusterName,
                              @EffectorParam(name = "Blueprint Name") String blueprintName,
                              @EffectorParam(name = "Stack Name") String stackName,
                              @EffectorParam(name = "Stack version") String stackVersion,
                              @EffectorParam(name = "Hosts", description = "List of FQDNs to add to cluster") List<String> hosts,
                              @EffectorParam(name = "Services", description = "List of services to install on cluster") List<String> services,
                              @EffectorParam(name = "Configurations", description = "Map of configurations to apply to blueprint") Map<String, Map> config);

    @Effector(description = "Update the stack url")
    public void updateStackRepository(@EffectorParam(name = "Stack Name") String stackName,
                                      @EffectorParam(name = "Stack Version") String stackVersion,
                                      @EffectorParam(name = "Operating System") String os,
                                      @EffectorParam(name = "Repository Name") String repoName,
                                      @EffectorParam(name = "Repository URL") String url);

    @Effector(description = "Add, install and start a new service to an existing cluster")
    public void addServiceToCluster(@EffectorParam(name = "cluster", description = "Cluster name") final String cluster,
                                    @EffectorParam(name = "service", description = "Service name") final String service,
                                    @EffectorParam(name = "mappings", description = "Mappings of component to host") Map<String, String> mappings,
                                    @EffectorParam(name = "configuration", description = "Services Configuration", nullable = true, defaultValue = EffectorParam.MAGIC_STRING_MEANING_NULL) Map<String, Map<Object, Object>> configuration);

    @Effector(description = "Create a new configuration for a specific service")
    public void createServiceConfiguration(@EffectorParam(name = "Cluster name") String cluster,
                                           @EffectorParam(name = "Service configuration key") String configurationKey,
                                           @EffectorParam(name = "Service configuration") Map<Object, Object> configuration);

    @Effector(description = "Add new host to a hostgroup and install components")
    public void addHostsToHostGroup(@EffectorParam(name = "Blueprint Name") String blueprintName,
                                    @EffectorParam(name = "Hostgroup Name") String hostgroupName,
                                    @EffectorParam(name = "Hosts") List<String> hosts,
                                    @EffectorParam(name = "Cluster Name") String cluster);

    @Effector(description = "Start a service on a cluster")
    public void startService(@EffectorParam(name = "Cluster name") String cluster,
                             @EffectorParam(name = "Service name") String service);
    /**
     * Are we installing the ambari agent on the same server as the ambari server?
     * Calculated based on whether any components are configured to be installed on server.
     *
     * @return true if agent to be installed on server
     */
    public boolean agentOnServer();
}


