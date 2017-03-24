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

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.brooklyn.api.mgmt.Task;
import org.apache.brooklyn.core.mgmt.BrooklynTaskTags;
import org.apache.brooklyn.entity.stock.BasicStartableImpl;
import org.apache.brooklyn.util.collections.MutableList;
import org.apache.brooklyn.util.core.task.Tasks;
import org.apache.commons.collections.CollectionUtils;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import io.brooklyn.ambari.AmbariCluster;
import io.brooklyn.ambari.FunctionRunningCallable;
import io.brooklyn.ambari.agent.AmbariAgent;

/**
 * Abstract implementation of the an extra service that provides utility methods.
 */
public abstract class AbstractExtraService extends BasicStartableImpl implements ExtraService {

    private List<ComponentMapping> componentMappings;

    @Override
    @Deprecated
    public Map<String, Map> getAmbariConfig() {
        return null;
    }

    @Override
    public Map<String, Map> getAmbariConfig(AmbariCluster ambariCluster) {
        return getAmbariConfig();
    }

    @Override
    @Nonnull
    public List<ComponentMapping> getComponentMappings() {
        if (componentMappings == null) {
            componentMappings = MutableList.of();
            if (getConfig(COMPONENT_NAMES) != null) {
                for (String mapping : getConfig(COMPONENT_NAMES)) {
                    componentMappings.add(new ComponentMapping(mapping, getConfig(BIND_TO)));
                }
            }
        }

        return componentMappings;
    }

    /**
     * Utility method that will execute the given function on the given nodes. The executions will be done in a parallel.
     *
     * @param nodes the nodes to execute the function on.
     * @param fn the function to execute.
     * @param <T> the type of node.
     * @return a new pool of tasks.
     */
    // TODO: Pass the task's name and description as parameter
    protected <T> Task<List<?>> parallelListenerTask(final Iterable<T> nodes, final Function<T, ?> fn) {
        List<Task<?>> tasks = Lists.newArrayList();
        for (final T node : nodes) {
            Task<?> t = Tasks.builder()
                    .name(node.toString())
                    .description("Invocation on " + node.toString())
                    .body(new FunctionRunningCallable<T>(node, fn))
                    .tag(BrooklynTaskTags.NON_TRANSIENT_TASK_TAG)
                    .build();
            tasks.add(t);
        }
        return Tasks.parallel("Parallel invocation of " + fn + " on ambari nodes", tasks);
    }

    @Override
    public void preHostGroupScale(AmbariCluster ambariCluster, Iterable<AmbariAgent> newAgents) {
       // By default do nothing
    }

    @Override
    public void postHostGroupScale(AmbariCluster ambariCluster, Iterable<AmbariAgent> newAgents) {
       // By default do nothing
    }

    /**
     * Utility method that will execute the given function on the given nodes, only if they have one of the given components
     * installed on them. The executions will be done in a parallel.
     * @param nodes the nodes to execute the function on.
     * @param fn the function to execute.
     * @param components the list of components for which we want to function to be executed.
     * @return a new pool of tasks.
     */
    // TODO: Pass the task's name and description as parameter
    protected Task<List<?>> parallelListenerTask(final Iterable<AmbariAgent> nodes, final Function<AmbariAgent, ?> fn, List<String> components) {
        Preconditions.checkNotNull(components);

        List<Task<?>> tasks = Lists.newArrayList();
        for (final AmbariAgent ambariAgent : nodes) {
            Preconditions.checkNotNull(ambariAgent.getAttribute(AmbariAgent.COMPONENTS));
            if (!CollectionUtils.containsAny(ambariAgent.getAttribute(AmbariAgent.COMPONENTS), components)) {
                continue;
            }

            Task<?> t = Tasks.builder()
                    .name(ambariAgent.toString())
                    .description("Invocation on " + ambariAgent.toString())
                    .body(new FunctionRunningCallable<AmbariAgent>(ambariAgent, fn))
                    .tag(BrooklynTaskTags.NON_TRANSIENT_TASK_TAG)
                    .build();
            tasks.add(t);
        }
        return Tasks.parallel("Parallel invocation of " + fn + " on ambari agents", tasks);
    }
}
