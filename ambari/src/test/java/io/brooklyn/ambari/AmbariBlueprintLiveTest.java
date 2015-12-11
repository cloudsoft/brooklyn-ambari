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

import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.brooklyn.api.effector.Effector;
import org.apache.brooklyn.api.entity.Application;
import org.apache.brooklyn.api.entity.Entity;
import org.apache.brooklyn.api.mgmt.ManagementContext;
import org.apache.brooklyn.api.mgmt.Task;
import org.apache.brooklyn.core.entity.Attributes;
import org.apache.brooklyn.core.entity.Entities;
import org.apache.brooklyn.core.entity.lifecycle.Lifecycle;
import org.apache.brooklyn.core.mgmt.internal.EffectorUtils;
import org.apache.brooklyn.core.mgmt.internal.LocalManagementContext;
import org.apache.brooklyn.core.mgmt.rebind.RebindOptions;
import org.apache.brooklyn.core.mgmt.rebind.RebindTestUtils;
import org.apache.brooklyn.entity.group.DynamicCluster;
import org.apache.brooklyn.entity.software.base.SoftwareProcess;
import org.apache.brooklyn.launcher.blueprints.AbstractBlueprintTest;
import org.apache.brooklyn.test.Asserts;
import org.apache.brooklyn.test.EntityTestUtils;
import org.apache.brooklyn.util.collections.MutableMap;
import org.apache.brooklyn.util.core.ResourceUtils;
import org.apache.brooklyn.util.core.task.Tasks;
import org.apache.brooklyn.util.guava.Maybe;
import org.apache.brooklyn.util.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

import io.brooklyn.ambari.agent.AmbariAgent;
import io.brooklyn.ambari.hostgroup.AmbariHostGroup;
import io.brooklyn.ambari.server.AmbariServer;

@Test(singleThreaded = false, threadPoolSize = 2)
public class AmbariBlueprintLiveTest extends AbstractBlueprintTest {

    private static final Logger LOG = LoggerFactory.getLogger(AmbariBlueprintLiveTest.class);

    // Move setup behaviour to class so that we can run tests against single brooklyn instance
    @BeforeClass(alwaysRun = true)
    public void setupClass() throws Exception {
        super.setUp();
    }

    @BeforeMethod
    @Override
    public void setUp() throws Exception {
        // Do nothing
    }

    @AfterClass(alwaysRun = true)
    public void tearDownClass() throws Exception {
        super.tearDown();
    }

    @AfterMethod
    @Override
    public void tearDown() throws Exception {
        //Do nothing
    }

    protected Application runTestsAndGetApp(Reader yaml) throws Exception {
        Application app = this.launcher.launchAppYaml(yaml);
        this.assertNoFires(app);
        Application newApp = this.rebind(app, RebindOptions.create());
        this.assertNoFires(newApp);
        this.assertHadoopClusterEventuallyDeployed(newApp);

        return newApp;
    }

    protected void assertNoFires(final Entity app) {
        EntityTestUtils.assertAttributeEqualsEventually(app, Attributes.SERVICE_UP, Boolean.valueOf(true));
        EntityTestUtils.assertAttributeEqualsEventually(app, Attributes.SERVICE_STATE_ACTUAL, Lifecycle.RUNNING);
        Asserts.succeedsEventually(ImmutableMap.of("timeout", Duration.FIVE_MINUTES), new Runnable() {
            public void run() {
                Iterator i$ = Entities.descendants(app).iterator();

                while (i$.hasNext()) {
                    Entity entity = (Entity) i$.next();
                    Assert.assertNotEquals(entity.getAttribute(Attributes.SERVICE_STATE_ACTUAL), Lifecycle.ON_FIRE);
                    Assert.assertNotEquals(entity.getAttribute(Attributes.SERVICE_UP), Boolean.valueOf(false));
                    if (entity instanceof SoftwareProcess) {
                        EntityTestUtils.assertAttributeEquals(entity, Attributes.SERVICE_STATE_ACTUAL, Lifecycle.RUNNING);
                        EntityTestUtils.assertAttributeEquals(entity, Attributes.SERVICE_UP, Boolean.TRUE);
                    }
                }

            }
        });
    }

