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

package io.brooklyn.ambari.service;

import static org.apache.brooklyn.util.ssh.BashCommands.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;

import org.apache.brooklyn.api.mgmt.Task;
import org.apache.brooklyn.core.effector.EffectorTasks;
import org.apache.brooklyn.core.effector.ssh.SshEffectorTasks;
import org.apache.brooklyn.core.entity.Entities;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import io.brooklyn.ambari.AmbariCluster;
import io.brooklyn.ambari.agent.AmbariAgent;
import io.brooklyn.ambari.server.AmbariServer;

/**
 * Ranger hadoop service implementation. This will install and preform all the prerequisites needed for Ranger.
 */
public class RangerImpl extends AbstractExtraService implements Ranger {

    private static final Logger LOG = LoggerFactory.getLogger(RangerImpl.class);

    private static final String DB_HOST = "localhost";
    private static final String DB_NAME = "ranger";

    private static final List<String> REQUIRES_JDBC_DRIVER = Lists.newArrayList("NAMENODE", "HBASE_MASTER", "HBASE_REGIONSERVER");
    private static final List<String> REQUIRES_MYSQL_CLIENT = Lists.newArrayList("RANGER_ADMIN");

    @Override
    public Map<String, Map> getAmbariConfig(AmbariCluster ambariCluster) {
        String rangerFqdn = getFqdnFor(ambariCluster, "RANGER_ADMIN");

        if (StringUtils.isEmpty(rangerFqdn)) {
            rangerFqdn = DB_HOST;
        }

        return ImmutableMap.<String, Map>builder()
                .put("admin-properties", ImmutableMap.builder()
                        .put("db_host", DB_HOST)
                        .put("db_name", DB_NAME)
                        .put("db_root_user", getConfig(DB_USER))
                        .put("db_root_password", getConfig(DB_PASSWORD))
                        .put("policymgr_external_url", String.format("http://%s:%d", rangerFqdn, 6080))
                        .build())
                .put("ranger-admin-site", ImmutableMap.builder()
                        .put("ranger.jpa.jdbc.url", String.format("jdbc:mysql://%s/%s", DB_HOST, DB_NAME))
                        .build())
                .put("ranger-env", ImmutableMap.builder()
                        .put("ranger_admin_password", getConfig(RANGER_PASSWORD))
                        .build())
                .build();
    }

    @Override
    public void preClusterDeploy(AmbariCluster ambariCluster) throws ExtraServiceException {
        try {
            LOG.info("{} performing Ranger requirements on Ambari server", this);
            Task<List<?>> rangerServerRequirementsTasks = parallelListenerTask(ambariCluster.getAmbariServers(), new AmbariServerRequirementsFunction());
            Entities.submit(this, rangerServerRequirementsTasks).get();

            LOG.info("{} performing Ranger requirements on Ambari nodes with {} components installed", this, REQUIRES_JDBC_DRIVER);
            Task<List<?>> rangerAgentRequirementsTasks = parallelListenerTask(ambariCluster.getAmbariAgents(), new AmbariAgentRequirementsFunction(), REQUIRES_JDBC_DRIVER);
            Entities.submit(this, rangerAgentRequirementsTasks).get();

            LOG.info("{} performing Ranger requirements on the Ranger host", this);
            Task<List<?>> mysqlRequirementTasks = parallelListenerTask(ambariCluster.getAmbariAgents(), new MysqlRequirementsFunction(), REQUIRES_MYSQL_CLIENT);
            Entities.submit(this, mysqlRequirementTasks).get();
        } catch (ExecutionException | InterruptedException ex) {
            // If something failed, we propagate the exception.
            throw new ExtraServiceException(ex.getMessage());
        }
    }

    @Override
    public void postClusterDeploy(AmbariCluster ambariCluster) throws ExtraServiceException {
    }

    @Override
    public void preHostGroupScale(AmbariCluster ambariCluster, Iterable<AmbariAgent> newAgents) {
        try {
            LOG.info("{} performing Ranger requirements on Ambari nodes with {} components installed", this, REQUIRES_JDBC_DRIVER);
            Task<List<?>> rangerAgentRequirementsTasks = parallelListenerTask(newAgents, new AmbariAgentRequirementsFunction(), REQUIRES_JDBC_DRIVER);
            Entities.submit(this, rangerAgentRequirementsTasks).get();
        } catch (ExecutionException | InterruptedException ex) {
            // If something failed, we propagate the exception.
            throw new ExtraServiceException(ex.getMessage());
        }
    }

    @Nullable
    private String getFqdnFor(AmbariCluster ambariCluster, String component) {
        for (AmbariAgent ambariAgent : ambariCluster.getAmbariAgents()) {
            Preconditions.checkNotNull(ambariAgent.getAttribute(AmbariAgent.COMPONENTS));
            if (!ambariAgent.getAttribute(AmbariAgent.COMPONENTS).contains(component)) {
                continue;
            }
            return ambariAgent.getFqdn();
        }
        return null;
    }

    class AmbariServerRequirementsFunction extends AbstractExtraServicesTask<AmbariServer> {

        @Override
        public Task<Integer> sshTaskApply(AmbariServer ambariServer) {
            return SshEffectorTasks
                    .ssh(
                            installPackageOr(ImmutableMap.of(), "mysql-connector-java", installPackageOrFail(ImmutableMap.of(), "libmysql-java")),
                            sudo("ambari-server setup --jdbc-db=mysql --jdbc-driver=/usr/share/java/mysql-connector-java.jar"))
                    .summary("Initialise Ranger requirements on " + ambariServer.getId())
                    .machine(EffectorTasks.getSshMachine(ambariServer))
                    .newTask()
                    .asTask();
        }
    }

    class AmbariAgentRequirementsFunction extends AbstractExtraServicesTask<AmbariAgent> {

        @Override
        public Task<Integer> sshTaskApply(AmbariAgent ambariAgent) {

            return SshEffectorTasks
                    .ssh(installPackageOr(ImmutableMap.of(), "mysql-connector-java", installPackageOrFail(ImmutableMap.of(), "libmysql-java")))
                    .summary("Initialise Ranger requirements on " + ambariAgent.getId())
                    .machine(EffectorTasks.getSshMachine(ambariAgent))
                    .newTask()
                    .asTask();
        }
    }

    class MysqlRequirementsFunction extends AbstractExtraServicesTask<AmbariAgent> {
        @Override
        public Task<Integer> sshTaskApply(AmbariAgent ambariAgent) {

            return SshEffectorTasks
                    .ssh(
                            installExecutable("mysql-server"),
                            installPackageOr(ImmutableMap.of(), "mysql", installPackageOrFail(ImmutableMap.of(), "mysql-client")),
                            alternatives(sudo("service mysqld restart"), sudo("service mysql restart")),
                            String.format("mysql -u root -e 'create user `%s`@`%s` identified by \"%s\";'", getConfig(DB_USER), DB_HOST, getConfig(DB_PASSWORD)),
                            String.format("mysql -u root -e 'grant all privileges on *.* to `%s`@`%s` identified by \"%s\" with grant option; flush privileges;'", getConfig(DB_USER), DB_HOST, getConfig(DB_PASSWORD)))
                    .summary("Initialise MySQL requirements on " + ambariAgent.getId())
                    .machine(EffectorTasks.getSshMachine(ambariAgent))
                    .newTask()
                    .asTask();
        }
    }
}
