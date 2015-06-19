package io.brooklyn.ambari.server;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import io.brooklyn.ambari.rest.DefaultAmbariBluePrintTest;
import io.brooklyn.ambari.rest.DefaultBluePrintClusterBinding;
import io.brooklyn.ambari.rest.RecommendationResponse;
import io.brooklyn.ambari.rest.RecommendationResponse.BlueprintClusterBinding;

import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DefaultBluePrintClusterBindingTest {

    private ObjectMapper objectMapper = new ObjectMapper();
    private Map mapOfJsonFromDefaultAmbariClusterBinding;

    @BeforeMethod(alwaysRun=true)
    public void setUp() throws Exception {
        DefaultBluePrintClusterBinding defaultBluePrintClusterBinding = DefaultBluePrintClusterBinding.createFromRecommendation(createClusterBindingFromExampleJson());
        defaultBluePrintClusterBinding.setBluePrintName("bp1");
        mapOfJsonFromDefaultAmbariClusterBinding = objectMapper.readValue(defaultBluePrintClusterBinding.toJson(), Map.class);
    }

    @Test
    public void testBinds4HostGroups() throws Exception {
        assertTrue(getHostGroups().size() == 4);
    }

    @Test
    public void testBindsHostGroup2To1202() throws Exception {
        assertNotNull(getHost(getHostGroup("host-group-2"), "u1202.ambari.apache.org"));

    }

    private Map getHost(Map hostGroup, String fqdn) {
        for (Map map : getHosts(hostGroup)) {
            if(map.get("fqdn").equals(fqdn)){
                return map;
            }
        }
        return null;
    }

    private List<Map> getHosts(Map hostGroup) {
        return (List<Map>) hostGroup.get("hosts");
    }


    private Map getHostGroup(String hostGroupName) {
        for (Map map : getHostGroups()) {
            if (getName(map).equals(hostGroupName)) {
                return map;
            }
        }
        return null;
    }

    private String getName(Map hostGroup) {
        return ((String) hostGroup.get("name"));
    }

    private List<Map> getHostGroups() {
        return (List<Map>) mapOfJsonFromDefaultAmbariClusterBinding.get("host_groups");
    }

    private BlueprintClusterBinding createClusterBindingFromExampleJson() throws java.io.IOException {
        RecommendationResponse recommendationResponse = objectMapper.readValue(DefaultAmbariBluePrintTest.EXAMPLE_AMBARI_RECOMMENDATION_RESPONSE_JSON, RecommendationResponse.class);
        return recommendationResponse.getBlueprintClusterBinding();
    }
}