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

package io.brooklyn.ambari.service;

import static org.apache.brooklyn.util.text.Strings.trim;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.brooklyn.config.ConfigKey;
import org.apache.brooklyn.core.config.ConfigKeys;
import org.apache.brooklyn.entity.stock.BasicStartable;
import org.apache.brooklyn.util.core.flags.SetFromFlag;
import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;
import com.google.common.reflect.TypeToken;

import io.brooklyn.ambari.AmbariCluster;
import io.brooklyn.ambari.agent.AmbariAgent;

/**
 * Defines an "extra service" for the Hadoop cluster. An entity implementing this interface will be assured to be called
 * at two particular times within the Ambari lifecycle:
 * <ul>
 *     <ol>{@link ExtraService#preClusterDeploy(AmbariCluster)} will be call once all the Ambari agents and servers have
 *     been installed, just before deploying a new hadoop cluster.</ol>
 *     <ol>{@link ExtraService#postClusterDeploy(AmbariCluster)} once the hadoop cluster has been deployed.</ol>
 * </ul>
 */
public interface ExtraService extends BasicStartable {

    class ComponentMapping {

        private final String component;
        private final String host;

        public ComponentMapping(String mapping, String defaultHost) {
            Preconditions.checkNotNull(mapping, "Mapping is required");
            Preconditions.checkNotNull(defaultHost, "Default host is required");

            String host = null;
            String component = null;
            if (mapping.contains("|")) {
                String[] split = mapping.split("\\|");
                host = trim(split[1]);
                component = trim(split[0]);
            } else {
                host = defaultHost;
                component = mapping;

            }
            if (StringUtils.isEmpty(host)) {
                throw new IllegalArgumentException(String.format("Extra component \"%s\" is not bound to any host group. " +
                        "Please use \"%s\" configuration key for global binding or specify it by add \"|<host-group-name>\" after the component name",
                        component, COMPONENT_NAMES.getName()));
            }

            this.component = component;
            this.host = host;
        }

        public String getComponent() {
            return component;
        }

        public String getHost() {
            return host;
        }
    }

    @SetFromFlag("bindTo")
    ConfigKey<String> BIND_TO = ConfigKeys.newStringConfigKey("bindTo", "Name of component which will be use to determine the host to install RANGER");

    @SetFromFlag("serviceName")
    ConfigKey<String> SERVICE_NAME = ConfigKeys.newStringConfigKey("serviceName", "Name of the Hadoop service, identified by Ambari");

    @SetFromFlag("componentNames")
    ConfigKey<List<String>> COMPONENT_NAMES =
            ConfigKeys.newConfigKey(
                    new TypeToken<List<String>>() {},
                    "componentNames",
                    "List of component names for this Hadoop service, identified by Ambari. " +
                    "Items should be of form <component>|<hostGroup> or simply " +
                    "<component>.  If hostgroup is omitted it is assumed that the " +
                    "component should be installed on the bound hostgroup.");

    /**
     * Returns the list of mapping <component-name> <--> <host-group-name> for this extra service.
     *
     * @return a list of mapping.
     */
    @Nonnull
    List<ComponentMapping> getComponentMappings();

    /**
     * Returns the necessary configuration the extra services implementation need to pass to Ambari.
     * Deprecated, please use {@link ExtraService#getAmbariConfig(AmbariCluster)} instead.
     *
     * Note that the default implementation will still call this method.
     *
     * @return a map of configuration.
     */
    @Deprecated
    Map<String, Map> getAmbariConfig();

    /**
     * Returns the necessary configuration the extra services implementation need to pass to Ambari.
     *
     * @param ambariCluster the current Ambari cluster entity.
     * @return a map of configuration.
     */
    Map<String, Map> getAmbariConfig(AmbariCluster ambariCluster);

    /**
     * Called just before the hadoop cluster will be deployed. If an error occurred during this phase, the subclasses
     * should throw an {@link ExtraServiceException} for the error to be propagated properly to the tree.
     *
     * @param ambariCluster the current Ambari cluster entity.
     */
    void preClusterDeploy(AmbariCluster ambariCluster) throws ExtraServiceException;

    /**
     * Called just after the hadoop cluster has been deployed. If an error occurred during this phase, the subclasses
     * should throw an {@link ExtraServiceException} for the error to be propagated properly to the tree.
     *
     * @param ambariCluster the current Ambari cluster entity.
     */
    void postClusterDeploy(AmbariCluster ambariCluster) throws ExtraServiceException;

    /**
     * Called after new hosts provisioned but before they are added to cluster
     *
     * @param ambariCluster the current Ambari cluster entity.
     * @param newAgents the new created ambari agents
     */
    void preHostGroupScale(AmbariCluster ambariCluster, Iterable<AmbariAgent> newAgents);

    /**
     * Called after new hosts have been added to Ambari cluster, components installed, etc
     * @param ambariCluster the current Ambari cluster entity.
     * @param newAgents the new created ambari agents
     */
    void postHostGroupScale(AmbariCluster ambariCluster, Iterable<AmbariAgent> newAgents);
}

