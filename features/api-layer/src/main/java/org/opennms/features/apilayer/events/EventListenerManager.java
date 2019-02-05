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

package org.opennms.features.apilayer.events;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.opennms.features.apilayer.utils.ModelMappers;
import org.opennms.integration.api.v1.events.EventHandler;
import org.opennms.integration.api.v1.events.EventListener;
import org.opennms.integration.api.v1.model.InMemoryEvent;
import org.opennms.netmgt.events.api.EventSubscriptionService;
import org.opennms.netmgt.xml.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;

import com.google.common.base.Strings;

public class EventListenerManager {

    private static final Logger LOG = LoggerFactory.getLogger(EventListenerManager.class);

    private final EventSubscriptionService eventSubscriptionService;
    private final Map<EventListener, EventListenerAdapter> eventListenerToAdapterMap = new LinkedHashMap<>();

    public EventListenerManager(EventSubscriptionService eventSubscriptionService) {
        this.eventSubscriptionService = Objects.requireNonNull(eventSubscriptionService);
    }

    @SuppressWarnings({ "rawtypes" })
    public synchronized void onBind(EventListener extension, Map properties) {
        LOG.debug("bind called with {}: {}", extension, properties);
        if (extension != null && !eventListenerToAdapterMap.containsKey(extension)) {
            final EventListenerAdapter adapter;
            try {
                adapter = new EventListenerAdapter(extension);
            } catch (IllegalStateException e) {
                LOG.warn("Skipping {}", extension, e);
                return;
            }

            final Set<String> ueis = adapter.getUEIs();
            if (ueis.contains(EventHandler.ALL_UEIS)) {
                eventSubscriptionService.addEventListener(adapter);
            } else {
                eventSubscriptionService.addEventListener(adapter, ueis);
            }

            eventListenerToAdapterMap.put(extension,adapter);
        }
    }

    @SuppressWarnings({ "rawtypes" })
    public synchronized void onUnbind(EventListener extension, Map properties) {
        LOG.debug("unbind called with {}: {}", extension, properties);
        final EventListenerAdapter adapter = eventListenerToAdapterMap.remove(extension);
        if (adapter != null) {
            final Set<String> ueis = adapter.getUEIs();
            if (ueis.contains(EventHandler.ALL_UEIS)) {
                eventSubscriptionService.removeEventListener(adapter);
            } else {
                eventSubscriptionService.removeEventListener(adapter, ueis);
            }
        }
    }

    private static class EventListenerAdapter implements org.opennms.netmgt.events.api.EventListener, org.opennms.netmgt.events.api.ThreadAwareEventListener {
        private final EventListener delegate;
        private final Map<String, Method> ueiToHandlerMap;
        private final Method catchAllEventHandler;

        public EventListenerAdapter(EventListener delegate) {
            this.delegate = Objects.requireNonNull(delegate);
            ueiToHandlerMap = generateUeiToHandlerMap();
            catchAllEventHandler = ueiToHandlerMap.get(EventHandler.ALL_UEIS);
        }

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public int getNumThreads() {
            return delegate.getNumThreads();
        }

        @Override
        public void onEvent(Event event) {
            if (event.getUei() == null) {
                return;
            }

            Method m = ueiToHandlerMap.get(event.getUei());
            if (m == null) {
                m = catchAllEventHandler;
                if (m == null) {
                    throw new IllegalArgumentException(String.format("Received an event with UEI: '%s' for which we have no handler!", event.getUei()));
                }
            }

            try {
                m.invoke(delegate, ModelMappers.toEvent(event));
            } catch (IllegalAccessException|InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        private Set<String> getUEIs() {
            return new HashSet<>(ueiToHandlerMap.keySet());
        }

        /**
         * Similar to {@link org.opennms.netmgt.events.api.AnnotationBasedEventListenerAdapter#populateUeiToHandlerMap}
         */
        private Map<String, Method> generateUeiToHandlerMap() {
            final Map<String, Method> ueiToHandlerMap = new HashMap<>();
            final Method[] methods = delegate.getClass().getMethods();
            for(Method method : methods) {
                final EventHandler handlerInfo = AnnotationUtils.findAnnotation(method, EventHandler.class);
                if (handlerInfo != null) {
                    final String singleUei = handlerInfo.uei();
                    if (!Strings.isNullOrEmpty(singleUei)) {
                        validateMethodAsEventHandler(method);
                        if (ueiToHandlerMap.containsKey(singleUei)) {
                            throw new IllegalStateException("Cannot define method "+method+" as a handler for event "+singleUei+" since "+ueiToHandlerMap.get(singleUei)+" is already defined as a handler");
                        }
                        ueiToHandlerMap.put(singleUei, method);
                    }

                    final String[] ueis = handlerInfo.ueis();
                    if (ueis.length > 0) {
                        validateMethodAsEventHandler(method);
                        for (String uei : ueis) {
                            if (ueiToHandlerMap.containsKey(singleUei)) {
                                throw new IllegalStateException("Cannot define method "+method+" as a handler for event "+singleUei+" since "+ueiToHandlerMap.get(singleUei)+" is already defined as a handler");
                            }
                            ueiToHandlerMap.put(uei, method);
                        }
                    }
                }
            }

            if (ueiToHandlerMap.isEmpty()) {
                throw new IllegalStateException("annotatedListener must have public EventHandler annotated methods");
            }
            return ueiToHandlerMap;
        }

        private static void validateMethodAsEventHandler(Method method) {
            if (method.getParameterTypes().length != 1) {
                throw new IllegalStateException("Invalid number of paremeters for method "+method+". EventHandler methods must take a single event argument");
            }
            if (!method.getParameterTypes()[0].isAssignableFrom(InMemoryEvent.class)) {
                throw new IllegalStateException("Parameter of incorrect type for method "+method+". EventHandler methods must take a single event argument");
            }
        }
    }
}
