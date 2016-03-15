/*******************************************************************************
 * Copyright (c) 2015 Netfishers - contact@netfishers.onl
 *******************************************************************************/
package onl.netfishers.blt.bgp;

import java.net.Inet4Address;

import com.googlecode.ipv6.IPv6Address;
import com.googlecode.ipv6.IPv6Network;
import com.googlecode.ipv6.IPv6NetworkMask;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import onl.netfishers.blt.bgp.config.nodes.Capabilities;
import onl.netfishers.blt.bgp.config.nodes.CapabilitiesList;
import onl.netfishers.blt.bgp.config.nodes.ClientConfiguration;
import onl.netfishers.blt.bgp.config.nodes.PeerConfiguration;
import onl.netfishers.blt.bgp.config.nodes.impl.ClientConfigurationImpl;
import onl.netfishers.blt.bgp.config.nodes.impl.PeerConfigurationImpl;
import onl.netfishers.blt.bgp.net.AddressFamily;
import onl.netfishers.blt.bgp.net.SubsequentAddressFamily;
import onl.netfishers.blt.bgp.net.attributes.LinkStateAttribute;
import onl.netfishers.blt.bgp.net.attributes.MultiProtocolNLRI;
import onl.netfishers.blt.bgp.net.attributes.MultiProtocolNLRIInformation;
import onl.netfishers.blt.bgp.net.attributes.PathAttribute;
import onl.netfishers.blt.bgp.net.attributes.PathAttributeType;
import onl.netfishers.blt.bgp.net.attributes.bgplsnlri.BgpLsIPTopologyPrefixNLRI;
import onl.netfishers.blt.bgp.net.attributes.bgplsnlri.BgpLsLinkNLRI;
import onl.netfishers.blt.bgp.net.attributes.bgplsnlri.BgpLsNodeDescriptor;
import onl.netfishers.blt.bgp.net.attributes.bgplsnlri.BgpLsNodeNLRI;
import onl.netfishers.blt.bgp.net.attributes.bgplsnlri.IPPrefix;
import onl.netfishers.blt.bgp.net.capabilities.Capability;
import onl.netfishers.blt.bgp.net.capabilities.MultiProtocolCapability;
import onl.netfishers.blt.bgp.netty.FSMState;
import onl.netfishers.blt.bgp.netty.fsm.BGPv4FSM;
import onl.netfishers.blt.bgp.netty.protocol.update.UpdatePacket;
import onl.netfishers.blt.snmp.SnmpPollingTask;
import onl.netfishers.blt.tasks.Task;
import onl.netfishers.blt.tasks.TaskManager;
import onl.netfishers.blt.topology.TopologyService;
import onl.netfishers.blt.topology.net.Ipv4Route;
import onl.netfishers.blt.topology.net.Ipv4Subnet;
import onl.netfishers.blt.topology.net.Ipv4Subnet.MalformedIpv4SubnetException;
import onl.netfishers.blt.topology.net.Ipv6Route;
import onl.netfishers.blt.topology.net.Link;
import onl.netfishers.blt.topology.net.Network;
import onl.netfishers.blt.topology.net.Router;
import onl.netfishers.blt.topology.net.Router.RouterIdentifier;

