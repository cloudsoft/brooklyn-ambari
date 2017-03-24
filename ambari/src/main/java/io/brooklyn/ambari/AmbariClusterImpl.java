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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.transform;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Predicate;
import org.apache.brooklyn.api.effector.Effector;
import org.apache.brooklyn.api.entity.EntityLocal;
import org.apache.brooklyn.api.entity.EntitySpec;
import org.apache.brooklyn.api.location.Location;
import org.apache.brooklyn.api.mgmt.Task;
import org.apache.brooklyn.config.ConfigKey;
import org.apache.brooklyn.core.entity.Attributes;
import org.apache.brooklyn.core.entity.Entities;
import org.apache.brooklyn.core.entity.lifecycle.ServiceStateLogic;
import org.apache.brooklyn.core.mgmt.BrooklynTaskTags;
import org.apache.brooklyn.core.mgmt.internal.EffectorUtils;
import org.apache.brooklyn.enricher.stock.Enrichers;
import org.apache.brooklyn.entity.software.base.SoftwareProcess;
import org.apache.brooklyn.entity.stock.BasicStartableImpl;
import org.apache.brooklyn.util.collections.MutableList;
import org.apache.brooklyn.util.collections.MutableMap;
import org.apache.brooklyn.util.core.flags.TypeCoercions;
import org.apache.brooklyn.util.core.task.Tasks;
import org.apache.brooklyn.util.guava.Maybe;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import io.brooklyn.ambari.agent.AmbariAgent;
import io.brooklyn.ambari.agent.AmbariAgentImpl;
import io.brooklyn.ambari.cluster.ClusterStateEventListener;
import io.brooklyn.ambari.cluster.RegisteredHostEventListener;
import io.brooklyn.ambari.hostgroup.AmbariHostGroup;
import io.brooklyn.ambari.rest.AmbariApiException;
import io.brooklyn.ambari.rest.domain.Bindings;
import io.brooklyn.ambari.rest.domain.Blueprint;
import io.brooklyn.ambari.rest.domain.HostComponent;
import io.brooklyn.ambari.rest.domain.HostGroup;
import io.brooklyn.ambari.rest.domain.Recommendation;
import io.brooklyn.ambari.rest.domain.RecommendationWrapper;
import io.brooklyn.ambari.rest.domain.RecommendationWrappers;
import io.brooklyn.ambari.rest.domain.Request;
import io.brooklyn.ambari.rest.domain.Stack;
import io.brooklyn.ambari.server.AmbariServer;
import io.brooklyn.ambari.service.ExtraService;
import io.brooklyn.ambari.service.ExtraServiceException;

/**
 * The minimum requirements for an ambari hadoop cluster.
 * These can be set in the provisioning properties of yaml or by
 * using machines of this spec in a byon cluster.
 * <p/>
 * "minRam", 8192,
 * "osFamily", "ubuntu",
 * "osVersionRegex", "12.*",
 */
public class AmbariClusterImpl extends BasicStartableImpl implements AmbariCluster {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(BasicStartableImpl.class);

    public static final ImmutableList<String> DEFAULT_SERVICES = ImmutableList.<String>of("ZOOKEEPER");
    public static final ImmutableMap<String, Map> DEFAULT_CONFIG_MAP = ImmutableMap.<String, Map>of();

    //TODO there an issue with rebind here?  On rebind should be populated from somewhere else?
    private boolean isHostGroupsDeployment;
    private List<String> services;
    private Map<String, List<String>> componentsByNode;

    private Function<AmbariNode, String> mapAmbariNodeToFQDN = new Function<AmbariNode, String>() {
        @Nullable
        @Override
        public String apply(@Nullable AmbariNode ambariNode) {
            return ambariNode.getFqdn();
        }
    };

