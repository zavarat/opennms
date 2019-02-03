/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2019 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2019 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.features.apilayer.config;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.opennms.integration.api.v1.config.poller.PollerConfigExtension;
import org.opennms.netmgt.config.PollerConfigFactory;
import org.opennms.netmgt.config.poller.Monitor;
import org.opennms.netmgt.config.poller.Package;
import org.opennms.netmgt.config.poller.PollerConfiguration;
import org.opennms.netmgt.config.poller.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PollerConfigExtensionManager extends ConfigExtensionManager<PollerConfigExtension, PollerConfiguration> {

    private static final Logger LOG = LoggerFactory.getLogger(PollerConfigExtensionManager.class);

    public PollerConfigExtensionManager() {
        super(PollerConfiguration.class, new PollerConfiguration());
    }

    @Override
    protected PollerConfiguration getConfigForExtensions(Set<PollerConfigExtension> extensions) {
        // Rebuild the poller configuration
        final PollerConfiguration pollerConfiguration = new PollerConfiguration();

        // Map the monitors, first one for a given name wins
        final Map<String, Monitor> monitorsByName = new LinkedHashMap<>();
        extensions.stream()
                .flatMap(e -> e.getMonitors().stream())
                .forEach(m -> {
                    if (monitorsByName.containsKey(m.getServiceName())) {
                        // pass
                        return;
                    }
                    final Monitor monitor = new Monitor();
                    monitor.setService(m.getServiceName());
                    monitor.setClassName(m.getClassName());
                    monitorsByName.put(m.getServiceName(), monitor);
                });
        monitorsByName.values().forEach(pollerConfiguration::addMonitor);

        // Merge all of the packages
        final Map<String, Package> packagesByName = new LinkedHashMap<>();
        extensions.stream()
                .flatMap(e -> e.getPackages().stream())
                .forEach(p -> {
                    final Package pkg = packagesByName.computeIfAbsent(p.getName(), (name) -> {
                        final Package newPkg = new Package();
                        newPkg.setName(name);
                        return newPkg;
                    });

                    // Add all of the services, if one already exists with the given name, skip it
                    p.getServices().forEach(svc -> {
                        if (pkg.getService(svc.getName()) != null) {
                            return;
                        }
                        final Service service = new Service();
                        service.setName(svc.getName());
                        service.setInterval(svc.getInterval());
                        svc.getParameters().forEach(service::addParameter);
                        pkg.addService(service);
                    });
                });
        packagesByName.values().forEach(pollerConfiguration::addPackage);

        return pollerConfiguration;
    }

    @Override
    protected void triggerReload() {
        try {
            PollerConfigFactory.reload();
        } catch (IOException e) {
            LOG.warn("Automatic reload trigger failed. Changes in extension may not be immediately propagated.", e);
        }
    }
}
