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

package io.brooklyn.ambari.hostgroup;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;

import org.apache.brooklyn.api.entity.Entity;
import org.apache.brooklyn.api.entity.EntitySpec;
import org.apache.brooklyn.api.mgmt.Task;
import org.apache.brooklyn.core.entity.Entities;
import org.apache.brooklyn.entity.group.DynamicClusterImpl;
import org.apache.brooklyn.entity.software.base.SameServerEntity;
import org.apache.brooklyn.util.exceptions.Exceptions;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import io.brooklyn.ambari.AmbariCluster;
import io.brooklyn.ambari.EtcHostsManager;
import io.brooklyn.ambari.agent.AmbariAgent;
import io.brooklyn.ambari.agent.AmbariAgentImpl;
import io.brooklyn.ambari.service.AbstractExtraServicesTask;
import io.brooklyn.ambari.service.ExtraService;
import io.brooklyn.ambari.service.ExtraServiceException;

public class AmbariHostGroupImpl extends DynamicClusterImpl implements AmbariHostGroup {
    public static final Logger LOG = LoggerFactory.getLogger(AmbariHostGroup.class);

    @Override
    public void init() {
        super.init();
        EntitySpec<?> siblingSpec = getConfig(AmbariHostGroup.SIBLING_SPEC);
        if (siblingSpec != null) {
            config().set(MEMBER_SPEC, agentWithSiblingsSpec(siblingSpec));
        } else {
            config().set(MEMBER_SPEC, ambariAgentSpec());
        }
    }

    @Override
    public List<String> getHostFQDNs() {
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        for (AmbariAgent agent : Entities.descendants(this, AmbariAgent.class)) {
            String fqdn = agent.getFqdn();
            if (fqdn != null) {
                builder.add(fqdn);
            }
        }
        return builder.build();
    }

    @Nullable
    @Override
    public List<String> getComponents() {
        return getConfig(HADOOP_COMPONENTS);
    }

    @Override
    public Collection<Entity> resizeByDelta(int delta) {
        Collection<Entity> entities = super.resizeByDelta(delta);

        if (delta > 0) {
            EtcHostsManager.setHostsOnMachines(getAmbariCluster().getAmbariNodes(), getConfig(AmbariCluster.ETC_HOST_ADDRESS));
            if (getAmbariCluster().isClusterComplete()) {
                final List<AmbariAgent> ambariAgents = getAmbariAgents(entities);
                try {
                    runExtraServicesTasks("Pre-scale tasks", new PreScaleTask(ambariAgents));
                    getAmbariCluster().addHostsToHostGroup(getDisplayName(), ambariAgents);
                    runExtraServicesTasks("Post-scale tasks", new PostScaleTask(ambariAgents));
                } catch (ExtraServiceException ex) {
                    Exceptions.propagate(ex);
                }
            }
        }

        return entities;
    }

    private void runExtraServicesTasks(String taskName, AbstractExtraServicesTask fn) {
        Task<List<?>> extraServicesParallelTask = getAmbariCluster().createExtraServicesParallelTask(taskName, fn);
        try {
            Entities.submit(this, extraServicesParallelTask).get();
        } catch (ExecutionException | InterruptedException ex) {
            Throwable rootCause = ExceptionUtils.getRootCause(ex);
            if (rootCause != null && rootCause instanceof ExtraServiceException) {
                throw (ExtraServiceException) rootCause;
            } else {
                throw new ExtraServiceException(ex.getMessage());
            }
        }
    }

    private List<AmbariAgent> getAmbariAgents(Collection<Entity> entities) {
        ImmutableList.Builder<AmbariAgent> builder = ImmutableList.<AmbariAgent>builder();
        for (Entity entity : entities) {
            if (entity instanceof AmbariAgent) {
                builder.add((AmbariAgent) entity);
            } else {
                builder.addAll(Entities.descendants(entity, AmbariAgent.class));
            }
        }

        return builder.build();
    }

    private EntitySpec<? extends AmbariAgent> ambariAgentSpec() {
        return AmbariAgentImpl.createAgentSpec((AmbariCluster) getParent(), config().getLocalBag());
    }

    private EntitySpec agentWithSiblingsSpec(EntitySpec<?> siblingSpec) {
        return EntitySpec.create(SameServerEntity.class)
                .child(ambariAgentSpec())
                .child(siblingSpec);
    }

    private AmbariCluster getAmbariCluster() {
        return Iterables.getFirst(Iterables.filter(Entities.ancestors(this), AmbariCluster.class), null);
    }

    private class PostScaleTask extends AbstractExtraServicesTask<ExtraService> {
        private final List<AmbariAgent> ambariAgents;

        public PostScaleTask(List<AmbariAgent> ambariAgents) {
            this.ambariAgents = ambariAgents;
        }

        @Override
        public Task<Integer> sshTaskApply(ExtraService node) {
            node.postHostGroupScale(getAmbariCluster(), ambariAgents);
            return null;
        }
    }

    private class PreScaleTask extends AbstractExtraServicesTask<ExtraService> {
        private final List<AmbariAgent> ambariAgents;

        public PreScaleTask(List<AmbariAgent> ambariAgents) {
            this.ambariAgents = ambariAgents;
        }

        @Override
        public Task<Integer> sshTaskApply(ExtraService node) {
            node.preHostGroupScale(getAmbariCluster(), ambariAgents);
            return null;
        }
    }
}
