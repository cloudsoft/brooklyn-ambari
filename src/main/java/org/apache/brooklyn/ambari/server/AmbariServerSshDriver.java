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
package org.apache.brooklyn.ambari.server;

import brooklyn.entity.basic.EntityLocal;
import brooklyn.entity.java.JavaSoftwareProcessSshDriver;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.util.collections.MutableMap;
import brooklyn.util.ssh.BashCommands;
import org.apache.brooklyn.ambari.AmbariInstallHelper;
import org.apache.brooklyn.ambari.DefaultAmbariInstallHelper;
import org.apache.brooklyn.ambari.server.AmbariServerDriver;

import static brooklyn.util.ssh.BashCommands.installPackage;

public class AmbariServerSshDriver extends JavaSoftwareProcessSshDriver implements AmbariServerDriver {

    private final AmbariInstallHelper ambariInstallHelper = new DefaultAmbariInstallHelper();

    public AmbariServerSshDriver(EntityLocal entity, SshMachineLocation machine) {
        super(entity, machine);
    }

    @Override
    public boolean isRunning() {
        return newScript(MutableMap.of("usePidFile", false), CHECK_RUNNING)
                .body.append(BashCommands.sudo("ambari-server status"))
                .execute() == 0;
    }

    @Override
    protected String getLogFileLocation() {
        return "/var/log/ambari-server/ambari-server.log";
    }

    @Override
    public void stop() {
        newScript(STOPPING).body.append(BashCommands.sudo("ambari-server stop")).execute();
    }

    @Override
    public void install() {
        newScript(INSTALLING).body.append(
                ambariInstallHelper.installAmbariRequirements(getMachine()),
                installPackage("ambari-server"),
                BashCommands.sudo("ambari-server setup -s"))
                .failOnNonZeroResultCode()
                .execute();
    }

    @Override
    public void customize() {
    }

    @Override
    public void launch() {
        newScript(LAUNCHING)
                .body.append(BashCommands.sudo("ambari-server start"))
                .failOnNonZeroResultCode()
                .execute();
    }

    @Override
    public void postLaunch() {
        super.postLaunch();
    }
}
