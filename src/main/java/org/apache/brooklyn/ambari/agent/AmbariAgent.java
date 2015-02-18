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
package org.apache.brooklyn.ambari.agent;

import brooklyn.catalog.Catalog;
import brooklyn.config.ConfigKey;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.basic.SoftwareProcess;
import brooklyn.entity.java.UsesJava;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.util.flags.SetFromFlag;
import brooklyn.util.javalang.JavaClassNames;

/**
 * Created by duncangrant on 15/12/14.
 */
@Catalog(name="Ambari Agent", description="Ambari Agent: part of an ambari cluster that runs on each node that will form part of the Hadoop cluster")
@ImplementedBy(AmbariAgentImpl.class)
public interface AmbariAgent extends SoftwareProcess, UsesJava {

    @SetFromFlag("configFileUrl")
    ConfigKey<String> TEMPLATE_CONFIGURATION_URL = ConfigKeys.newConfigKey(
            "ambari.templateConfigurationUrl", "Template file (in freemarker format) for the ambari-agent.ini file",
            JavaClassNames.resolveClasspathUrl(AmbariAgent.class, "ambari-agent.ini"));

// TODO remove Random
    @SetFromFlag("ambariServerFQDN")
    ConfigKey<String> AMBARI_SERVER_FQDN = ConfigKeys.newStringConfigKey(
            "ambari.server.fqdn", "Fully Qualified Domain Name of ambari server that agent should register to", "Random");
}
