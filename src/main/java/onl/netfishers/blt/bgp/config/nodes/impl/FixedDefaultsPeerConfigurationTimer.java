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
 * File: org.bgp4.config.nodes.impl.FixedDefaultsPeerConfigurationTimer.java 
 */
/**
 * Copyright 2013 Nitin Bahadur (nitinb@gmail.com)
 * 
 * License: same as above
 * 
 * Modified to run as an independent java application, one that does not
 * require webserver or app server
 */
package onl.netfishers.blt.bgp.config.nodes.impl;

import onl.netfishers.blt.bgp.config.nodes.PeerConfigurationTimer;

/**
 * @author nitinb
 *
 */
public class FixedDefaultsPeerConfigurationTimer implements
		PeerConfigurationTimer {

	/* (non-Javadoc)
	 * @see onl.netfishers.blt.bgp.config.nodes.PeerConfigurationTimer#getHoldTime()
	 */
	@Override
	public int getHoldTime() {
		return 120;
	}

	/* (non-Javadoc)
	 * @see onl.netfishers.blt.bgp.config.nodes.PeerConfigurationTimer#getIdleHoldTime()
	 */
	@Override
	public int getIdleHoldTime() {
		return 30;
	}

	/* (non-Javadoc)
	 * @see onl.netfishers.blt.bgp.config.nodes.PeerConfigurationTimer#getDelayOpenTime()
	 */
	@Override
	public int getDelayOpenTime() {
		return 15;
	}

	/* (non-Javadoc)
	 * @see onl.netfishers.blt.bgp.config.nodes.PeerConfigurationTimer#getConnectRetryTime()
	 */
	@Override
	public int getConnectRetryTime() {
		return 60;
	}

	/* (non-Javadoc)
	 * @see onl.netfishers.blt.bgp.config.nodes.PeerConfigurationTimer#getAutomaticStartInterval()
	 */
	@Override
	public int getAutomaticStartInterval() {
		return 120;
	}

}
