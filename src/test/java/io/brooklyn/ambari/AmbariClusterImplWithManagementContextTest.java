package io.brooklyn.ambari;

import brooklyn.entity.BrooklynAppUnitTestSupport;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.location.basic.LocalhostMachineProvisioningLocation;
import com.google.common.collect.ImmutableList;
import org.apache.http.auth.Credentials;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;

import static org.testng.Assert.*;

public class AmbariClusterImplWithManagementContextTest extends BrooklynAppUnitTestSupport {
    /*

    private AmbariCluster entity;

    @BeforeMethod(alwaysRun = true)
    @Override
    public void setUp() throws Exception {
        super.setUp();
        entity = app.createAndManageChild(EntitySpec.create(AmbariCluster.class).configure(AmbariCluster.SERVER_SPEC, EntitySpec.create(AmbariServerTest.class)));
    }

    @AfterMethod(alwaysRun = true)
    public void shutdown() {
        if (mgmt != null) Entities.destroyAll(mgmt);
    }

    @Test
    public void testTest() throws Exception {
        AmbariCluster entity = mgmt.getEntityManager().createEntity(
                EntitySpec.create(AmbariCluster.class));

        assertEquals(entity.getChildren().size(), 2);
    }

    @Test
    public void testFirstEmitEmptyList() throws Exception {

        Iterable<AmbariServerTest> descendants = Entities.descendants(entity, AmbariServerTest.class);
        AmbariServerTest ambariServer = descendants.iterator().next();
        ambariServer.emitHosts(new LinkedList<String>());

    }

    @Test
    public void testEmitListWithOneItemCallsRegisterOnce() throws Exception {
        Iterable<AmbariServerTest> descendants = Entities.descendants(entity, AmbariServerTest.class);
        AmbariServerTest ambariServer = descendants.iterator().next();
        Iterable<AmbariCluster> descendants1 = Entities.descendants(entity, AmbariCluster.class);
        AmbariCluster ambariCluster = descendants1.iterator().next();

        AmbariApiHelperHostSpy ambariApiHelper = new AmbariApiHelperHostSpy();
        ambariCluster.setAmbariApiHelper(ambariApiHelper);

        ambariCluster.start(ImmutableList.of(new LocalhostMachineProvisioningLocation()));


        ambariServer.emitHosts(ImmutableList.<String>of("host1"));

        assertTrue(ambariApiHelper.wasCalled);

    }

    private static class AmbariApiHelperStub implements AmbariApiHelper {
        @Override
        public void createClusterAPI(String cluster, Credentials usernamePasswordCredentials, String basicAuthorizationValue, URI attribute) {
            throw new RuntimeException("Should not have tried to add cluster");
        }

        @Override
        public void addHostToCluster(String cluster, String host) {
            throw new RuntimeException("Should not have tried to add host to cluster");
        }
    }

    private static class AmbariApiHelperHostSpy extends AmbariApiHelperStub {
        public boolean wasCalled;

        @Override
        public void addHostToCluster(String cluster, String host) {
            wasCalled = true;
        }
    }*/
}