    @Override
    public void init() {
        super.init();

        isHostGroupsDeployment = Iterables.size(getHostGroups()) > 0;

        addChild(createServerSpec(getConfig(SECURITY_GROUP)));
        if (!getConfig(SERVER_COMPONENTS).isEmpty()) {
            for (AmbariServer ambariServer : getAmbariServers()) {
                ambariServer.config().set(SoftwareProcess.CHILDREN_STARTABLE_MODE, SoftwareProcess.ChildStartableMode.BACKGROUND_LATE);
                EntitySpec<? extends AmbariAgent> agentSpec = AmbariAgentImpl.createAgentSpec(this, null);
                ambariServer.addChild(agentSpec);
            }
        }

        services = MutableList.copyOf(getConfig(HADOOP_SERVICES));

        calculateTotalAgents();
        if (!isHostGroupsDeployment) {
            createClusterTopology();
            if (services.size() == 0) {
                services.addAll(DEFAULT_SERVICES);
            }
        }


        addEnricher(Enrichers.builder()
                .propagating(Attributes.MAIN_URI)
                .from(getMasterAmbariServer())
                .build());

        componentsByNode = new MutableMap<String, List<String>>();
        addDeprecatedExtraServiceToExtraServices();
        for (EntitySpec<? extends ExtraService> entitySpec : getConfig(EXTRA_HADOOP_SERVICES)) {
            LOG.warn(EXTRA_HADOOP_SERVICES.getName() + " configuration key is deprecated. Extra services should now be defined through as children by using 'brooklyn.children'");
            addChild(entitySpec);
        }

        final Iterable<String> ambariHostGroupNames = transform(getHostGroups(), new Function<AmbariHostGroup, String>() {
            @Nullable
            @Override
            public String apply(@Nullable AmbariHostGroup ambariHostGroup) {
                return ambariHostGroup != null ? ambariHostGroup.getDisplayName() : null;
            }
        });

        for (ExtraService extraService : Entities.descendants(this, ExtraService.class)) {
            if (extraService.getConfig(ExtraService.SERVICE_NAME) == null && extraService.getConfig(ExtraService.COMPONENT_NAMES) == null) {
                continue;
            }

            if (isHostGroupsDeployment) {
                checkNotNull(extraService.getConfig(ExtraService.COMPONENT_NAMES),
                        "Entity \"%s\" must define a list of components names as this is a host groups based deployment. Please use the \"%s\" configuration key",
                        extraService.getEntityType().getName(),
                        ExtraService.COMPONENT_NAMES.getName());

                for (ExtraService.ComponentMapping componentMapping : extraService.getComponentMappings()) {
                    if (!componentMapping.getHost().equals(getConfig(SERVER_HOST_GROUP)) && !Iterables.contains(ambariHostGroupNames, componentMapping.getHost())) {
                        throw new IllegalStateException(String.format("Extra component \"%s\" of entity \"%s\" cannot be bound to \"%s\" host group because it does not exist. Please choose from %s or " + getConfig(SERVER_HOST_GROUP),
                                componentMapping.getComponent(), extraService.getEntityType().getName(), componentMapping.getHost(), ambariHostGroupNames));
                    }
                    if (!componentsByNode.containsKey(componentMapping.getHost())) {
                        componentsByNode.put(componentMapping.getHost(), MutableList.<String>of());
                    }
                    componentsByNode.get(componentMapping.getHost()).add(componentMapping.getComponent());
                }
            } else {
                checkNotNull(extraService.getConfig(ExtraService.SERVICE_NAME),
                        "Entity \"%s\" must define a service name as this is a services based deployment. Please use the \"%s\" configuration key",
                        extraService.getEntityType().getName(),
                        ExtraService.SERVICE_NAME.getName());

                if (StringUtils.isNotBlank(extraService.getConfig(ExtraService.SERVICE_NAME))) {
                    services.add(extraService.getConfig(ExtraService.SERVICE_NAME));
                }
            }
        }

    }

    private void addDeprecatedExtraServiceToExtraServices() {
        EntitySpec<? extends ExtraService> entitySpec = getConfig(EXTRA_HADOOP_SERVICE);
        if (entitySpec != null) {
            LOG.warn(EXTRA_HADOOP_SERVICE.getName() + " configuration key is deprecated. Extra services should now be defined through as children by using 'brooklyn.children'");
            MutableList<EntitySpec<? extends ExtraService>> specs = MutableList.copyOf(getConfig(EXTRA_HADOOP_SERVICES));
            specs.add(entitySpec);
            config().set(EXTRA_HADOOP_SERVICES, specs);
        }
    }

