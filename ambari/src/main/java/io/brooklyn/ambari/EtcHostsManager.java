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

import static org.apache.brooklyn.util.ssh.BashCommands.*;

import java.util.Map;

import org.apache.brooklyn.api.entity.Entity;
import org.apache.brooklyn.api.sensor.AttributeSensor;
import org.apache.brooklyn.core.entity.Attributes;
import org.apache.brooklyn.core.location.Machines;
import org.apache.brooklyn.location.ssh.SshMachineLocation;
import org.apache.brooklyn.util.guava.Maybe;
import org.apache.brooklyn.util.ssh.BashCommands;
import org.apache.brooklyn.util.text.Identifiers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

public class EtcHostsManager {

    private static final Logger LOG = LoggerFactory.getLogger(EtcHostsManager.class);

    private EtcHostsManager() {
    }

    /**
     * For each machine, set its own hostname correctly, and add the other entity's details to /etc/hosts
     *
     * @param machines machines to have their hostname and /etc/hosts set
     */
    public static void setHostsOnMachines(Iterable<? extends Entity> machines) {
        setHostsOnMachines(machines, Attributes.SUBNET_ADDRESS);
    }

    public static void setHostsOnMachines(Iterable<? extends Entity> machines, AttributeSensor<String> addressSensor) {
        Map<String, String> mapping = gatherIpHostnameMapping(machines, addressSensor);

        for (Entity e : machines) {
            Maybe<SshMachineLocation> sshLocation = Machines.findUniqueSshMachineLocation(e.getLocations());

            if (!sshLocation.isPresentAndNonNull()) {
                LOG.debug("{} has no {}, not setting hostname or updating /etc/hosts", e, SshMachineLocation.class);
                continue;
            }

            SshMachineLocation loc = sshLocation.get();
            ImmutableList.Builder<String> commands = ImmutableList.builder();

            // It would be great if we could use BashCommands.setHostname(), but it doesn't quite do what we need: it
            // maps the hostname to 127.0.0.1. But this then means that e.g. "ping myhostname" pings 127.0.0.1, and that
            // behaviour causes some processes to bind ports to 127.0.0.1 instead of 0.0.0.0. What we need instead is
            // that the first line maps the hostname to its actual IP address. So we partly override the behaviour of
            // this method later by pre-pending to /etc/hosts.

            // Find and set entity's own hostname
            Maybe<String> ip = Machines.findSubnetOrPrivateIp(e);
            String key = e.getAttribute(addressSensor);
            if (ip.isAbsentOrNull()) {
                LOG.debug("{} has no IP address, not setting hostname", e);
                continue;
            } else if (!mapping.containsKey(key)) {
                LOG.debug("{} has no hostname mapping, not setting hostname", e);
            } else {
                commands.addAll(BashCommands.setHostname(mapping.get(key)));
            }

            // Add the other entity's details to /etc/hosts
            for (Map.Entry<String, String> entry : mapping.entrySet()) {
                boolean isMyOwnEntry = entry.getKey().equals(key);
                String fqdn = entry.getValue();
                if (fqdn.endsWith("."))
                    fqdn = fqdn.substring(0, fqdn.length() - 1);
                int dotAt = fqdn.indexOf('.');
                String[] values = dotAt > 0
                        ? new String[]{fqdn, fqdn.substring(0, dotAt)}
                        : new String[]{fqdn};

                if (isMyOwnEntry)
                    commands.add(
                            alternatives(
                                    sudo("grep " + ip.get() + " " + "/etc/hosts"),
                                    prependToEtcHosts(ip.get(), values)));
                else
                    commands.add(
                            alternatives(
                                    sudo("grep " + entry.getKey() + " " + "/etc/hosts"),
                                    appendToEtcHosts(entry.getKey(), values)));
            }

            // Ensure that 127.0.0.1 maps to localhost, and nothing else
            String bakFileExtension = "bak" + Identifiers.makeRandomId(4);
            commands.add(
                    alternatives(
                            sudo("grep 127.0.0.1 localhost /ect/hosts" ),
                            sudo("sed -i." + bakFileExtension + " -e \'s/127.0.0.1\\s.*/127.0.0.1 localhost/\' /etc/hosts")));

            loc.execCommands("set hostname and fill /etc/hosts", commands.build());
        }
    }

    public static Map<String, String> gatherIpHostnameMapping(Iterable<? extends Entity> entities) {
        return gatherIpHostnameMapping(entities, Attributes.SUBNET_ADDRESS);
    }

    /**
     * Scans the entities, determines the IP address and hostname for each entity, and returns a map that connects from
     * IP address to fully-qualified domain name.
     *
     * @param entities      entities to search for name/IP information.
     * @param addressSensor the sensor containing the IP address for each entity.
     * @return a map with an entry for each entity, mapping its IP address to its fully-qualified domain name.
     */
    public static Map<String, String> gatherIpHostnameMapping(Iterable<? extends Entity> entities, AttributeSensor<String> addressSensor) {
        Map<String, String> mapping = Maps.newHashMap();
        for (Entity e : entities) {
            Optional<String> name = Optional.fromNullable(e.getAttribute(AmbariNode.FQDN))
                    .or(Optional.fromNullable(Machines.findSubnetOrPublicHostname(e).orNull()));
            Maybe<String> ip = Maybe.fromNullable(e.getAttribute(addressSensor));
            if (name.isPresent() && ip.isPresentAndNonNull()) {
                mapping.put(ip.get(), name.get());
            } else {
                LOG.debug("No hostname or ip found for {}: hostname={}, ip={}",
                        new Object[]{e, name.orNull(), ip.orNull()});
            }
        }
        LOG.debug("Generated host mapping: " + Joiner.on(", ").withKeyValueSeparator("=").join(mapping));
        return mapping;
    }
}
