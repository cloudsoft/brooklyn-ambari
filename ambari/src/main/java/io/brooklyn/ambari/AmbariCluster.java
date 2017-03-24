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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import org.apache.brooklyn.api.catalog.Catalog;
import org.apache.brooklyn.api.entity.EntitySpec;
import org.apache.brooklyn.api.entity.ImplementedBy;
import org.apache.brooklyn.api.mgmt.Task;
import org.apache.brooklyn.api.sensor.AttributeSensor;
import org.apache.brooklyn.config.ConfigKey;
import org.apache.brooklyn.core.config.BasicConfigKey;
import org.apache.brooklyn.core.config.ConfigKeys;
import org.apache.brooklyn.core.config.MapConfigKey;
import org.apache.brooklyn.core.sensor.Sensors;
import org.apache.brooklyn.entity.group.Cluster;
import org.apache.brooklyn.entity.software.base.SoftwareProcess;
import org.apache.brooklyn.entity.stock.BasicStartable;
import org.apache.brooklyn.util.core.flags.SetFromFlag;

import com.google.common.base.Function;
import com.google.common.reflect.TypeToken;

import io.brooklyn.ambari.agent.AmbariAgent;
import io.brooklyn.ambari.rest.AmbariApiException;
import io.brooklyn.ambari.server.AmbariServer;
import io.brooklyn.ambari.service.ExtraService;
import io.brooklyn.ambari.service.ExtraServiceException;
import org.apache.brooklyn.util.text.Strings;

@Catalog(name = "Ambari Cluster", description = "Ambari Cluster: Made up of one or more Ambari Server and One or more Ambari Agents")
@ImplementedBy(AmbariClusterImpl.class)
public interface AmbariCluster extends BasicStartable {

    @SetFromFlag("initialSize")
    ConfigKey<Integer> INITIAL_SIZE = ConfigKeys.newConfigKeyWithDefault(Cluster.INITIAL_SIZE, 0);

    AttributeSensor<Integer> EXPECTED_AGENTS = Sensors.newIntegerSensor("ambaricluster.expectedservers", "Number of ambari agents expected to register with cluster");

    @SetFromFlag("blueprint.name")
    ConfigKey<String> BLUEPRINT_NAME = ConfigKeys.newStringConfigKey("blueprint.name", "Ambari Blueprint name", "mybp");

    @SetFromFlag("cluster.name")
    ConfigKey<String> CLUSTER_NAME = ConfigKeys.newStringConfigKey("cluster.name", "Ambari Cluster name", "Cluster1");

    @SetFromFlag("securityGroup")
    ConfigKey<String> SECURITY_GROUP = ConfigKeys.newStringConfigKey("securityGroup", "Security group to be shared by agents and server");

    @SetFromFlag("services")
    ConfigKey<List<String>> HADOOP_SERVICES = ConfigKeys.newConfigKey(new TypeToken<List<String>>() {
    }, "services", "List of services to deploy to Hadoop Cluster");

    @SetFromFlag("stackName")
    ConfigKey<String> HADOOP_STACK_NAME = ConfigKeys.newStringConfigKey("stackName", "Hadoop stack name", "HDP");

    @SetFromFlag("stackVersion")
    ConfigKey<String> HADOOP_STACK_VERSION = ConfigKeys.newStringConfigKey("stackVersion", "Hadoop stack version", "2.3");

    @SetFromFlag("repoBaseUrl")
    ConfigKey<? extends String> REPO_BASE_URL = ConfigKeys.newStringConfigKey("repository.base.url", "The base url of the ambari repo", "http://public-repo-1.hortonworks.com");

    @Deprecated
    @SetFromFlag("extraServices")
    ConfigKey<List<EntitySpec<? extends ExtraService>>> EXTRA_HADOOP_SERVICES = BasicConfigKey.builder(new TypeToken<List<EntitySpec<? extends ExtraService>>>() {
    })
            .name("extraServices")
            .description("List of extra services to deploy to Hadoop Cluster " +
                    "NB: this configuration parameter doesn't work in yaml")
            .defaultValue(new LinkedList<EntitySpec<? extends ExtraService>>())
            .build();

