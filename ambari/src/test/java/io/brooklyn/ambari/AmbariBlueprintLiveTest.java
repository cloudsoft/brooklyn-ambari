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

import java.io.Reader;
import java.io.StringReader;
import java.util.Map;

import org.apache.brooklyn.api.entity.Application;
import org.apache.brooklyn.core.mgmt.rebind.RebindOptions;
import org.apache.brooklyn.core.mgmt.rebind.RebindTestUtils;
import org.apache.brooklyn.launcher.blueprints.AbstractBlueprintTest;
import org.apache.brooklyn.util.collections.MutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;

@Test(singleThreaded = false, threadPoolSize = 2)
public class AmbariBlueprintLiveTest extends AbstractBlueprintTest {

    private static final Logger LOG = LoggerFactory.getLogger(AmbariBlueprintLiveTest.class);
    private BlueprintTestHelper blueprintTestHelper;
    private AmbariLiveTestHelper ambariLiveTestHelper;

    // Move setup behaviour to class so that we can run tests against single brooklyn instance
    @BeforeClass(alwaysRun = true)
    public void setupClass() throws Exception {
        super.setUp();
    }

    @BeforeMethod
    @Override
    public void setUp() throws Exception {
        super.setUp();
        blueprintTestHelper = new BlueprintTestHelper();
        ambariLiveTestHelper = new AmbariLiveTestHelper();
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

        String yaml = blueprintTestHelper.getYamlFileContents(yamlFile, provider, region, options, this);

        runTestsAndGetApp(new StringReader(yaml));
    }

    @Test(groups = {"Live"})
    public void testAddServiceToCluster() throws Exception {
        String yamlFile = "ambari-cluster-small.yaml";
        String provider = "softlayer";
        String region = "ams01";
        Map<String, String> options = ImmutableMap.<String, String>builder()
                .put("minRam", "16384")
                .put("minCores", "4")
                .put("osFamily", "ubuntu")
                .put("osVersionRegex", "12.*")
                .put("stopIptables", "true")
                .build();
        LOG.info("Testing {} on {}:{} using {} ({})", new Object[]{yamlFile, provider, region, "Small cluster on Softlayer adding FLUME", options});

        String yaml = blueprintTestHelper.getYamlFileContents(yamlFile, provider, region, options, this);

        final StringReader yaml1 = new StringReader(yaml);
        ambariLiveTestHelper.assertAddServiceToClusterEffectorWorks(runTestsAndGetApp(yaml1));
    }

    @Test(groups = {"Live"})
    public void testExpandCluster() throws Exception {
        String yamlFile = "ambari-cluster-by-hostgroup.yaml";
        String provider = "softlayer";
        String region = "ams01";
        Map<String, String> options = ImmutableMap.<String, String>builder()
                .put("minRam", "16384")
                .put("minCores", "4")
                .put("osFamily", "ubuntu")
                .put("osVersionRegex", "12.*")
                .put("stopIptables", "true")
                .build();

        LOG.info("Testing {} on {}:{} using {} ({})", new Object[]{yamlFile, provider, region, "Cluster by HostGroup on Softlayer scaling DataNodes", options});
        String yaml =
                blueprintTestHelper.getYamlFileContents(
                        yamlFile,
                        provider,
                        region,
                        options,
                        this);

        Application app = runTestsAndGetApp(new StringReader(yaml));
        ambariLiveTestHelper.assertResizeClusterWorks(app);
    }

    protected Application rebind(Application currentApp, RebindOptions options) throws Exception {
        options = blueprintTestHelper.getRebindOptions(options, this.createNewManagementContext(), this.classLoader, this.mementoDir, this.mgmt);
        RebindTestUtils.waitForPersisted(currentApp);
        this.mgmt = options.newManagementContext;
        return blueprintTestHelper.rebindAndGetApp(currentApp, options);
    }

    protected Application runTestsAndGetApp(Reader yaml) throws Exception {
        Application app = this.launcher.launchAppYaml(yaml);
        blueprintTestHelper.assertNoFires(app);
        Application newApp = rebind(app, RebindOptions.create());
        blueprintTestHelper.assertNoFires(newApp);
        this.ambariLiveTestHelper.assertHadoopClusterEventuallyDeployed(newApp);

        return newApp;
    }

}
