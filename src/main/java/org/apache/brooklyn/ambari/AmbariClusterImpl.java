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
package org.apache.brooklyn.ambari;

import brooklyn.entity.basic.BasicStartableImpl;
import brooklyn.entity.basic.SoftwareProcess;
import brooklyn.entity.group.DynamicCluster;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.software.ssh.SshCommandSensor;
import brooklyn.event.SensorEvent;
import brooklyn.event.SensorEventListener;
import brooklyn.location.Location;
import brooklyn.util.config.ConfigBag;
import brooklyn.util.time.Duration;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.brooklyn.ambari.agent.AmbariAgent;
import org.apache.brooklyn.ambari.server.AmbariServer;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static brooklyn.event.basic.DependentConfiguration.attributeWhenReady;

public class AmbariClusterImpl extends BasicStartableImpl implements AmbariCluster {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(BasicStartableImpl.class);
    public static final ImmutableList<String> DEFAULT_SERVICES = ImmutableList.<String>of("ZOOKEEPER");
    //TODO is there an issue with rebind here?  On rebind should be populated from somewhere else?
    private final ConcurrentHashMap<String, Boolean> registeredHosts = new ConcurrentHashMap<String, Boolean>();
    private AtomicBoolean clusterCreationInitialised = new AtomicBoolean(false);
    private Integer initialClusterSize;

    @Override
    public void init() {
        super.init();

        setDisplayName("Ambari Cluster");
        initialClusterSize = getConfig(INITIAL_SIZE);
        //TODO need to do something better with security groups here
        Object securityGroup = getConfig(SECURITY_GROUP);
        ImmutableMap<String, Object> serverProvisioningProperties = ImmutableMap.<String, Object>of( "inboundPorts", ImmutableList.of(8080, 22), "securityGroups", securityGroup,
                "minRam", 8192);

        SshCommandSensor<String> hostnameSensor = new SshCommandSensor<String>(ConfigBag.newInstance()
                .configure(SshCommandSensor.SENSOR_PERIOD, Duration.millis(100))
                .configure(SshCommandSensor.SENSOR_NAME, "fqdn")
                .configure(SshCommandSensor.SENSOR_COMMAND, "hostname -s"
                ));

        setAttribute(AMBARI_SERVER, addChild(getConfig(SERVER_SPEC)
                        .configure(SoftwareProcess.PROVISIONING_PROPERTIES, serverProvisioningProperties)
                        .displayName("Ambari Server")
                        .addInitializer(hostnameSensor)
        ));


        ImmutableMap<String, Object> agentProvisioningProperties = ImmutableMap.<String, Object>of(
                "minRam", 8192,
                "osFamily", "ubuntu",
                "osVersionRegex", "12.*",
                "securityGroups", securityGroup);

        EntitySpec<AmbariAgent> agentEntitySpec = EntitySpec.create(AmbariAgent.class)
                .configure(AmbariAgent.AMBARI_SERVER_FQDN, attributeWhenReady(getAttribute(AMBARI_SERVER), AmbariServer.HOSTNAME))
                        //TODO shouldn't use default os
                .addInitializer(hostnameSensor)
                .configure(SoftwareProcess.PROVISIONING_PROPERTIES, agentProvisioningProperties);

        setAttribute(AMBARI_AGENT, addChild(EntitySpec.create(DynamicCluster.class)
                        .configure(DynamicCluster.INITIAL_SIZE, initialClusterSize)
                        .configure(DynamicCluster.MEMBER_SPEC, agentEntitySpec)
                        .displayName("All Nodes")
        ));
    }

    @Override
    public void start(Collection<? extends Location> locations) {
        super.start(locations);

        subscribe(getAttribute(AMBARI_SERVER), AmbariServer.REGISTERED_HOSTS, registeredHostsEventListener);

        setAttribute(SERVICE_UP, Boolean.TRUE);
    }

    final SensorEventListener<List<String>> registeredHostsEventListener = new SensorEventListener<List<String>>() {
        @Override
        public void onEvent(SensorEvent<List<String>> event) {
            for (String host : event.getValue()) {
                registeredHosts.putIfAbsent(host, true);
            }
            if (registeredHosts.size() == initialClusterSize && !clusterCreationInitialised.getAndSet(true)) {
                createCluster();
            }
        }
    };

    private void createCluster() {
        getAttribute(AMBARI_SERVER).waitForServiceUp();
        List<String> services = getConfig(HADOOP_SERVICES);

        LOG.debug("About to create cluster with services: " + services);
        if (services.size() > 0) {
            getAttribute(AMBARI_SERVER).installHDP("Cluster1", "mybp", getListOfHosts(), services);
        } else {
            getAttribute(AMBARI_SERVER).installHDP("Cluster1", "mybp", getListOfHosts(), DEFAULT_SERVICES);
        }
    }

    private ImmutableList<String> getListOfHosts() {
        return ImmutableList.<String>copyOf(registeredHosts.keySet());
    }
}