    @Override
    public void start(Collection<? extends Location> locations) {
        super.start(locations);
        subscribe(getMasterAmbariServer(), AmbariServer.REGISTERED_HOSTS, new RegisteredHostEventListener(this, config().get(AmbariCluster.PAUSE_FOR_DEPLOYMENT)));
        subscribe(getMasterAmbariServer(), AmbariServer.CLUSTER_STATE, new ClusterStateEventListener(this));

        EtcHostsManager.setHostsOnMachines(getAmbariNodes(), getConfig(ETC_HOST_ADDRESS));
    }

    @Override
    public Iterable<AmbariNode> getAmbariNodes() {
        return Entities.descendants(this, AmbariNode.class);
    }

    @Override
    public Iterable<AmbariAgent> getAmbariAgents() {
        return Entities.descendants(this, AmbariAgent.class);
    }

    @Override
    public Iterable<AmbariServer> getAmbariServers() {
        return Entities.descendants(this, AmbariServer.class);
    }

    @Override
    public AmbariServer getMasterAmbariServer() {
        return Iterables.getFirst(Entities.descendants(this, AmbariServer.class), null);
    }

    @Override
    public List<String> getExtraStackDefinitionsUrls() {
        return getConfig(STACK_DEFINITION_URLS);
    }

    @Override
    public void addHostsToHostGroup(String hostgroupName, List<AmbariAgent> hosts) {
        final Maybe<Effector<?>> effector = EffectorUtils.findEffector(getMasterAmbariServer().getEntityType().getEffectors(), "addHostsToHostGroup");
        if (effector.isAbsentOrNull()) {
            throw new IllegalStateException("Cannot get the addHostsToHostGroup effector");
        }
        getMasterAmbariServer().invoke(effector.get(), ImmutableMap.of(
                "Blueprint Name", getConfig(AmbariCluster.BLUEPRINT_NAME),
                "Hostgroup Name", hostgroupName,
                "Hosts", Lists.transform(hosts, mapAmbariNodeToFQDN),
                "Cluster Name", getConfig(AmbariCluster.CLUSTER_NAME)
        ));
    }

    @Override
    public void addAlertNotification(String name, String description, Boolean global, String notificationType,
                                     List<String> alertStates, List<String> ambariDispatchRecipients,
                                     String mailSmtpHost, Integer mailSmtpPort, String mailSmtpFrom, Boolean mailSmtpAuth) {

        final Maybe<Effector<?>> effector = EffectorUtils.findEffector(getMasterAmbariServer().getEntityType().getEffectors(), "addAlertNotification");
        if (effector.isAbsentOrNull()) {
            throw new IllegalStateException("Cannot get the addAlertNotification effector");
        }

        getMasterAmbariServer().invoke(effector.get(), ImmutableMap.<String, Object>builder()
                .put("Notification Name", name)
                .put("Description", description)
                .put("Global", global)
                .put("Notification Type", notificationType)
                .put("Alert States", alertStates)
                .put("Ambari Dispatch Recipients", ambariDispatchRecipients)
                .put("SMTP Host", mailSmtpHost)
                .put("SMTP Port", mailSmtpPort)
                .put("SMTP From", mailSmtpFrom)
                .put("SMTP Auth", mailSmtpAuth)
                .build());

    }

    @Override
    public void editAlertNotification(String name, String description, Boolean global, String notificationType,
                                      List<String> alertStates, List<String> ambariDispatchRecipients,
                                      String mailSmtpHost, Integer mailSmtpPort, String mailSmtpFrom, Boolean mailSmtpAuth) {

        final Maybe<Effector<?>> effector = EffectorUtils.findEffector(getMasterAmbariServer().getEntityType().getEffectors(), "editAlertNotification");
        if (effector.isAbsentOrNull()) {
            throw new IllegalStateException("Cannot get the editAlertNotification effector");
        }

        getMasterAmbariServer().invoke(effector.get(), ImmutableMap.<String, Object>builder()
                .put("Notification Name", name)
                .put("Description", description)
                .put("Global", global)
                .put("Notification Type", notificationType)
                .put("Alert States", alertStates)
                .put("Ambari Dispatch Recipients", ambariDispatchRecipients)
                .put("SMTP Host", mailSmtpHost)
                .put("SMTP Port", mailSmtpPort)
                .put("SMTP From", mailSmtpFrom)
                .put("SMTP Auth", mailSmtpAuth)
                .build());
    }

