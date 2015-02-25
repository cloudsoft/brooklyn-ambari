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
package io.brooklyn.ambari.server;

import io.brooklyn.ambari.TestDriver;
import io.brooklyn.ambari.server.AmbariServerImpl;
import io.brooklyn.ambari.server.AmbariServerTest;

import java.util.List;

/**
 * Created by duncangrant on 07/01/15.
 */
public class AmbariServerTestImpl extends AmbariServerImpl implements AmbariServerTest {

    @Override
    public Class getDriverInterface() {
        return TestDriver.class;
    }
    @Override
    public void emitHosts(List<String> hosts) {
        emit(REGISTERED_HOSTS, hosts);
    }

    @Override
    protected void connectSensors() {
        super.connectSensors();
        setAttribute(SERVICE_UP, true);
    }
}
