package org.apache.brooklyn.ambari.rest;

import org.codehaus.jackson.map.ObjectMapper;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

public class DefaultAmbariBluePrintTest {

    private Map mapOfJsonFromDefaultAmbariBlueprint;
    private ObjectMapper objectMapper = new ObjectMapper();
    @BeforeMethod
    public void setUp() throws Exception {
        DefaultAmbariBluePrint defaultAmbariBluePrint = DefaultAmbariBluePrint.createBlueprintFromRecommendation(createBlueprintFromExampleJson());
        mapOfJsonFromDefaultAmbariBlueprint = objectMapper.readValue(defaultAmbariBluePrint.toJson(), Map.class);

    }

    @Test
    public void testHostGroupNameSetFromJSON() throws Exception {
        assertTrue(getName(getHostGroup(0)).contains("host-group-"));
    }

    @Test
    public void testHostGroup3ContainsDataNode() throws Exception {
       assertNotNull(getComponent(getHostGroup("host-group-3"), "DATANODE"));
    }

    @Test
    public void testHostGroup4DoesNotContainHDFSClient() throws Exception {
        assertNull(getComponent(getHostGroup("host-group-4"), "HDFS_CLIENT"));
    }

    @Test
    public void testDefaultAmbariBluePrintFromRecommendationHasDefaultConfid(){
        assertEquals(getConfiguration().size(), 1);
    }

    @Test
    public void testDefaultBaseBlueprint2pt2() throws Exception {
        Map blueprintBase = getBlueprints();
        assertTrue(blueprintBase.get("stack_name").equals("HDP"));
        assertTrue(blueprintBase.get("stack_version").equals("2.2"));
    }

    private Map getBlueprints() {
        return (Map) mapOfJsonFromDefaultAmbariBlueprint.get("Blueprints");
    }

    private List getConfiguration() {
        return (List) mapOfJsonFromDefaultAmbariBlueprint.get("configurations");
    }

    private List<Map> getComponents(Map hostGroup) {
        return (List<Map>) hostGroup.get("components");
    }

    private Map getComponent(Map hostGroup, String componentName) {
        for (Map map : getComponents(hostGroup)) {
            if(map.get("name").equals(componentName)){
                return map;
            }
        }
        return null;
    }

    private String getName(Map hostGroup) {
        return ((String) hostGroup.get("name"));
    }

    private Map getHostGroup(String hostGroupName) {
        for (Map map : getHostGroups()) {
            if(getName(map).equals(hostGroupName)){
                    return map;
            }
        }
        return null;
    }

    private Map getHostGroup(int index) {
        return getHostGroups().get(index);
    }

    private List<Map> getHostGroups() {
        return (List<Map>) mapOfJsonFromDefaultAmbariBlueprint.get("host_groups");
    }

    private RecommendationResponse.Resource.Recommendations.Blueprint createBlueprintFromExampleJson() throws java.io.IOException {
        RecommendationResponse recommendationResponse = objectMapper.readValue(EXAMPLE_AMBARI_RECOMMENDATION_RESPONSE_JSON, RecommendationResponse.class);
        return recommendationResponse.getBlueprint();
    }

