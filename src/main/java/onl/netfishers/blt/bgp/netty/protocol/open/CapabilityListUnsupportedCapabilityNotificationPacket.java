/**
 *  Copyright 2012 Rainer Bieniek (Rainer.Bieniek@web.de)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * 
 * File: onl.netfishers.blt.bgp.netty.protocol.open.CapabilityListUnsupportedCapabilityNotificationPacket.java 
 */
package onl.netfishers.blt.bgp.netty.protocol.open;

import java.util.List;

import onl.netfishers.blt.bgp.net.capabilities.Capability;

import org.jboss.netty.buffer.ChannelBuffer;

/**
 * @author Rainer Bieniek (Rainer.Bieniek@web.de)
 *
 */
public class CapabilityListUnsupportedCapabilityNotificationPacket extends UnsupportedCapabilityNotificationPacket {

	private List<Capability> capabilities;

	public CapabilityListUnsupportedCapabilityNotificationPacket(List<Capability> capabilities) {
		this.capabilities = capabilities;
	}
	
	/* (non-Javadoc)
	 * @see onl.netfishers.blt.bgp.netty.protocol.NotificationPacket#encodeAdditionalPayload()
	 */
	@Override
	protected ChannelBuffer encodeAdditionalPayload() {
		return CapabilityCodec.encodeCapabilities(capabilities);
	}

	/**
	 * @return the capabilities
	 */
	public List<Capability> getCapabilities() {
		return capabilities;
	}
}
