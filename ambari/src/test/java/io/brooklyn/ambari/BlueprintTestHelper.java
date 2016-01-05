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

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.brooklyn.api.entity.Application;
import org.apache.brooklyn.api.entity.Entity;
import org.apache.brooklyn.api.mgmt.ManagementContext;
import org.apache.brooklyn.core.entity.Attributes;
import org.apache.brooklyn.core.entity.Entities;
import org.apache.brooklyn.core.entity.lifecycle.Lifecycle;
import org.apache.brooklyn.core.mgmt.internal.LocalManagementContext;
import org.apache.brooklyn.core.mgmt.rebind.RebindOptions;
import org.apache.brooklyn.core.mgmt.rebind.RebindTestUtils;
import org.apache.brooklyn.entity.software.base.SoftwareProcess;
import org.apache.brooklyn.test.Asserts;
import org.apache.brooklyn.test.EntityTestUtils;
import org.apache.brooklyn.util.core.ResourceUtils;
import org.apache.brooklyn.util.time.Duration;
import org.testng.Assert;

import com.google.common.collect.ImmutableMap;

public class BlueprintTestHelper {
    void assertNoFires(final Entity app) {
        EntityTestUtils.assertAttributeEqualsEventually(app, Attributes.SERVICE_UP, Boolean.valueOf(true));
        EntityTestUtils.assertAttributeEqualsEventually(app, Attributes.SERVICE_STATE_ACTUAL, Lifecycle.RUNNING);
        Asserts.succeedsEventually(ImmutableMap.of("timeout", Duration.FIVE_MINUTES), new Runnable() {
            public void run() {
                Iterator i$ = Entities.descendants(app).iterator();

                while (i$.hasNext()) {
                    Entity entity = (Entity) i$.next();
                    Assert.assertNotEquals(entity.getAttribute(Attributes.SERVICE_STATE_ACTUAL), Lifecycle.ON_FIRE);
                    Assert.assertNotEquals(entity.getAttribute(Attributes.SERVICE_UP), Boolean.valueOf(false));
                    if (entity instanceof SoftwareProcess) {
                        EntityTestUtils.assertAttributeEquals(entity, Attributes.SERVICE_STATE_ACTUAL, Lifecycle.RUNNING);
                        EntityTestUtils.assertAttributeEquals(entity, Attributes.SERVICE_UP, Boolean.TRUE);
                    }
                }

            }
        });
    }

    RebindOptions getRebindOptions(final RebindOptions rebindOptions, final LocalManagementContext newMgmt, final ClassLoader classLoader, final File mementoDir, final ManagementContext mgmt) {
        ManagementContext origMgmt = mgmt;
        RebindOptions newOptions = RebindOptions.create(rebindOptions);
        if(newOptions.classLoader == null) {
            newOptions.classLoader(classLoader);
        }

        if(newOptions.mementoDir == null) {
            newOptions.mementoDir(mementoDir);
        }

        if(newOptions.origManagementContext == null) {
            newOptions.origManagementContext(origMgmt);
        }

        if(newOptions.newManagementContext == null) {
            newOptions.newManagementContext(newMgmt);
        }
        return newOptions;
    }

    Application rebindAndGetApp(final Application currentApp, final RebindOptions options) throws Exception {
        final Collection<Application> newApps = RebindTestUtils.rebindAll(options);
        for (Application newApp : newApps) {
            if (newApp.getId().equals(currentApp.getId())) {
                return newApp;
            }
        }

        throw new IllegalStateException("Application could not be rebinded; serialization probably failed");
    }

    String buildLocation(final String provider, final String region, final Map<String, String> options) {
        StringBuffer sb = new StringBuffer("location:\n").append(String.format("  %s:%s:\n", provider, region));
        for (Map.Entry<String, String> o : options.entrySet()) {
            sb.append(String.format("    %s: %s\n", o.getKey(), o.getValue()));
        }

        return sb.toString();
    }

    String getYamlFileContents(final String yamlFile, final String provider, final String region, final Map<String, String> options, final AmbariBlueprintLiveTest ambariBlueprintLiveTest) {
        return buildLocation(provider, region, options) + new ResourceUtils(ambariBlueprintLiveTest).getResourceAsString(yamlFile);
    }
}