    @DataProvider(name = "providerData", parallel = true)
    public Object[][] providerData() {

        return new Object[][]{
                new Object[]{
                        "ambari-cluster.yaml", "softlayer", "ams01", "UbuntuOnSoftlayer", MutableMap.<String, String>builder()
                        .put("minRam", "16384")
                        .put("minCores", "4")
                        .put("osFamily", "ubuntu")
                        .put("osVersionRegex", "12.*")
                        .put("stopIptables", "true")
                        .build()
                },
                new Object[]{
                        "ambari-cluster.yaml", "softlayer", "ams03", "CentOSOnSoftlayer", MutableMap.<String, String>builder()
                        .put("minRam", "16384")
                        .put("minCores", "4")
                        .put("osFamily", "centos")
                        .put("osVersionRegex", "6.*")
                        .put("stopIptables", "true")
                        .build()
                },
                new Object[]{
                        "ambari-cluster.yaml", "softlayer", "lon02", "RHEDOnSoftlayer", MutableMap.<String, String>builder()
                        .put("minRam", "16384")
                        .put("minCores", "4")
                        .put("osFamily", "rhel")
                        .put("osVersionRegex", "6.*")
                        .put("stopIptables", "true")
                        .build()
                },
                new Object[]{
                        "ambari-cluster-by-hostgroup.yaml", "softlayer", "sea01", "UbuntuOnSoftlayer", MutableMap.<String, String>builder()
                        .put("minRam", "16384")
                        .put("minCores", "4")
                        .put("osFamily", "ubuntu")
                        .put("osVersionRegex", "12.*")
                        .put("stopIptables", "true")
                        .build()
                },
                new Object[]{
                        "ambari-cluster-w-extra-services.yaml", "softlayer", "dal05", "CentOSOnSoftlayer", MutableMap.<String, String>builder()
                        .put("minRam", "16384")
                        .put("minCores", "4")
                        .put("osFamily", "centos")
                        .put("osVersionRegex", "6.*")
                        .put("stopIptables", "true")
                        .build()
                }
        };
    }

    @Test(groups = {"Live"}, dataProvider = "providerData")
    public void testYamlBlueprint(String yamlFile, String provider, String region, String description, Map<String, String> options) throws Exception {
        LOG.info("Testing {} on {}:{} using {} ({})", new Object[]{yamlFile, provider, region, description, options});

        String yaml = buildLocation(provider, region, options) + new ResourceUtils(this).getResourceAsString(yamlFile);

        runTestsAndGetApp(new StringReader(yaml));
    }

    @Test(groups = {"Live"})
    public void testAddServiceToCluster() throws Exception {
        Map<String, String> options = ImmutableMap.<String, String>builder()
                .put("minRam", "16384")
                .put("minCores", "4")
                .put("osFamily", "ubuntu")
                .put("osVersionRegex", "12.*")
                .put("stopIptables", "true")
                .build();

        String yaml = buildLocation("softlayer", "ams01", options) + new ResourceUtils(this).getResourceAsString("ambari-cluster-small.yaml");

        runTestsAndEffectors(new StringReader(yaml));
    }

    @Test(groups = {"Live"})
    public void testExpandCluster() throws Exception {
        Map<String, String> options = ImmutableMap.<String, String>builder()
                .put("minRam", "16384")
                .put("minCores", "4")
                .put("osFamily", "ubuntu")
                .put("osVersionRegex", "12.*")
                .put("stopIptables", "true")
                .build();

        String yaml =
                buildLocation("softlayer", "ams01", options) +
                        new ResourceUtils(this)
                                .getResourceAsString("ambari-cluster-by-hostgroup.yaml");

        runTestsAndResizeEffector(new StringReader(yaml));
    }

    protected Application rebind(Application currentApp, RebindOptions options) throws Exception {
        ManagementContext origMgmt = this.mgmt;
        LocalManagementContext newMgmt = this.createNewManagementContext();
        options = RebindOptions.create(options);
        if(options.classLoader == null) {
            options.classLoader(this.classLoader);
        }

        if(options.mementoDir == null) {
            options.mementoDir(this.mementoDir);
        }

        if(options.origManagementContext == null) {
            options.origManagementContext(origMgmt);
        }

        if(options.newManagementContext == null) {
            options.newManagementContext(newMgmt);
        }

        RebindTestUtils.waitForPersisted(currentApp);

        this.mgmt = options.newManagementContext;
        final Collection<Application> newApps = RebindTestUtils.rebindAll(options);
        for (Application newApp : newApps) {
            if (newApp.getId().equals(currentApp.getId())) {
                return newApp;
            }
        }

        throw new IllegalStateException("Application could not be rebinded; serialization probably failed");
    }

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

    private void runTestsAndResizeEffector(StringReader yaml) throws Exception {
        Application app = runTestsAndGetApp(yaml);
        assertResizeClusterWorks(app);
    }

    private void assertResizeClusterWorks(Application app) {
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

        final Maybe<Effector<?>> effector = EffectorUtils.findEffector(ambariHostGroup.getEntityType().getEffectors(), "resizeByDelta");
        if (effector.isAbsentOrNull()) {
            throw new IllegalStateException("Cannot get the addServiceToCluster effector");
        }

        Task<Collection<Entity>> task = ambariHostGroup.invoke(DynamicCluster.RESIZE_BY_DELTA, ImmutableMap.of("delta", 1));

        task.getUnchecked();
        assertFalse(task.isError(), "Effector should not fail");
    }

    private void runTestsAndEffectors(Reader yaml) throws Exception {
        Application app = runTestsAndGetApp(yaml);
        assertAddServiceToClusterEffectorWorks(app);
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

    private String buildLocation(String provider, String region, Map<String, String> options) {
        StringBuffer sb = new StringBuffer("location:\n").append(String.format("  %s:%s:\n", provider, region));
        for (Map.Entry<String, String> o : options.entrySet()) {
            sb.append(String.format("    %s: %s\n", o.getKey(), o.getValue()));
        }

        return sb.toString();
    }
}
