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
package io.brooklyn.ambari.rest;

import brooklyn.util.collections.Jsonya;
import brooklyn.util.exceptions.Exceptions;
import brooklyn.util.http.HttpTool;
import brooklyn.util.http.HttpToolResponse;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HttpHeaders;

import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.List;

public class DefaultAmbariApiHelper implements AmbariApiHelper {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultAmbariApiHelper.class);
    private final UsernamePasswordCredentials usernamePasswordCredentials;
    private final URI baseUri;
    
    public DefaultAmbariApiHelper(UsernamePasswordCredentials usernamePasswordCredentials, URI baseUri) {
        this.usernamePasswordCredentials = usernamePasswordCredentials;
        this.baseUri = baseUri;
    }
    
    @Override
    public void createClusterAPI(String cluster, String version) {
        String json = Jsonya.newInstance().at("Clusters").put("version", version).root().toString();
        post(Optional.of(json), "/api/v1/clusters/{cluster}", cluster);
    }

    @Override
    public void addHostToCluster(String cluster, String host) {        
        post("/api/v1/clusters/{cluster}/hosts/{host}", cluster, host);
    }

    @Override
    public void addServiceToCluster(String cluster, String service) {
        post("/api/v1/clusters/{cluster}/services/{service}", cluster, service);
    }

    @Override
    public void createComponent(String cluster, String service, String component) {
        post("/api/v1/clusters/{cluster}/services/{service}/components/{component}", cluster, service, component);
    }

    @Override
    public void createHostComponent(String cluster, String hostName, String component) {
        post("/api/v1/clusters/{cluster}/hosts/{hostName}/host_components/{component}", cluster, hostName, component);
    }

    @Override
    public void createBlueprint(String blueprintName, DefaultAmbariBluePrint blueprint) {
        post(Optional.of(blueprint.toJson()), "/api/v1/blueprints/{blueprintname}", blueprintName);
    }

    @Override
    public RecommendationResponse getRecommendations(List<String> hosts, Iterable<String> services, String stack, String version) {
        String json = Jsonya.newInstance()
                .root().put("hosts", hosts)
                .root().put("services", services)
                .root().put("recommend", "host_groups")
                .root().toString();
        return getRecommendationResponseFrom(post(Optional.of(json),
                "/api/v1/stacks/{stack}/versions/{version}/recommendations", stack, version));
    }

    private RecommendationResponse getRecommendationResponseFrom(HttpToolResponse httpToolResponse) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String contentAsString = httpToolResponse.getContentAsString();
            return objectMapper.readValue(contentAsString, RecommendationResponse.class);
        } catch (IOException e) {
            LOG.error("Error getting recommendations", e);
            throw Exceptions.propagate(e);
        }
    }

    @Override
    public void createCluster(String clusterName, String blueprintName, DefaultBluePrintClusterBinding bluePrintClusterBinding) {
        bluePrintClusterBinding.setBluePrintName(blueprintName);
        post(Optional.of(bluePrintClusterBinding.toJson()), "/api/v1/clusters/{clustername}", clusterName);
    }
    
    private HttpToolResponse post(String path, Object... templateParams) {
        return post(Optional.<String>absent(), path, templateParams);
    }

    private HttpToolResponse post(Optional<String> body, String path, Object... templateParams) {
        URI uri = UriBuilder.fromUri(baseUri).path(path).build(templateParams);
        HttpClient httpClient = createHttpClient(uri);
        ImmutableMap<String, String> headers = ImmutableMap.of("x-requested-by", "bob", HttpHeaders.AUTHORIZATION, HttpTool.toBasicAuthorizationValue(usernamePasswordCredentials));
        LOG.debug("POST: uri={}, headers={}, body={}", new Object[]{
                uri,
                Joiner.on(",").withKeyValueSeparator("=").join(headers),
                body.isPresent() ? body.get() : "(empty)"});
        return post(body, uri, httpClient, headers);
    }

    private HttpToolResponse post(Optional<String> body, URI uri, HttpClient httpClient, ImmutableMap<String, String> headers) {
        HttpToolResponse httpToolResponse = HttpTool.httpPost(httpClient, uri, headers, body.isPresent() ? body.get().getBytes() : new byte[0]);
        if (!isAcceptableReturnCode(httpToolResponse)) {
            throw new AmbariApiException(httpToolResponse);
        }
        LOG.debug("Response from server: {}", httpToolResponse.getContentAsString());
        return httpToolResponse;
    }

    private HttpClient createHttpClient(URI uri) {
        //TODO trustAll should probably be fixed
         return HttpTool.httpClientBuilder().credentials(usernamePasswordCredentials).trustAll().uri(uri).build();
    }

    private boolean isAcceptableReturnCode(HttpToolResponse httpToolResponse) {
        return httpToolResponse.getResponseCode() / 100 == 2;
    }

}