    public void deleteAlertNotification(String name) {
        final Maybe<Effector<?>> effector = EffectorUtils.findEffector(getMasterAmbariServer().getEntityType().getEffectors(), "editAlertNotification");
        if (effector.isAbsentOrNull()) {
            throw new IllegalStateException("Cannot get the editAlertNotification effector");
        }

        getMasterAmbariServer().invoke(effector.get(), ImmutableMap.<String, Object>builder()
                .put("Notification Name", name)
                .build());
    }


    @Override
    public void addAlertGroup(String name,List<Integer> definitions) {

        final Maybe<Effector<?>> effector = EffectorUtils.findEffector(getMasterAmbariServer().getEntityType().getEffectors(), "addAlertGroup");
        if (effector.isAbsentOrNull()) {
            throw new IllegalStateException("Cannot get the addAlertGroup effector");
        }

        getMasterAmbariServer().invoke(effector.get(), ImmutableMap.of(
                "name", name,
                "definitions", definitions
                ));
    }

    @Override
    public void deployCluster() throws AmbariApiException, ExtraServiceException {
        // Set the flag to true so the deployment won't happen multiple times
        setAttribute(CLUSTER_SERVICES_INITIALISE_CALLED, true);

        // Wait for the Ambari server to be up
        getMasterAmbariServer().waitForServiceUp();

        final Map<String, List<String>> componentsByNodeName = new MutableMap<String, List<String>>();

        RecommendationWrapper recommendationWrapper = null;

        if (isHostGroupsDeployment) {
            LOG.info("{} getting the recommendation from AmbariHostGroup configuration", this);
            recommendationWrapper = getRecommendationWrapperFromAmbariHostGroups();
        } else {
            LOG.info("{} getting the recommendation from Ambari for the services: {}", this, services);
            recommendationWrapper = getRecommendationWrapperFromAmbariServer();
        }

        checkNotNull(recommendationWrapper);
        checkNotNull(recommendationWrapper.getRecommendation());
        checkNotNull(recommendationWrapper.getRecommendation().getBlueprint());
        checkNotNull(recommendationWrapper.getRecommendation().getBindings());

        for (HostGroup hostGroup : recommendationWrapper.getRecommendation().getBlueprint().getHostGroups()) {
            if (!componentsByNodeName.containsKey(hostGroup.getName())) {
                componentsByNodeName.put(hostGroup.getName(), new MutableList<String>());
            }
            final List<HostComponent> hostComponents = MutableList.copyOf(hostGroup.getComponents());
            for (HostComponent component : hostComponents) {
                // ZKFC (ZooKeeper Failover Controller) is disabled if HA setup is not present
                // ZKFC requires at least 2 NameNodes in a HA setup to operate and/or install correctly
                if (StringUtils.equals(component.getName(), "ZKFC") && !isHaEnabled(getConfig(AMBARI_CONFIGURATIONS))) {
                    hostGroup.getComponents().remove(component);
                    continue;
                }
                componentsByNodeName.get(hostGroup.getName()).add(component.getName());
            }
        }

        for (HostGroup hostGroup : recommendationWrapper.getRecommendation().getBindings().getHostGroups()) {
            AmbariAgent ambariAgent = null;

            for (int i = 0; i < hostGroup.getHosts().size(); i++) {
                final Map<String, String> host = hostGroup.getHosts().get(i);
                final String fqdn = host.get("fqdn");
                if (StringUtils.isNotBlank(fqdn)) {
                    final List<String> components = componentsByNodeName.get(hostGroup.getName());
                    ambariAgent = getAmbariAgentByFqdn(fqdn);
                    if (ambariAgent != null && components != null) {
                        ambariAgent.setComponents(components);
                    }
                }
            }
        }

        Map<String, Map> configuration = MutableMap.copyOf(getConfig(AMBARI_CONFIGURATIONS));

        if (configuration.size() == 0) {
            configuration.putAll(DEFAULT_CONFIG_MAP);
        }

        for (ExtraService extraService : getExtraServices()) {
            configuration = mergeMaps(configuration, extraService.getAmbariConfig(this));
        }

        LOG.info("{} calling pre-cluster-deploy on all Ambari nodes", this);
        try {
            Task<List<?>> preClusterDeployTasks = createExtraServicesParallelTask("preClusterDeploy", new PreClusterDeployFunction());
            Entities.submit(this, preClusterDeployTasks).get();
        } catch (ExecutionException | InterruptedException ex) {
            // If something failed within an extra service, we propagate the exception for the cluster to handle it properly.
            Throwable rootCause = ExceptionUtils.getRootCause(ex);
            if (rootCause != null && rootCause instanceof ExtraServiceException) {
                throw (ExtraServiceException) rootCause;
            } else {
                throw new ExtraServiceException(ex.getMessage());
            }
        }

        LOG.info("{} calling cluster-deploy", this);
        try {
            Request request = getMasterAmbariServer().deployCluster(getConfig(AmbariCluster.CLUSTER_NAME), getConfig(AmbariCluster.BLUEPRINT_NAME), recommendationWrapper, configuration);
        } catch (AmbariApiException ex) {
            // If the cluster failed to deploy, we first put the server "ON FIRE" and throw again the exception for the
            // cluster to handle it properly.
            ServiceStateLogic.ServiceNotUpLogic.updateNotUpIndicator((EntityLocal) getMasterAmbariServer(), "ambari.api", ex.getMessage());
            throw ex;
        }
    }

