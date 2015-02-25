package io.brooklyn.ambari.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jayway.jsonpath.PathNotFoundException;

import org.testng.annotations.Test;

import io.brooklyn.ambari.server.AmbariServerImpl;

import java.util.List;

import static brooklyn.test.Asserts.assertThat;
import static brooklyn.util.collections.CollectionFunctionals.contains;
import static brooklyn.util.collections.CollectionFunctionals.sizeEquals;

public class AmbariServerImplTest {

    private AmbariServerImpl ambariServer = new AmbariServerImpl();

    @Test(expectedExceptions = PathNotFoundException.class)
    public void testNullJsonThrows() {
        assertThat(getHostsFromJson("{}"), sizeEquals(0));
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testEmptyJsonThrows() {
        assertThat(ambariServer.getHosts().apply(null), sizeEquals(0));
    }

    @Test
    public void testOneHostReturnsSingleItemInList() {
        assertThat(getHostsFromJson(JSON_WITH_ONE_HOST), contains("ip-10-121-18-69.eu-west-1.compute.internal"));
    }

    @Test
    public void testFourHostsReturnsFourItemsInList() {
        assertThat(getHostsFromJson(JSON_WITH_FOUR_HOSTS), contains("ip-10-121-18-69.eu-west-1.compute.internal"));
        assertThat(getHostsFromJson(JSON_WITH_FOUR_HOSTS), contains("ip-10-121-20-75.eu-west-1.compute.internal"));
        assertThat(getHostsFromJson(JSON_WITH_FOUR_HOSTS), contains("ip-10-122-4-179.eu-west-1.compute.internal"));
        assertThat(getHostsFromJson(JSON_WITH_FOUR_HOSTS), contains("ip-10-91-154-171.eu-west-1.compute.internal"));
    }


    private List<String> getHostsFromJson(String json) {
        return ambariServer.getHosts().apply(getAsJsonObject(json));
    }

    private JsonObject getAsJsonObject(String jsonWithOneHost) {
        return new JsonParser().parse(jsonWithOneHost).getAsJsonObject();
    }

    private static final String JSON_WITH_ONE_HOST = "{\n" +
            "  \"href\" : \"http://ec2-54-228-116-93.eu-west-1.compute.amazonaws.com:8080/api/v1/hosts\",\n" +
            "  \"items\" : [\n" +
            "    {\n" +
            "      \"href\" : \"http://ec2-54-228-116-93.eu-west-1.compute.amazonaws.com:8080/api/v1/hosts/ip-10-121-18-69.eu-west-1.compute.internal\",\n" +
            "      \"Hosts\" : {\n" +
            "        \"host_name\" : \"ip-10-121-18-69.eu-west-1.compute.internal\"\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    private static final String JSON_WITH_FOUR_HOSTS = "{\n" +
            "  \"href\" : \"http://ec2-54-228-116-93.eu-west-1.compute.amazonaws.com:8080/api/v1/hosts\",\n" +
            "  \"items\" : [\n" +
            "    {\n" +
            "      \"href\" : \"http://ec2-54-228-116-93.eu-west-1.compute.amazonaws.com:8080/api/v1/hosts/ip-10-121-18-69.eu-west-1.compute.internal\",\n" +
            "      \"Hosts\" : {\n" +
            "        \"host_name\" : \"ip-10-121-18-69.eu-west-1.compute.internal\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"href\" : \"http://ec2-54-228-116-93.eu-west-1.compute.amazonaws.com:8080/api/v1/hosts/ip-10-121-20-75.eu-west-1.compute.internal\",\n" +
            "      \"Hosts\" : {\n" +
            "        \"host_name\" : \"ip-10-121-20-75.eu-west-1.compute.internal\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"href\" : \"http://ec2-54-228-116-93.eu-west-1.compute.amazonaws.com:8080/api/v1/hosts/ip-10-122-4-179.eu-west-1.compute.internal\",\n" +
            "      \"Hosts\" : {\n" +
            "        \"host_name\" : \"ip-10-122-4-179.eu-west-1.compute.internal\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"href\" : \"http://ec2-54-228-116-93.eu-west-1.compute.amazonaws.com:8080/api/v1/hosts/ip-10-91-154-171.eu-west-1.compute.internal\",\n" +
            "      \"Hosts\" : {\n" +
            "        \"host_name\" : \"ip-10-91-154-171.eu-west-1.compute.internal\"\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}";

}