    @Deprecated
    @SetFromFlag("extraService")
    ConfigKey<EntitySpec<? extends ExtraService>> EXTRA_HADOOP_SERVICE = BasicConfigKey.builder(new TypeToken<EntitySpec<? extends ExtraService>>() {
    })
            .name("extraService")
            .description("List of extra services to deploy to Hadoop Cluster")
            .build();

    ConfigKey<EntitySpec<? extends AmbariServer>> SERVER_SPEC = BasicConfigKey.builder(new TypeToken<EntitySpec<? extends AmbariServer>>() {
    }).name("ambaricluster.serverspec")
            .defaultValue(EntitySpec.create(AmbariServer.class))
            .build();

    ConfigKey<EntitySpec<? extends AmbariAgent>> AGENT_SPEC = BasicConfigKey.builder(new TypeToken<EntitySpec<? extends AmbariAgent>>() {
    }
    ).name("ambaricluster.agentspec")
            .defaultValue(EntitySpec.create(AmbariAgent.class))
            .build();

    @SetFromFlag("hostAddressSensor")
    ConfigKey<AttributeSensor<String>> ETC_HOST_ADDRESS = AmbariConfigAndSensors.ETC_HOST_ADDRESS;

    @SetFromFlag("version")
    ConfigKey<String> SUGGESTED_VERSION = ConfigKeys.newConfigKeyWithDefault(SoftwareProcess.SUGGESTED_VERSION, "2.2.0.0");

    @SetFromFlag("serverComponents")
    ConfigKey<List<String>> SERVER_COMPONENTS =
            ConfigKeys.newConfigKey(
                    new TypeToken<List<String>>() {
                    }, "ambari.server.components", "List of components to install on Ambari Server.  " +
                            "If non-empty then ambari agent will be added to server", new LinkedList<String>());

    @SetFromFlag("ambariConfigMap")
    ConfigKey<Map<String, Map>> AMBARI_CONFIGURATIONS =
            new MapConfigKey<Map>(Map.class, "ambari.configurations", "Map of maps");

    @SetFromFlag("ambariStackDefsUrls")
    ConfigKey<List<String>> STACK_DEFINITION_URLS =
            ConfigKeys.newConfigKey(new TypeToken<List<String>>() {
            }, "ambari.stack.urls", "stack definitions as tar.gz", new LinkedList<String>());

    @SetFromFlag("pauseForDeployment")
    ConfigKey<Boolean> PAUSE_FOR_DEPLOYMENT =
            ConfigKeys.newBooleanConfigKey(
                    "ambari.pause.before.deployment",
                    "Pauses once ambari server and clusters ready to deploy.  Creates deployCluster " +
                            "effector to continue deployment",
                    Boolean.FALSE);

    @SetFromFlag("domainName")
    ConfigKey<String> DOMAIN_NAME = ConfigKeys.newStringConfigKey("ambari.domain.name", "Domain name to use for all hosts FQDNs", "ambari.local");

    @SetFromFlag("serverHostGroup")
    ConfigKey<String> SERVER_HOST_GROUP = ConfigKeys.newStringConfigKey("ambari.server.hostgroup.name", "Host group name for the agent on the Ambari server", "server-group");

    AttributeSensor<Boolean> CLUSTER_SERVICES_INITIALISE_CALLED = Sensors.newBooleanSensor("ambari.cluster.servicesInitialiseCalled");

    AttributeSensor<Boolean> CLUSTER_SERVICES_INSTALLED = Sensors.newBooleanSensor("ambari.cluster.servicesInstalled");


    String AMBARI_ALERTS_CONFIG_PREFIX = "ambari.alerts.notification.";

    String AMBARI_ALERTS_NOTIFICATION_PROPERTIES_PREFIX = "properties.";

    @SetFromFlag("ambariAlertNotifications")
    ConfigKey<Map<String, Object>> AMBARI_ALERT_NOTIFICATIONS =
            new MapConfigKey(Map.class, Strings.removeFromEnd(AMBARI_ALERTS_CONFIG_PREFIX, "."),
                    "Map compatible with Ambari requirements for creating/editing alert notification request");


    List<String> AMBARI_ALERTS_NOTIFICATION_LIST_KEYS = ImmutableList.of(
            "alert_states",
            AMBARI_ALERTS_NOTIFICATION_PROPERTIES_PREFIX + "ambari.dispatch.recipients");

