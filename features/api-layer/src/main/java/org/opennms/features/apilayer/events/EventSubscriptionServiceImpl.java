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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.opennms.features.apilayer.utils.ModelMappers;
import org.opennms.integration.api.v1.events.EventListener;
import org.opennms.integration.api.v1.events.EventSubscriptionService;
import org.opennms.netmgt.xml.event.Event;

public class EventSubscriptionServiceImpl implements EventSubscriptionService {

    private final org.opennms.netmgt.events.api.EventSubscriptionService delegate;
    // FIXME: How to clean up entries in the map? Maybe we should return a future on registration instead
    private final Map<EventListener, EventListenerAdapter> eventListenerToAdapterMap = new LinkedHashMap<>();

    public EventSubscriptionServiceImpl(org.opennms.netmgt.events.api.EventSubscriptionService delegate) {
        this.delegate = delegate;
    }

    @Override
    public void addEventListener(EventListener listener) {
        delegate.addEventListener(getOrCreateAdapter(listener));
    }

    @Override
    public void addEventListener(EventListener listener, Collection<String> ueis) {
        delegate.addEventListener(getOrCreateAdapter(listener), ueis);
    }

    @Override
    public void addEventListener(EventListener listener, String uei) {
        delegate.addEventListener(getOrCreateAdapter(listener), uei);
    }

    @Override
    public void removeEventListener(EventListener listener) {
        final EventListenerAdapter adapter = eventListenerToAdapterMap.remove(listener);
        if (adapter != null) {
            delegate.removeEventListener(adapter);
        }
    }

    @Override
    public void removeEventListener(EventListener listener, Collection<String> ueis) {
        final EventListenerAdapter adapter = eventListenerToAdapterMap.remove(listener);
        if (adapter != null) {
            delegate.removeEventListener(adapter, ueis);
        }
    }

    @Override
    public void removeEventListener(EventListener listener, String uei) {
        final EventListenerAdapter adapter = eventListenerToAdapterMap.remove(listener);
        if (adapter != null) {
            delegate.removeEventListener(adapter, uei);
        }
    }

    @Override
    public boolean hasEventListener(String uei) {
        return false;
    }

    private EventListenerAdapter getOrCreateAdapter(EventListener listener) {
        return eventListenerToAdapterMap.computeIfAbsent(listener, EventListenerAdapter::new);
    }

    private static class EventListenerAdapter implements org.opennms.netmgt.events.api.EventListener, org.opennms.netmgt.events.api.ThreadAwareEventListener {
        private final EventListener delegate;

        EventListenerAdapter(EventListener delegate) {
            this.delegate = Objects.requireNonNull(delegate);
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
            if (event == null || event.getUei() == null) {
                return;
            }
            delegate.onEvent(ModelMappers.toEvent(event));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EventListenerAdapter that = (EventListenerAdapter) o;
            return Objects.equals(delegate, that.delegate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(delegate);
        }

        @Override
        public String toString() {
            return "EventListenerAdapter{" +
                    "delegate=" + delegate +
                    '}';
        }
    }
}
