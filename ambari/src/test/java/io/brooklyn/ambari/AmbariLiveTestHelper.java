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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.fail;

import org.apache.brooklyn.api.effector.Effector;
import org.apache.brooklyn.api.entity.Application;
import org.apache.brooklyn.api.mgmt.Task;
import org.apache.brooklyn.core.entity.Entities;
import org.apache.brooklyn.core.mgmt.internal.EffectorUtils;
import org.apache.brooklyn.entity.group.DynamicCluster;
import org.apache.brooklyn.test.EntityTestUtils;
import org.apache.brooklyn.util.core.task.Tasks;
import org.apache.brooklyn.util.guava.Maybe;
import org.apache.brooklyn.util.time.Duration;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

import io.brooklyn.ambari.agent.AmbariAgent;
import io.brooklyn.ambari.hostgroup.AmbariHostGroup;
import io.brooklyn.ambari.server.AmbariServer;

public class AmbariLiveTestHelper {
    protected void assertHadoopClusterEventuallyDeployed(Application app) {
        AmbariServer ambariServer = Entities.descendants(app, AmbariServer.class).iterator().next();
        EntityTestUtils.assertAttributeEventually(
                ImmutableMap.of("timeout", Duration.minutes(60)),
                ambariServer,
                AmbariServer.CLUSTER_STATE,
                Predicates.not(Predicates.or(Predicates.equalTo("ABORTED"), Predicates.equalTo("FAILED"), Predicates.equalTo("TIMEDOUT")))
        );
        EntityTestUtils.assertAttributeEventually(
                ImmutableMap.of("timeout", Duration.minutes(60)),
                ambariServer,
                AmbariServer.CLUSTER_STATE,
                Predicates.equalTo("COMPLETED"));
    }

    AmbariHostGroup getDataNodeHostGroup(Application app) {
        AmbariHostGroup ambariHostGroup = null;
        for (AmbariHostGroup hostGroup : Entities.descendants(app, AmbariHostGroup.class)) {
            if(hostGroup.getDisplayName().equals("DataNode")) {
                ambariHostGroup = hostGroup;
                break;
            }
        }

        if(ambariHostGroup==null) {
            fail();
        }
        return ambariHostGroup;
    }

    void assertFalseTaskIsError(Task<?> task) {
        assertFalse(task.isError(), "Effector should not fail");
        for (Task<?> childTask : Tasks.children(task)) {
            assertFalseTaskIsError(childTask);
        }
    }

    void assertResizeClusterWorks(Application app) {
        AmbariHostGroup ambariHostGroup = getDataNodeHostGroup(app);

        Task<?> task = ambariHostGroup.invoke(DynamicCluster.RESIZE_BY_DELTA, ImmutableMap.of("delta", 1));

        task.getUnchecked();
        assertFalseTaskIsError(task);
    }

    protected void assertAddServiceToClusterEffectorWorks(Application app) {
        final AmbariServer ambariServer = Entities.descendants(app, AmbariServer.class).iterator().next();
        final AmbariAgent ambariAgent = Entities.descendants(app, AmbariAgent.class).iterator().next();
        final Maybe<Effector<?>> effector = EffectorUtils.findEffector(ambariServer.getEntityType().getEffectors(), "addServiceToCluster");
        if (effector.isAbsentOrNull()) {
            throw new IllegalStateException("Cannot get the addServiceToCluster effector");
        }

        final Task<?> effectorTask = ambariServer.invoke(effector.get(), ImmutableMap.of(
                "cluster", "Cluster1",
                "service", "FLUME",
                "mappings", ImmutableMap.of("FLUME_HANDLER", ambariAgent.getFqdn()),
                "configuration", ImmutableMap.of(
                        "flume-env", ImmutableMap.of(
                                "flume_conf_dir", "/etc/flume/conf",
                                "flume_log_dir", "/var/log/flume",
                                "flume_run_dir", "/var/run/flume",
                                "flume_user", "flume"))
        ));

        effectorTask.getUnchecked();
        assertFalse(effectorTask.isError(), "Effector should not fail");
        assertEquals(2, Iterables.size(Tasks.children(effectorTask)));
        assertFalse(Tasks.failed(Tasks.children(effectorTask)).iterator().hasNext(), "All sub-task should not fail");
    }
}
