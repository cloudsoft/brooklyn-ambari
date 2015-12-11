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

import javax.annotation.Nullable;

import org.apache.brooklyn.api.entity.Entity;
import org.apache.brooklyn.api.entity.EntitySpec;
import org.apache.brooklyn.core.entity.Entities;
import org.apache.brooklyn.entity.group.DynamicClusterImpl;
import org.apache.brooklyn.entity.software.base.SameServerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import io.brooklyn.ambari.AmbariCluster;
import io.brooklyn.ambari.EtcHostsManager;
import io.brooklyn.ambari.agent.AmbariAgent;
import io.brooklyn.ambari.agent.AmbariAgentImpl;
import io.brooklyn.ambari.server.AmbariServer;

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
            final List<AmbariAgent> ambariAgents = ImmutableList.copyOf(Iterables.filter(entities, AmbariAgent.class));
           // move test to cluster?
            if(getAmbariCluster().getMasterAmbariServer().getAttribute(AmbariServer.CLUSTER_STATE).equals("COMPLETED")){
               // Can I remove task?
//                DynamicTasks.queue(Tasks.builder()
//                        .displayName("add nodes to Ambari service")
//                        .body(new Runnable() {
//                            @Override
//                            public void run() {
                                getAmbariCluster().addHostsToHostGroup(getDisplayName(), ambariAgents);
//                            }
//                        })
//                        .build());
            }
        }

        return entities;
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
}