    public static final String EXAMPLE_AMBARI_RECOMMENDATION_RESPONSE_JSON = "{\n" +
            "  \"resources\" : [\n" +
            "    {\n" +
            "      \"href\" : \"http://u1201.ambari.apache.org:8080/api/v1/stacks/HDP/versions/2.2/recommendations/2\",\n" +
            "      \"hosts\" : [\n" +
            "        \"u1202.ambari.apache.org\",\n" +
            "        \"u1204.ambari.apache.org\",\n" +
            "        \"u1205.ambari.apache.org\",\n" +
            "        \"u1203.ambari.apache.org\"\n" +
            "      ],\n" +
            "      \"services\" : [\n" +
            "        \"ZOOKEEPER\",\n" +
            "        \"HDFS\"\n" +
            "      ],\n" +
            "      \"Recommendation\" : {\n" +
            "        \"id\" : 2\n" +
            "      },\n" +
            "      \"Versions\" : {\n" +
            "        \"stack_name\" : \"HDP\",\n" +
            "        \"stack_version\" : \"2.2\"\n" +
            "      },\n" +
            "      \"recommendations\" : {\n" +
            "        \"blueprint\" : {\n" +
            "          \"configurations\" : null,\n" +
            "          \"host_groups\" : [\n" +
            "            {\n" +
            "              \"name\" : \"host-group-4\",\n" +
            "              \"components\" : [\n" +
            "                {\n" +
            "                  \"name\" : \"ZOOKEEPER_SERVER\"\n" +
            "                },\n" +
            "                {\n" +
            "                  \"name\" : \"JOURNALNODE\"\n" +
            "                },\n" +
            "                {\n" +
            "                  \"name\" : \"ZKFC\"\n" +
            "                },\n" +
            "                {\n" +
            "                  \"name\" : \"DATANODE\"\n" +
            "                }\n" +
            "              ]\n" +
            "            },\n" +
            "            {\n" +
            "              \"name\" : \"host-group-2\",\n" +
            "              \"components\" : [\n" +
            "                {\n" +
            "                  \"name\" : \"HDFS_CLIENT\"\n" +
            "                },\n" +
            "                {\n" +
            "                  \"name\" : \"NAMENODE\"\n" +
            "                },\n" +
            "                {\n" +
            "                  \"name\" : \"ZOOKEEPER_CLIENT\"\n" +
            "                },\n" +
            "                {\n" +
            "                  \"name\" : \"ZOOKEEPER_SERVER\"\n" +
            "                },\n" +
            "                {\n" +
            "                  \"name\" : \"JOURNALNODE\"\n" +
            "                },\n" +
            "                {\n" +
            "                  \"name\" : \"ZKFC\"\n" +
            "                },\n" +
            "                {\n" +
            "                  \"name\" : \"DATANODE\"\n" +
            "                }\n" +
            "              ]\n" +
            "            },\n" +
            "            {\n" +
            "              \"name\" : \"host-group-1\",\n" +
            "              \"components\" : [\n" +
            "                {\n" +
            "                  \"name\" : \"ZOOKEEPER_SERVER\"\n" +
            "                },\n" +
            "                {\n" +
            "                  \"name\" : \"JOURNALNODE\"\n" +
            "                },\n" +
            "                {\n" +
            "                  \"name\" : \"ZKFC\"\n" +
            "                },\n" +
            "                {\n" +
            "                  \"name\" : \"DATANODE\"\n" +
            "                },\n" +
            "                {\n" +
            "                  \"name\" : \"SECONDARY_NAMENODE\"\n" +
            "                }\n" +
            "              ]\n" +
            "            },\n" +
            "            {\n" +
            "              \"name\" : \"host-group-3\",\n" +
            "              \"components\" : [\n" +
            "                {\n" +
            "                  \"name\" : \"JOURNALNODE\"\n" +
            "                },\n" +
            "                {\n" +
            "                  \"name\" : \"ZKFC\"\n" +
            "                },\n" +
            "                {\n" +
            "                  \"name\" : \"DATANODE\"\n" +
            "                }\n" +
            "              ]\n" +
            "            }\n" +
            "          ]\n" +
            "        },\n" +
            "        \"blueprint_cluster_binding\" : {\n" +
            "          \"host_groups\" : [\n" +
            "            {\n" +
            "              \"name\" : \"host-group-1\",\n" +
            "              \"hosts\" : [\n" +
            "                {\n" +
            "                  \"fqdn\" : \"u1203.ambari.apache.org\"\n" +
            "                }\n" +
            "              ]\n" +
            "            },\n" +
            "            {\n" +
            "              \"name\" : \"host-group-2\",\n" +
            "              \"hosts\" : [\n" +
            "                {\n" +
            "                  \"fqdn\" : \"u1202.ambari.apache.org\"\n" +
            "                }\n" +
            "              ]\n" +
            "            },\n" +
            "            {\n" +
            "              \"name\" : \"host-group-3\",\n" +
            "              \"hosts\" : [\n" +
            "                {\n" +
            "                  \"fqdn\" : \"u1205.ambari.apache.org\"\n" +
            "                }\n" +
            "              ]\n" +
            "            },\n" +
            "            {\n" +
            "              \"name\" : \"host-group-4\",\n" +
            "              \"hosts\" : [\n" +
            "                {\n" +
            "                  \"fqdn\" : \"u1204.ambari.apache.org\"\n" +
            "                }\n" +
            "              ]\n" +
            "            }\n" +
            "          ]\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}\n";

}