import org.quartz.ObjectAlreadyExistsException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BgpService {

	public static class BgpServiceException extends Exception {
		private static final long serialVersionUID = 8955541027177960985L;

		public BgpServiceException() {
			super();
		}

		public BgpServiceException(String message) {
			super(message);
		}
	}

	private static Logger logger = LoggerFactory.getLogger(BgpService.class);
	private static Logger pfxLogger = LoggerFactory.getLogger("PFX");

	private static HashMap<String, Object> instanceMap = new HashMap<String, Object>();
	private static List<BGPv4FSM> fsmList = new ArrayList<BGPv4FSM>();
	

	public static void init() {
		// Use the global Quartz scheduler (owned by TaskManager)
		addInstance(Scheduler.class.getName(), TaskManager.getScheduler());
	}

	public static synchronized Object getInstance(String className) {
		Object singleton = instanceMap.get(className);
		if (singleton != null) {
			return singleton;
		}
		try {
			singleton = Class.forName(className).newInstance();
			logger.info("Created singleton {}.", singleton);
		}
		catch (ClassNotFoundException e) {
			logger.error("Couldn't find class {}.", className);
		}
		catch (InstantiationException e) {
			logger.error("Couldn't instantiate an object of type {}.", className);
		}
		catch (IllegalAccessException e) {
			logger.error("Couldn't access class {}.", className);
		}
		instanceMap.put(className, singleton);
		return singleton;
	}

	public static synchronized void addInstance(String className,
	    Object classObject) {
		Object singleton = instanceMap.get(className);
		if (singleton != null) {
			logger.error("Cannot add new instance for {}, instance already exists",
			    className);
		}
		instanceMap.put(className, classObject);
	}

	public static synchronized void deleteInstance(String className) {
		Object singleton = instanceMap.get(className);
		if (singleton != null) {
			instanceMap.remove(singleton);
		}
	}

	public static void startSession(InetAddress peerAddress, int bgpAs,
	    String name) throws BgpServiceException {
		{
			BGPv4FSM fsm = lookupFSM(peerAddress);
			if (fsm != null) {
				logger.warn("The BGP session to {} is already started.", peerAddress);
				return;
			}
		}
		
		ClientConfiguration clientConfig = new ClientConfigurationImpl(peerAddress);
		Capabilities capabilities = new CapabilitiesList(
		    new Capability[] { new MultiProtocolCapability(AddressFamily.BGP_LS,
		        SubsequentAddressFamily.NLRI_BGP_LS) });
		PeerConfiguration peer;
		try {
			peer = new PeerConfigurationImpl(name, clientConfig, bgpAs, bgpAs, 1, 16);
			peer.setCapabilities(capabilities);
			peer.setHoldTime(30);
			peer.setIdleHoldTime(15);
			peer.setDelayOpenTime(15);
			peer.setConnectRetryTime(30);
			peer.setAutomaticStartInterval(5);
			BGPv4FSM fsm = new BGPv4FSM();
			registerFSM(fsm);
			fsm.configure(peer);
			fsm.startFSMManual();
		}
		catch (Exception e) {
			logger.error("Can't configure and start the BGP session.", e);
			throw new BgpServiceException("Unable to configure and start the BGP session");
		}
	}

	public static void stopSession(InetAddress peerAddress) {
		BGPv4FSM fsm = lookupFSM(peerAddress);
		if (fsm != null) {
			fsm.stopFSM();
			unregisterFSM(fsm);
			fsm.destroyFSM();
		}

	}

	public static void processUpdate(BGPv4FSM fsm, UpdatePacket message) {
		TopologyService topologyService = TopologyService.getService();
		synchronized (topologyService) {
			Inet4Address source = (Inet4Address) fsm.getRemotePeerAddress()
			    .getAddress();
			Network network = null;
			boolean toSave = false;
			for (Network availableNetwork : topologyService.getNetworks()) {
				if (availableNetwork.getBgpPeer().getInetAddress().equals(source)) {
					network = availableNetwork;
					break;
				}
			}
			if (network == null) {
				logger.warn("No network found for BGP update from peer {}.", source);
				return;
			}
			LinkStateAttribute lsAttribute = null;
			for (PathAttribute attribute : message.getPathAttributes()) {
				if (attribute instanceof LinkStateAttribute) {
					lsAttribute = (LinkStateAttribute) attribute;
				}
			}
			
			
			for (PathAttribute attribute : message.getPathAttributes()) {
				if (attribute instanceof MultiProtocolNLRI) {
					MultiProtocolNLRI mpNlriAttribute = (MultiProtocolNLRI) attribute;
					if (mpNlriAttribute.getAddressFamily() != AddressFamily.BGP_LS
					    || mpNlriAttribute.getSubsequentAddressFamily() != SubsequentAddressFamily.NLRI_BGP_LS) {
						logger.debug("Ignoring NLRI AFI {}, SAFI {}.",
						    mpNlriAttribute.getAddressFamily(),
						    mpNlriAttribute.getSubsequentAddressFamily());
						continue;
					}
					mpNlriAttribute.getPathAttributeType();
					for (MultiProtocolNLRIInformation nlri : mpNlriAttribute.getNlris()) {
						
						if (nlri instanceof BgpLsNodeNLRI) {
							BgpLsNodeNLRI nodeNlri = (BgpLsNodeNLRI) nlri;
							BgpLsNodeDescriptor node = nodeNlri.getLocalNodeDescriptor();
							
							try {
								RouterIdentifier routerId = new RouterIdentifier(
									node.getIgpRouterId(), node.getAutonomousSystem(),
									node.getOspfAreaId(), node.getBgpLsIdentifier());
								
								Router router = network.findOrAddRouter(routerId);
								
								if (lsAttribute != null) {
									router.clearIsisAreas();
									for (byte[] isisAreaIdentifier : lsAttribute.getIsisAreaIdentifiers()) {
										if (isisAreaIdentifier != null && isisAreaIdentifier.length > 0) {
											router.addIsisArea(isisAreaIdentifier);
										}
									}
								}
															
								router.setLost(mpNlriAttribute.getPathAttributeType() == PathAttributeType.MULTI_PROTOCOL_UNREACHABLE);
								
								if (router.getRouterId().getData().length == BgpLsNodeDescriptor.IGPROUTERID_ISISISONODEID_LENGTH) {
									try {
										router.setName(lsAttribute.getNodeName());
									}	
									catch (Exception e) {
											logger.warn("NodeName of router: "
													+router.getRouterId().toString()+" cannot been found in LS Attribute");
									}
								}
								
								router.setNeedTeRefresh(true);
								toSave = true;
							}
							catch (Exception e) {
								logger.warn("Invalid node");
							}
							
						}
						else if (nlri instanceof BgpLsLinkNLRI) {
							BgpLsLinkNLRI linkNlri = (BgpLsLinkNLRI) nlri;
							BgpLsNodeDescriptor localNode = linkNlri.getLocalNodeDescriptors();
							BgpLsNodeDescriptor remoteNode = linkNlri.getRemoteNodeDescriptors();

							Link link = null ;
							RouterIdentifier localId = null ;
							RouterIdentifier remoteId = null ;
							
							try {
								localId = new RouterIdentifier (
								    localNode.getIgpRouterId(),
								    localNode.getAutonomousSystem(), 
								    localNode.getOspfAreaId(),
								    localNode.getBgpLsIdentifier());
								remoteId = new RouterIdentifier (
								    remoteNode.getIgpRouterId(),
								    remoteNode.getAutonomousSystem(), 
								    remoteNode.getOspfAreaId(),
								    remoteNode.getBgpLsIdentifier());
								link = new Link(localId, remoteId, 
									new Ipv4Subnet((Inet4Address) linkNlri.getLinkDescriptors()
										.getIPv4InterfaceAddress(), 32), 
										new Ipv4Subnet((Inet4Address) linkNlri.getLinkDescriptors()
										.getIPv4NeighborAddress(), 32),
										linkNlri.getProtocolId());
							}	
							catch (Exception e) {
								logger.info("no IP address attached to this link:"
									+" localId: "+localId.getIdentifier().toString()
									+" remoteId: "+remoteId.getIdentifier().toString()
									+" protocolId: "+linkNlri.getProtocolId());
								try {
									link = new Link(localId, remoteId,new Ipv4Subnet(0,32),new Ipv4Subnet(0,32),linkNlri.getProtocolId());
								}
								catch (MalformedIpv4SubnetException err) {
									logger.warn("This link NLRI does not seem to have any valid descriptor or IP:");
									err.printStackTrace();
								}
							}	

							Router localRouter = network.findOrAddRouter(localId);
							localRouter.setNeedTeRefresh(true);
							Router remoteRouter = network.findOrAddRouter(remoteId);
							remoteRouter.setNeedTeRefresh(true);
							toSave = true;
							
							link = network.findOrAddLink(link);
							
							link.setLost(mpNlriAttribute.getPathAttributeType() == PathAttributeType.MULTI_PROTOCOL_UNREACHABLE);
							
							if (lsAttribute != null) {
								if (lsAttribute.isValidAdminGroup()) {
									link.setAdminGroup(lsAttribute.getAdminGroup());
								}
								if (lsAttribute.isValidMaxLinkBandwidth()) {
									link.setMaxLinkBandwidth(lsAttribute.getMaxLinkBandwidth());
								}
								if (lsAttribute.isValidMaxReservableLinkBandwidth()) {
									link.setMaxReservableLinkBandwidth(lsAttribute.getMaxReservableLinkBandwidth());
								}
								if (lsAttribute.getUnreservedBandwidth() != null) {
									link.setUnreservedBandwidth(lsAttribute.getUnreservedBandwidth().clone());
								}
								if (lsAttribute.isValidTeDefaultMetric()) {
									link.setTeDefaultMetric(lsAttribute.getTeDefaultMetric());
								}
								if (lsAttribute.isValidMetric()) {
									link.setMetric(lsAttribute.getMetric());
								}
								if (lsAttribute.isValidSharedRiskLinkGroups()) {
									link.getSharedRiskLinkGroups().clear();
									link.getSharedRiskLinkGroups().addAll(lsAttribute.getSharedRiskLinkGroups());
								}
							}

						}
							
						
						else if (nlri instanceof BgpLsIPTopologyPrefixNLRI) {
							BgpLsIPTopologyPrefixNLRI ipNlri = (BgpLsIPTopologyPrefixNLRI) nlri;
							BgpLsNodeDescriptor node = ipNlri.getLocalNodeDescriptors();
							long dateTicks = new Date().getTime();
							try {
								RouterIdentifier routerId = new RouterIdentifier(
								    node.getIgpRouterId(), node.getAutonomousSystem(),
								    node.getOspfAreaId(), node.getBgpLsIdentifier());
								Router router = network.findOrAddRouter(routerId);
								for (IPPrefix ipPrefix : ipNlri.getPrefixDescriptor().getPrefixList()) {
									if (ipPrefix.getPrefix().length <= 4) {
										try {
											byte[] prefix = new byte[] { 0, 0, 0, 0 };
											for (int i = 0; i < ipPrefix.getPrefix().length; i++) {
												prefix[i] = ipPrefix.getPrefix()[i];
											}
											long prefixMetric = 0;
											if (lsAttribute != null
											    && lsAttribute.isValidPrefixMetric()) {
												prefixMetric = lsAttribute.getPrefixMetric();
											}
											
											Ipv4Subnet subnet = new Ipv4Subnet(prefix, ipPrefix.getPrefixLength());
											Ipv4Route route = new Ipv4Route(subnet, prefixMetric, null, null, ipNlri.getProtocolId(), dateTicks, false, false);
											if (mpNlriAttribute.getPathAttributeType() == PathAttributeType.MULTI_PROTOCOL_UNREACHABLE) {
												logger.info("Router {} ({}) has just withdrawn {}",router.getRouterId(),
														router.getName(),subnet.toString());
												if (pfxLogger != null) {
													pfxLogger.info("Router {} ({}) has just withdrawn {}",router.getRouterId(), 
														router.getName(),subnet.toString());
												}
												route.setLost(true);
											}
											else {
												if (pfxLogger != null) {
													pfxLogger.info("Router {} ({}) has just announced {}",router.getRouterId(),
														router.getName(),subnet.toString());
												}
												route.setNew(true);
											}
											for (Ipv4Route r : router.getIpv4IgpRoutes()) {
												if (r.equals(route)) {
													router.removeIpv4IgpRoute(r);
													break;
												}
											}
											router.addIpv4IgpRoute(route);
											router.setNeedTeRefresh(true);
										}
										catch (Exception e) {
											logger.error("Unable to parse the IPv4 prefix.", e);
										}
									}
									else if (ipPrefix.getPrefix().length <= 16) {
										try {
											byte[] prefix = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
											for (int i = 0; i < ipPrefix.getPrefix().length; i++) {
												prefix[i] = ipPrefix.getPrefix()[i];
											}
											long prefixMetric = 0;
											if (lsAttribute != null
											    && lsAttribute.isValidPrefixMetric()) {
												prefixMetric = lsAttribute.getPrefixMetric();
											}
											
											IPv6Address address = IPv6Address.fromByteArray(prefix);
											IPv6NetworkMask mask = IPv6NetworkMask.fromPrefixLength(ipPrefix.getPrefixLength());
											IPv6Network subnet = IPv6Network.fromAddressAndMask(address, mask);
											Ipv6Route route = new Ipv6Route(subnet, subnet.toString(), prefixMetric, null, null, null,
													ipNlri.getProtocolId(), dateTicks, false, false);
											if (mpNlriAttribute.getPathAttributeType() == PathAttributeType.MULTI_PROTOCOL_UNREACHABLE) {
												logger.info("Router {} ({}) has just withdrawn {}",router.getRouterId(),
														router.getName(),subnet.toString());
												if (pfxLogger != null) {
													pfxLogger.info("Router {} ({}) has just withdrawn {}",router.getRouterId(), 
														router.getName(),subnet.toString());
												}
												route.setLost(true);
											}
											else {
												if (pfxLogger != null) {
													pfxLogger.info("Router {} ({}) has just announced {}",router.getRouterId(),
														router.getName(),subnet.toString());
												}
												route.setNew(true);
											}
											for (Ipv6Route r : router.getIpv6IgpRoutes()) {
												if (r.equals(route)) {
													router.removeIpv6IgpRoute(r);
													break;
												}
											}
											router.addIpv6IgpRoute(route);
											router.setNeedTeRefresh(true);
										}
										catch (Exception e) {
											logger.error("Unable to parse the IPv6 prefix.", e);
										}
									}
									else {
										logger.warn("Ignoring non IP prefix");
									}
								}
							}
							catch (Exception e) {
								logger.warn("Invalid node");
							}

						}
						else {
							logger
							    .warn("Got a non BGP LS NLRI in the BGP LS NLRI Attribute... ignoring.");
						}
					}
				}
			}
			for (Router router : network.getRouters()) {
				if (router.isVirtual()) {
					if ( router.getRouterId().getData().length == BgpLsNodeDescriptor.IGPROUTERID_ISISPSEUDONODE_LENGTH) {
						router.setName("DIS");
					} else if ( router.getRouterId().getData().length == BgpLsNodeDescriptor.IGPROUTERID_OSPFPSEUDONODE_LENGTH) {
						router.setName("DR");
					}
				}
				else if ( router.isNeedTeRefresh()) {
					if ( router.findSnmpCommunity() != null) {
						Task task = new SnmpPollingTask("Refresh router '"+router.getName()+"' state via SNMP (after BGP update)", router); 
						try {
							task.schedule(1000);
						}
						catch (ObjectAlreadyExistsException e) {
							logger.info("Not scheduling task '{}' since a similar task already exists.", task);
						}
						catch (SchedulerException e) {
							logger.error("Unable to schedule task '{}' following BGP-LS update.", task, e);
						}
					}
				}
			}
			if (toSave) {
				topologyService.writeToDisk();
			}
		}
	}

	private static void registerFSM(BGPv4FSM fsm) {
		synchronized (fsmList) {
			fsmList.add(fsm);
		}
	}

	private static void unregisterFSM(BGPv4FSM fsm) {
		synchronized (fsmList) {
			fsmList.remove(fsm);
		}
	}

	public static BGPv4FSM lookupFSM(InetAddress peerAddress) {
		synchronized (fsmList) {
			for (BGPv4FSM fsm : fsmList) {
				if (fsm.getRemotePeerAddress().getAddress().equals(peerAddress)) {
					return fsm;
				}
			}
		}
		return null;
	}

	public static BGPv4FSM lookupFSM(InetSocketAddress peerAddress) {
		synchronized (fsmList) {
			for (BGPv4FSM fsm : fsmList) {
				if (fsm.getRemotePeerAddress().equals(peerAddress)) {
					return fsm;
				}
			}
		}
		return null;
	}

	public static BGPv4FSM lookupFSM() {
		List<BGPv4FSM> candidates = new LinkedList<BGPv4FSM>();
		BGPv4FSM fsm = null;

		synchronized (fsmList) {
			for (BGPv4FSM fsmEntry : fsmList) {
				if (fsmEntry.getState() == FSMState.Connect) {
					candidates.add(fsmEntry);
				}
			}
		}
		if (candidates.size() > 1) {
			throw new IllegalStateException(
			    "Having more than one FSM instance in pending connect state");
		}
		else if (candidates.size() == 1) {
			fsm = candidates.get(0);
		}
		return fsm;
	}

}
