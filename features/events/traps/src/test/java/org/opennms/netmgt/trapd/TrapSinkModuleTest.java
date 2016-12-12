/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2016-2016 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2016 The OpenNMS Group, Inc.
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

package org.opennms.netmgt.trapd;

import org.junit.Assert;
import org.junit.Test;
import org.opennms.core.ipc.sink.api.Message;
import org.opennms.core.ipc.sink.api.SinkModule;
import org.opennms.netmgt.config.TrapdConfig;

public class TrapSinkModuleTest {

    private class MockModule implements SinkModule<Message> {
        @Override
        public String getId() {
            return "id";
        }

        @Override
        public int getNumConsumerThreads() {
            return 0;
        }

        @Override
        public String marshal(Message message) {
            return "";
        }

        @Override
        public Message unmarshal(String message) {
            return null;
        }
    }

    @Test
    public void testEqualsAndHashCode() throws Exception {
        TrapdConfig config = new TrapdConfigBean();
        final TrapSinkModule module = new TrapSinkModule(config);
        Assert.assertEquals(module, module);
        Assert.assertEquals(module.hashCode(), module.hashCode());

        final TrapSinkModule other = new TrapSinkModule(config);
        Assert.assertEquals(module, other);
        Assert.assertEquals(module.hashCode(), other.hashCode());

        final SinkModule otherImpl = new MockModule();
        Assert.assertNotEquals(module, otherImpl);
        Assert.assertNotEquals(module.hashCode(), otherImpl.hashCode());
    }
}
