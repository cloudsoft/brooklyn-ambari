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
package org.apache.brooklyn.ambari.rest;

import static org.apache.brooklyn.ambari.rest.DefaultAmbariBluePrint.toMaps;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.brooklyn.ambari.rest.RecommendationResponse.Resource.Recommendations.BlueprintClusterBinding;

import brooklyn.util.collections.Jsonya;

import com.google.common.collect.ImmutableMap;

/**
 * @author duncangrant
 */
public class DefaultBluePrintClusterBinding implements AsMap {
    private List<AsMap> hostGroups = new LinkedList<AsMap>();
    private String bluePrintName;

    public DefaultBluePrintClusterBinding(BlueprintClusterBinding blueprintClusterBinding) {
        for (BlueprintClusterBinding.HostGroup hostGroup : blueprintClusterBinding.host_groups) {
            hostGroups.add(new HostGroup(hostGroup));
        }
    }

    public static DefaultBluePrintClusterBinding createFromRecommendation(RecommendationResponse.Resource.Recommendations.BlueprintClusterBinding blueprintClusterBinding) {
        return new DefaultBluePrintClusterBinding(blueprintClusterBinding);
    }

    public String toJson() {
        return Jsonya.newInstance().add(this.asMap()).root().toString();
    }

    @Override
    public Map asMap() {
        return ImmutableMap.of("blueprint", bluePrintName, "default_password", "admin", "host_groups", toMaps(hostGroups));
    }

    public void setBluePrintName(String bluePrintName) {
        this.bluePrintName = bluePrintName;
    }

    private static class HostGroup implements AsMap {

        private final String name;
        private final List<Host> hosts = new LinkedList<Host>();

        public HostGroup(BlueprintClusterBinding.HostGroup hostGroup) {
            name = hostGroup.name;
            for (Map host : hostGroup.hosts) {
                hosts.add(new Host(host));
            }
        }

        @Override
        public Map asMap() {
            return ImmutableMap.of("name", name, "hosts", toMaps(hosts));

        }

        private class Host implements AsMap {

            private final Map hostParams;

            public Host(Map host) {
                hostParams = ImmutableMap.copyOf(host);
            }

            @Override
            public Map asMap() {
                return hostParams;
            }
        }
    }
}
