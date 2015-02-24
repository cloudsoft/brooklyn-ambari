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
package org.apache.brooklyn.ambari.server;

import java.util.List;

import brooklyn.catalog.Catalog;
import brooklyn.entity.annotation.Effector;
import brooklyn.entity.annotation.EffectorParam;
import brooklyn.entity.basic.SoftwareProcess;
import brooklyn.entity.java.UsesJava;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.PortAttributeSensorAndConfigKey;
import brooklyn.event.basic.Sensors;

import com.google.common.reflect.TypeToken;

@Catalog(name = "Ambari Server", description = "Ambari Server: part of an ambari cluster used to install and monitor a hadoop cluster.")
@ImplementedBy(AmbariServerImpl.class)
public interface AmbariServer extends SoftwareProcess, UsesJava {

    // TODO this value is read-only; changing its config value is not reflected in the deployed artifacts!
    PortAttributeSensorAndConfigKey HTTP_PORT =
            new PortAttributeSensorAndConfigKey("ambari.server.httpPort", "HTTP Port", "8080");

    AttributeSensor<List<String>> REGISTERED_HOSTS = Sensors.newSensor(
            new TypeToken<List<String>>() {},
            "ambari.server.registeredHosts", 
            "List of registered agent names");

    AttributeSensor<Boolean> URL_REACHABLE = Sensors.newBooleanSensor("ambari.server.urlReachable");

    /**
     * @throws IllegalStateException if times out.
     */
    public void waitForServiceUp();

    @Effector(description = "Adds a host to a cluster")
    public void addHostToCluster(@EffectorParam(name = "Cluster name") String cluster,
                                 @EffectorParam(name = "Host FQDN") String hostName);

    @Effector(description = "Add a service to a cluster")
    public void addServiceToCluster(@EffectorParam(name = "Cluster name") String cluster,
                                    @EffectorParam(name = "Service name") String service);

    @Effector(description = "Create component")
    public void addComponentToCluster(@EffectorParam(name = "Cluster name") String cluster,
                                      @EffectorParam(name = "Service name") String service,
                                      @EffectorParam(name = "Component name") String component);

    @Effector(description = "Create host component")
    public void createHostComponent(@EffectorParam(name = "Cluster name") String cluster,
                                    @EffectorParam(name = "Host FQDN") String hostName,
                                    @EffectorParam(name = "Component name") String component);

    @Effector(description = "Create and install cluster on hosts with services")
    public void installHDP(@EffectorParam(name = "Cluster Name") String clusterName,
                           @EffectorParam(name = "Blueprint Name") String blueprintName,
                           @EffectorParam(name = "Hosts", description = "List of FQDNs to add to cluster") List<String> hosts,
                           @EffectorParam(name = "Services", description = "List of services to install on cluster") List<String> services);
}