    @Override
    public void postDeployCluster() throws ExtraServiceException {
        // Set the flag to true so the post deployment won't happen multiple times
        setAttribute(CLUSTER_SERVICES_INSTALLED, true);

        LOG.info("{} calling post-cluster-deploy on all Ambari nodes", this);
        try {
            Task<List<?>> postDeployClusterTasks = createExtraServicesParallelTask("postClusterDeploy", new PostClusterDeployFunction());
            Entities.submit(this, postDeployClusterTasks).get();
        } catch (ExecutionException | InterruptedException ex) {
            // If something failed within an extra service, we propagate the exception for the cluster to handle it properly.
            Throwable rootCause = ExceptionUtils.getRootCause(ex);
            if (rootCause != null && rootCause instanceof ExtraServiceException) {
                throw (ExtraServiceException) rootCause;
            } else {
                throw new ExtraServiceException(ex.getMessage());
            }
        }
    }

    private EntitySpec<? extends AmbariServer> createServerSpec(Object securityGroup) {
        Iterable<Map.Entry<ConfigKey<?>, Object>> ambariConfigFromPropertiesIterator = Iterables.filter(getManagementContext().getConfig().getAllConfig().entrySet(), new Predicate<Object>() {
            @Override
            public boolean apply(@Nullable Object input) {
                return ((ConfigKey<?>) ((Map.Entry) input).getKey()).getName().contains(AMBARI_ALERTS_CONFIG_PREFIX);
            }
        });
        Map<String, Object> ambariConfigFromProperties = MutableMap.of();
        for (Map.Entry entry: ambariConfigFromPropertiesIterator) {
            ambariConfigFromProperties.put(((ConfigKey<?>) entry.getKey()).getName(), entry.getValue());
        }
        EntitySpec<? extends AmbariServer> serverSpec = EntitySpec.create(getConfig(SERVER_SPEC))
                .configure(SoftwareProcess.SUGGESTED_VERSION, getConfig(AmbariCluster.SUGGESTED_VERSION))
                .configure(ambariConfigFromProperties)
                .configure(config().getBag().getAllConfig())
                .displayName("Ambari Server");
        if (securityGroup != null) {
            serverSpec.configure(SoftwareProcess.PROVISIONING_PROPERTIES.subKey("securityGroups"), securityGroup);
        }
        return serverSpec;
    }

