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

import java.util.Map;

/**
* Created by duncangrant on 21/01/15.
*/
public class RecommendationResponse {
    public Resource[] resources;

    public Blueprint getBlueprint() {
        return resources != null && resources.length > 0 ?resources[0].recommendations.blueprint : null;
    }

    public BlueprintClusterBinding getBlueprintClusterBinding() {
        return resources != null && resources.length > 0 ? resources[0].recommendations.blueprint_cluster_binding: null;
    }

    public static class Resource {
        public String href;
        public String[] hosts;
        public String[] services;
        public Map<?,?> Recommendation;
        public Map<?,?> Versions;
        public Recommendations recommendations;
    }
    
    public static class Recommendations {
        public Blueprint blueprint;
        public BlueprintClusterBinding blueprint_cluster_binding;
    }
    
    public static class Blueprint {
        public Map<?,?> configurations;
        public HostGroup[] host_groups;
    }
    
    public static class HostGroup {
        public String name;
        public Map<?,?>[] components;
    }

    public static class BlueprintClusterBinding {
        public ClusterBindingHostGroup[] host_groups; 
    }
    
    public static class ClusterBindingHostGroup {
        public String name;
        public Map<?,?>[] hosts;
    }
}