    /**
     * Returns all Ambari nodes, i.e {@link AmbariServer} and {@link AmbariAgent} contains within the cluster.
     *
     * @return a collection of Ambari nodes.
     */
    Iterable<AmbariNode> getAmbariNodes();

    /**
     * Returns the Ambari servers installed on the cluster.
     *
     * @return a collection of Ambari servers, if applicable.
     */
    Iterable<AmbariServer> getAmbariServers();

    /**
     * Returns the Ambari agents installed on the cluster.
     *
     * @return a collection of Ambari agents.
     */
    Iterable<AmbariAgent> getAmbariAgents();

    /**
     * Returns the first Ambari server installed on the cluster. This is fine for now as we support only one server
     * for the entire hadoop cluster and therefore, this method will always return the same result.
     * <p/>
     * TODO: This however will need to be changed to properly handle a "cluster of server" once HA will be implemented
     *
     * @return the first Ambari server.
     */
    AmbariServer getMasterAmbariServer();

    /**
     * Configure and deploy a new Hadoop cluster on the registered Ambari agents.
     */
    void deployCluster() throws AmbariApiException, ExtraServiceException;

    /**
     * Call after a the hadoop cluster has been deployed
     */
    // TODO: Rename to postClusterDeploy to be consistent
    void postDeployCluster() throws ExtraServiceException;

    /**
     * Urls for extra stack definitions e.g. Kerberos
     *
     * @return List of Strings of form http://host/def.tar.gz
     */
    List<String> getExtraStackDefinitionsUrls();

    /**
     * Add hosts to hostgroup.  Instructs ambari to install services as required by current blueprint.
     * @param displayName the name of the hostgroup that nodes should be added to
     * @param hosts The list of new ambari agent entities
     */
    void addHostsToHostGroup(String displayName, List<AmbariAgent> hosts);

    /**
     * Add alert notification
     * @param name Notification name
     * @param description Notification description
     * @param global Is notification global for all groups
     * @param notificationType EMAIL or SNMP
     * @param alertStates Alert status changes for which a notification will be send - OK, WARNING, CRITICAL, UNKNOWN
     * @param ambariDispatchRecipients List of recipients
     * @param mailSmtpHost SMTP Host
     * @param mailSmtpPort SMTP Port
     * @param mailSmtpFrom SMTP From
     * @param mailSmtpAuth Is authentication required
     */
    void addAlertNotification(String name, String description, Boolean global, String notificationType,
                              List<String> alertStates, List<String> ambariDispatchRecipients, String mailSmtpHost,
                              Integer mailSmtpPort, String mailSmtpFrom, Boolean mailSmtpAuth);

    /**
     * Add alert notification
     * @param name Notification name
     * @param description Notification description
     * @param global Is notification global for all groups
     * @param notificationType EMAIL or SNMP
     * @param alertStates Alert status changes for which a notification will be send - OK, WARNING, CRITICAL, UNKNOWN
     * @param ambariDispatchRecipients List of recipients
     * @param mailSmtpHost SMTP Host
     * @param mailSmtpPort SMTP Port
     * @param mailSmtpFrom SMTP From
     * @param mailSmtpAuth Is authentication required
     */
    void editAlertNotification(String name, String description, Boolean global, String notificationType,
                               List<String> alertStates, List<String> ambariDispatchRecipients,
                               String mailSmtpHost, Integer mailSmtpPort, String mailSmtpFrom, Boolean mailSmtpAuth);

    /**
     * Add alert notification
     * @param name Notification name
     */
    public void deleteAlertNotification(String name);

    /**
     * Add alert notification
     * @param name Group name
     * @param definitions List of notification definitions
     */
    void addAlertGroup(String name, List<Integer> definitions);

    /**
     * @return true once all ambari services are installed and running
     */
    boolean isClusterComplete();

    /**
     * Create a task that runs subtasks on each extra service
     * @param taskName Name of the task
     * @param fn to run on each extra service
     * @return the parent task
     */
    public Task<List<?>> createExtraServicesParallelTask(String taskName, final Function<ExtraService, ?> fn);
}
