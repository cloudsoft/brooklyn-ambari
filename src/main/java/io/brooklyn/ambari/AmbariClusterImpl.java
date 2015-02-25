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
package io.brooklyn.ambari;

import static brooklyn.event.basic.DependentConfiguration.attributeWhenReady;
import static com.google.common.base.Preconditions.checkNotNull;
import io.brooklyn.ambari.agent.AmbariAgent;
import io.brooklyn.ambari.server.AmbariServer;

import java.util.Collection;
import java.util.List;

import org.slf4j.LoggerFactory;

import brooklyn.config.ConfigKey;
import brooklyn.enricher.Enrichers;
import brooklyn.entity.basic.Attributes;
import brooklyn.entity.basic.BasicStartableImpl;
import brooklyn.entity.basic.SoftwareProcess;
import brooklyn.entity.group.DynamicCluster;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.software.ssh.SshCommandSensor;
import brooklyn.event.SensorEvent;
import brooklyn.event.SensorEventListener;
import brooklyn.location.Location;
import brooklyn.util.config.ConfigBag;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
                "minRam", 8192,
                "osFamily", "ubuntu",
                "osVersionRegex", "12.*",
 */
public class AmbariClusterImpl extends BasicStartableImpl implements AmbariCluster {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(BasicStartableImpl.class);
    public static final ImmutableList<String> DEFAULT_SERVICES = ImmutableList.<String>of("ZOOKEEPER");
    //TODO is there an issue with rebind here?  On rebind should be populated from somewhere else?

    @Override
    public void init() {
        super.init();

        //TODO need to do something better with security groups here
        Object securityGroup = getConfig(SECURITY_GROUP);

        SshCommandSensor<String> hostnameSensor = new SshCommandSensor<String>(ConfigBag.newInstance()
                .configure(SshCommandSensor.SENSOR_NAME, "fqdn")
                .configure(SshCommandSensor.SENSOR_COMMAND, "hostname -s"
                ));

        // TODO Setting the securityGroup explicitly here could cause problems: if the brooklyn.properties
        //      location already has a security group defined, then this will override it?
        // Not specifying minRam, osFamily, etc because that can break some locations.
        setAttribute(AMBARI_SERVER, addChild(getConfig(SERVER_SPEC)
                        .configure(SoftwareProcess.PROVISIONING_PROPERTIES.subKey("securityGroups"), securityGroup)
                        .displayName("Ambari Server")
                        .addInitializer(hostnameSensor)
        ));


        ImmutableMap<String, Object> agentProvisioningProperties = ImmutableMap.<String, Object>of(
                "securityGroups", securityGroup);

        EntitySpec<AmbariAgent> agentEntitySpec = EntitySpec.create(AmbariAgent.class)
                .configure(SoftwareProcess.PROVISIONING_PROPERTIES.subKey("securityGroups"), securityGroup)
                .configure(AmbariAgent.AMBARI_SERVER_FQDN, attributeWhenReady(getAttribute(AMBARI_SERVER), AmbariServer.HOSTNAME))
                        //TODO shouldn't use default os
                .addInitializer(hostnameSensor)
                .configure(SoftwareProcess.PROVISIONING_PROPERTIES, agentProvisioningProperties);

        setAttribute(AMBARI_AGENTS, addChild(EntitySpec.create(DynamicCluster.class)
                        .configure(DynamicCluster.INITIAL_SIZE, getRequiredConfig(INITIAL_SIZE))
                        .configure(DynamicCluster.MEMBER_SPEC, agentEntitySpec)
                        .displayName("All Nodes")
        ));
        
        addEnricher(Enrichers.builder()
                .propagating(Attributes.MAIN_URI)
                .from(getAttribute(AMBARI_SERVER))
                .build());
    }

    @Override
    public void start(Collection<? extends Location> locations) {
        super.start(locations);

        // TODO We could try to prevent setting SERVICE_UP until the services are all intalled
        subscribe(getAttribute(AMBARI_SERVER), AmbariServer.REGISTERED_HOSTS, new RegisteredHostEventListener(this));
    }

    static final class RegisteredHostEventListener implements SensorEventListener<List<String>> {
        private final AmbariCluster entity;
        
        public RegisteredHostEventListener(AmbariCluster entity) {
            this.entity = entity;
        }
        
        @Override
        public void onEvent(SensorEvent<List<String>> event) {
            List<String> hosts = event.getValue();
            Integer initialClusterSize = entity.getConfig(INITIAL_SIZE);
            Boolean initialised = entity.getAttribute(CLUSTER_SERVICES_INITIALISE_CALLED);
            if (hosts != null && hosts.size() == initialClusterSize && !Boolean.TRUE.equals(initialised)) {
                entity.installServices();
            }
        }
    }

    @Override
    public void installServices() {
        setAttribute(CLUSTER_SERVICES_INITIALISE_CALLED, true);
        getAttribute(AMBARI_SERVER).waitForServiceUp();
        List<String> services = getConfig(HADOOP_SERVICES);
        List<String> hosts = getAttribute(AMBARI_SERVER).getAttribute(AmbariServer.REGISTERED_HOSTS);

        LOG.debug("About to create cluster with services: " + services);
        if (services.size() > 0) {
            getAttribute(AMBARI_SERVER).installHDP("Cluster1", "mybp", hosts, services);
        } else {
            getAttribute(AMBARI_SERVER).installHDP("Cluster1", "mybp", hosts, DEFAULT_SERVICES);
        }
    }

    private <T> T getRequiredConfig(ConfigKey<T> key) {
        return checkNotNull(getConfig(key), "config %s", key);
    }
}
