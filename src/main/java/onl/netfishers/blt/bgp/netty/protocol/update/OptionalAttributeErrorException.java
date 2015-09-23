/**
 *  Copyright 2012, 2014 Rainer Bieniek (Rainer.Bieniek@web.de)
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
 * File: onl.netfishers.blt.bgp.netty.protocol.UnrecognizedAttributeException.java 
 */
package onl.netfishers.blt.bgp.netty.protocol.update;

import onl.netfishers.blt.bgp.netty.protocol.NotificationPacket;

import org.jboss.netty.buffer.ChannelBuffer;

/**
 * @author Rainer Bieniek (Rainer.Bieniek@web.de)
 *
 */
public class OptionalAttributeErrorException extends AttributeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8298311237342239339L;

	/**
	 * 
	 */
	public OptionalAttributeErrorException() {
	}

	/**
	 * @param offendingAttribute
	 */
	public OptionalAttributeErrorException(byte[] offendingAttribute) {
		super(offendingAttribute);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 * @param offendingAttribute
	 */
	public OptionalAttributeErrorException(String message,
			byte[] offendingAttribute) {
		super(message, offendingAttribute);
	}

	/**
	 * @param buffer
	 */
	public OptionalAttributeErrorException(ChannelBuffer buffer) {
		super(buffer);
	}

	/**
	 * @param message
	 * @param buffer
	 */
	public OptionalAttributeErrorException(String message, ChannelBuffer buffer) {
		super(message, buffer);
	}

	/* (non-Javadoc)
	 * @see onl.netfishers.blt.bgp.netty.protocol.ProtocolPacketException#toNotificationPacket()
	 */
	@Override
	public NotificationPacket toNotificationPacket() {
		return new OptionalAttributeErrorNotificationPacket(getOffendingAttribute());
	}

}
