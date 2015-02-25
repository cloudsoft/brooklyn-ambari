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
package io.brooklyn.ambari;

import brooklyn.entity.basic.AbstractSoftwareProcessDriver;
import brooklyn.entity.basic.EntityLocal;
import brooklyn.entity.drivers.EntityDriver;
import brooklyn.location.Location;

/**
 * Created by duncangrant on 07/01/15.
 */
public class TestDriver extends AbstractSoftwareProcessDriver implements EntityDriver {
    public TestDriver(EntityLocal entity, Location location) {
        super(entity, location);
    }

    @Override
    public boolean isRunning() {
        return true;
    }

    @Override
    public void stop() {

    }

    @Override
    public void runPreInstallCommand(String command) {

    }

    @Override
    public void setup() {

    }

    @Override
    public void copyInstallResources() {

    }

    @Override
    public void install() {

    }

    @Override
    public void runPostInstallCommand(String command) {

    }

    @Override
    public void copyRuntimeResources() {

    }

    @Override
    public void customize() {

    }

    @Override
    public void runPreLaunchCommand(String command) {

    }

    @Override
    public void launch() {

    }

    @Override
    public void runPostLaunchCommand(String command) {

    }
}
