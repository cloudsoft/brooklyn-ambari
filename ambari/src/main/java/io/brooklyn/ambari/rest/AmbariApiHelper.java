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

import java.util.List;

/**
 * Created by duncangrant on 07/01/15.
 */
public interface AmbariApiHelper {
    public void createClusterAPI(String cluster, String version);

    public void addHostToCluster(String cluster, String host);

    public void addServiceToCluster(String cluster, String service);

    public void createComponent(String cluster, String service, String component);

    public void createHostComponent(String cluster, String hostName, String component);

    public RecommendationResponse getRecommendations(List<String> hosts, Iterable<String> services, String stack, String version);

    public void createBlueprint(String blueprintName, DefaultAmbariBluePrint blueprint);

    public void createCluster(String clusterName, String blueprintName, DefaultBluePrintClusterBinding bluePrintClusterBinding);
}