    private RecommendationWrapper getRecommendationWrapperFromAmbariHostGroups() {
        final Blueprint.Builder blueprintBuilder = new Blueprint.Builder();
        final Bindings.Builder bindingsBuilder = new Bindings.Builder();

        for (AmbariHostGroup ambariHostGroup : getHostGroups()) {
            HostGroup.Builder hostGroupBuilder = new HostGroup.Builder()
                    .setName(ambariHostGroup.getDisplayName())
                    .addComponents(ambariHostGroup.getComponents());
            if (componentsByNode.containsKey(ambariHostGroup.getDisplayName())) {
                hostGroupBuilder.addComponents(componentsByNode.get(ambariHostGroup.getDisplayName()));
            }
            blueprintBuilder.addHostGroup(hostGroupBuilder.build());

            bindingsBuilder.addHostGroup(new HostGroup.Builder()
                    .setName(ambariHostGroup.getDisplayName())
                    .addHosts(ambariHostGroup.getHostFQDNs())
                    .build());
        }

        List<String> serverComponentsList = getConfig(AmbariCluster.SERVER_COMPONENTS);
        if (!serverComponentsList.isEmpty()) {
            HostGroup.Builder hostGroupBuilder = new HostGroup.Builder()
                    .setName(getConfig(SERVER_HOST_GROUP))
                    .addComponents(serverComponentsList);
            if (componentsByNode.containsKey(getConfig(SERVER_HOST_GROUP))) {
                hostGroupBuilder.addComponents(componentsByNode.get(getConfig(SERVER_HOST_GROUP)));
            }
            blueprintBuilder.addHostGroup(hostGroupBuilder.build());
            Iterable<AmbariServer> ambariServers = getAmbariServers();
            Iterable<String> fqdns = transform(ambariServers, mapAmbariNodeToFQDN);

            bindingsBuilder.addHostGroup(new HostGroup.Builder()
                    .setName(getConfig(SERVER_HOST_GROUP))
                    .addHosts(Lists.newArrayList(fqdns))
                    .build());
        }

        return new RecommendationWrapper.Builder()
                .setStack(new Stack.Builder()
                        .setName(getConfig(HADOOP_STACK_NAME))
                        .setVersion(getConfig(HADOOP_STACK_VERSION))
                        .build())
                .setRecommendation(new Recommendation.Builder()
                        .setBlueprint(blueprintBuilder.build())
                        .setBindings(bindingsBuilder.build())
                        .build())
                .build();
    }

    protected Map<String, Map> mergeMaps(Map<String, Map> configuration, Map<String, Map> servicesConfig) {
        if (servicesConfig == null) {
            return configuration;
        }

        MutableMap<String, Map> newConfigurationMap = MutableMap.copyOf(configuration);
        for (Map.Entry<String, Map> stringMapEntry : servicesConfig.entrySet()) {
            if(!newConfigurationMap.containsKey(stringMapEntry.getKey())) {
                newConfigurationMap.put(stringMapEntry.getKey(), stringMapEntry.getValue());
            } else {
                if(stringMapEntry.getValue() != null) {
                    // Ensuring that the configuration passed via yaml gets precedence over the one provided by the external service
                    MutableMap<String, Map> tmpMap = MutableMap.copyOf(stringMapEntry.getValue());
                    tmpMap.putAll(newConfigurationMap.get(stringMapEntry.getKey()));

                    newConfigurationMap.put(stringMapEntry.getKey(), ImmutableMap.copyOf(tmpMap));
                }
            }
        }
        return newConfigurationMap;
    }

    private RecommendationWrapper getRecommendationWrapperFromAmbariServer() {
        final List<String> hosts = getMasterAmbariServer().getAttribute(AmbariServer.REGISTERED_HOSTS);
        final RecommendationWrappers recommendationWrappers = getMasterAmbariServer()
                .getRecommendations(getConfig(HADOOP_STACK_NAME), getConfig(HADOOP_STACK_VERSION), hosts, services);

        return recommendationWrappers.getRecommendationWrappers().size() > 0
                ? recommendationWrappers.getRecommendationWrappers().get(0)
                : null;
    }

