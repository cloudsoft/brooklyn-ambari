package io.brooklyn.ambari;

import org.testng.annotations.Test;

import brooklyn.launcher.blueprints.AbstractBlueprintTest;

public class AmbariBlueprintLiveTest extends AbstractBlueprintTest {

    @Test(groups={"Live", "WIP"})
    public void testAmbariCluster() throws Exception {
        runTest("ambari-cluster.yaml");
    }
}
