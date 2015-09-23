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
 * File: org.bgp4.config.global.BgpServerConfigurationEvent.java 
 */
package onl.netfishers.blt.bgp.config.global;

import onl.netfishers.blt.bgp.config.nodes.PeerConfiguration;

/**
 * @author Rainer Bieniek (Rainer.Bieniek@web.de)
 *
 */
public class PeerConfigurationEvent extends TypedEvent {

	private PeerConfiguration former;
	private PeerConfiguration current;
	
	public PeerConfigurationEvent(EventType type, PeerConfiguration former, PeerConfiguration current) {
		super(type);
		
		this.former = former;
		this.current = current;
	}

	/**
	 * @return the configuration
	 */
	public PeerConfiguration getFormer() {
		return former;
	}

	/**
	 * @return the current
	 */
	public PeerConfiguration getCurrent() {
		return current;
	}
}