    public Task<List<?>> createExtraServicesParallelTask(String taskName, final Function<ExtraService, ?> fn) {
        List<Task<?>> tasks = Lists.newArrayList();
        for (final ExtraService extraService : getExtraServices()) {
            Task<?> t = Tasks.builder()
                    .name(extraService.getEntityType().getSimpleName())
                    .description("pre-cluster-deploy tasks for " + extraService.getEntityType().getName() + " extra service")
                    .body(new FunctionRunningCallable<ExtraService>(extraService, fn))
                    .tag(BrooklynTaskTags.NON_TRANSIENT_TASK_TAG)
                    .build();
            tasks.add(t);
        }
        return Tasks.parallel(taskName, tasks);
    }

    @Nonnull
    private AmbariCluster getAmbariCluster() {
        return this;
    }

    @Nonnull
    public Iterable<ExtraService> getExtraServices() {
        return Entities.descendants(this, ExtraService.class);
    }

    @Nullable
    private AmbariAgent getAmbariAgentByFqdn(@Nonnull String fqdn) {
        checkNotNull(fqdn);

        for (AmbariAgent ambariAgent : Entities.descendants(this, AmbariAgent.class)) {
            if (StringUtils.equals(ambariAgent.getFqdn(), fqdn)) {
                return ambariAgent;
            }
        }
        return null;
    }

    private void calculateTotalAgents() {
        int agentsToExpect = 0;

        if (isHostGroupsDeployment) {
            for (AmbariHostGroup hostGroup : getHostGroups()) {
                agentsToExpect += hostGroup.getConfig(AmbariHostGroup.INITIAL_SIZE);
            }
        } else {
            agentsToExpect += getRequiredConfig(INITIAL_SIZE);
        }

        if (getMasterAmbariServer().agentOnServer()) {
            agentsToExpect += Iterables.size(getAmbariServers());
        }

        setAttribute(EXPECTED_AGENTS, agentsToExpect);
    }

    private Iterable<AmbariHostGroup> getHostGroups() {
        return Entities.descendants(this, AmbariHostGroup.class);
    }

    private void createClusterTopology() {
        int totalHostGroup = getAttribute(EXPECTED_AGENTS);
        // getAttribute(EXPECTED_AGENTS) = number of agents defined + agent on server. As createClusterTopology()
        // is called only for services based deployment, we need to remove the agent installed on the server from the
        // total count.
        if (getMasterAmbariServer().agentOnServer()) {
            totalHostGroup--;
        }

        for (int i = 0; i < totalHostGroup; i++) {
            addChild(EntitySpec.create(AmbariHostGroup.class)
                    .configure(AmbariHostGroup.INITIAL_SIZE, 1)
                    .displayName(String.format("host-group-%d", (i + 1))));
        }
    }

    private <T> T getRequiredConfig(ConfigKey<T> key) {
        return checkNotNull(getConfig(key), "config %s", key);
    }

    @Override
    public boolean isClusterComplete() {
        return "COMPLETED".equals(getMasterAmbariServer().getAttribute(AmbariServer.CLUSTER_STATE));
    }

    private class PreClusterDeployFunction implements Function<ExtraService, Void> {
        @Override
        public Void apply(ExtraService extraService) {
            extraService.preClusterDeploy(getAmbariCluster());
            return null;
        }
    }

    private class PostClusterDeployFunction implements Function<ExtraService, Void> {
        @Override
        public Void apply(ExtraService extraService) {
            extraService.postClusterDeploy(getAmbariCluster());
            return null;
        }
    }

    private boolean isHaEnabled(Map<String, Map> configuration) {
        String HDFS_SITE = "hdfs-site";
        String HA_ENABLE = "dfs.ha.automatic-failover.enabled";

        if (configuration.get(HDFS_SITE).get(HA_ENABLE) != null) {
            return Boolean.TRUE.equals(TypeCoercions.coerce(configuration.get(HDFS_SITE).get(HA_ENABLE), Boolean.class));
        }

        return false;
    }
}
