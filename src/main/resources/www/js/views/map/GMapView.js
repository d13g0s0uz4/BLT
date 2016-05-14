/*******************************************************************************
 * Copyright (c) 2015 Netfishers - contact@netfishers.onl
 *******************************************************************************/
define([
	'jquery',
	'underscore',
	'backbone',
	'models/network/DijkstraPathCollection',
	'models/network/NetworkModel',
	'models/network/RouterCollection',
	'models/network/LinkCollection',
	'models/network/InterfaceCollection',
	'models/network/Ipv4IgpRouteCollection',
	'models/network/Ipv6IgpRouteCollection',
	'text!templates/gmap/gmap.html',
	'text!templates/gmap/mapRouter.html',
	'text!templates/gmap/bubbleRouter.html',
	'text!templates/gmap/infoboxLink.html',
	'text!templates/map/link.html',
	'text!templates/gmap/bubbleInterfaces.html',
	'text!templates/gmap/bubbleIpv4IgpRoutes.html',
	'text!templates/gmap/bubbleIpv6IgpRoutes.html',
	'text!templates/gmap/bubbleInterfaceRow.html',
	'text!templates/gmap/bubbleIpv4IgpRouteRow.html',
	'text!templates/gmap/bubbleIpv6IgpRouteRow.html',
	'bootstrap',
	'infobox',
	'infobubble'
], function($, _, Backbone, DijkstraPathCollection, NetworkModel, RouterCollection, 
		LinkCollection, InterfaceCollection, Ipv4IgpRouteCollection, Ipv6IgpRouteCollection,  
		gMapTemplate, routerTemplate, bubbleRouterTemplate, linkTemplate, bubbleLinkTemplate, 
		bubbleInterfacesTemplate, bubbleIpv4IgpRoutesTemplate, bubbleIpv6IgpRoutesTemplate, 
		bubbleInterfaceRowTemplate, bubbleIpv4IgpRouteRowTemplate, bubbleIpv6IgpRouteRowTemplate) {
	
	return Backbone.View.extend({

		el: "#page",

		template: _.template(gMapTemplate),
		routerTemplate: _.template(routerTemplate),
		bubbleRouterTemplate: _.template(bubbleRouterTemplate),
		linkTemplate: _.template(linkTemplate),
		bubbleLinkTemplate: _.template(bubbleLinkTemplate),
		bubbleInterfacesTemplate: _.template(bubbleInterfacesTemplate),
		bubbleIpv4IgpRoutesTemplate: _.template(bubbleIpv4IgpRoutesTemplate),
		bubbleIpv6IgpRoutesTemplate: _.template(bubbleIpv6IgpRoutesTemplate),
		bubbleInterfaceRowTemplate: _.template(bubbleInterfaceRowTemplate),
		bubbleIpv4IgpRouteRowTemplate: _.template(bubbleIpv4IgpRouteRowTemplate),
		bubbleIpv6IgpRouteRowTemplate: _.template(bubbleIpv6IgpRouteRowTemplate),
		
		defaultBoxOptions: {
			disableAutoPan: true,
			maxWidth: 0,
			pixelOffset: new google.maps.Size(0, -20),
			zIndex: null,
			boxStyle: { 
				background: 'rgb(255,255,255)',
				color: 'rgb(10,10,10)',
				'font-size': '13px',
				opacity: 0.9,
				width: "auto"
			},
			closeBoxMargin: "10px 2px 2px 2px",
			closeBoxURL: "",
			infoBoxClearance: new google.maps.Size(1, 1),
			isHidden: false,
			pane: "floatPane",
			enableEventPropagation: false
		},
		
		defaultLinkBoxOptions: {
			disableAutoPan: true,
			maxWidth: 0,
			zIndex: null,
			boxStyle: { 
                background: "#666666",
                color: 'rgb(255,255,255)',
                'font-size': '10px',
                opacity: 0.9,
                width: "auto"
            },
			closeBoxMargin: "10px 2px 2px 2px",
			closeBoxURL: "",
			infoBoxClearance: new google.maps.Size(1, 1),
			isHidden: false,
			pane: "floatPane",
			enableEventPropagation: false
		},
		
		defaultRouterBubbleOptions: {
			maxHeight: 170,
			shadowStyle: 1,
			padding: 5,
			backgroundColor: 'rgb(220,220,220)',
			borderRadius: 2,
			arrowSize: 10,
			borderWidth: 1,
			borderColor: '#2c2c2c',
			disableAutoPan: true,
			hideCloseButton: false,
			closeSrc: 'img/fileclose12.png',
			arrowPosition: 20,
			arrowStyle: 2
		},
      	
      	defaultLinkBubbleOptions: {
			maxHeight: 170,
			shadowStyle: 1,
			padding: 5,
			borderRadius: 2,
			arrowSize: 10,
			borderWidth: 1,
			borderColor: '#2c2c2c',
			disableAutoPan: true,
			hideCloseButton: false,
			closeSrc: 'img/fileclose12.png',
			arrowPosition: 20,
			arrowStyle: 2
		},
		
		defaultRouterIcon: {
			url: '../../../img/router_grey.svg',
			scaledSize: new google.maps.Size(30, 30),
			origin: new google.maps.Point(0, 0),
			anchor: new google.maps.Point(20, 20),
			animation: google.maps.Animation.DROP
		},
		newRouterIcon: {
			url: '../../../img/router_green.svg',
			scaledSize: new google.maps.Size(46, 46),
			origin: new google.maps.Point(0, 0),
			anchor: new google.maps.Point(20, 20),
			animation: google.maps.Animation.DROP
		},
		withdrawRouterIcon: {
			url: '../../../img/router_orange.svg',
			scaledSize: new google.maps.Size(46, 46),
			origin: new google.maps.Point(0, 0),
			anchor: new google.maps.Point(20, 20),
			animation: google.maps.Animation.DROP
		},
		lostRouterIcon: {
			url: '../../../img/router_red.svg',
			scaledSize: new google.maps.Size(46, 46),
			origin: new google.maps.Point(0, 0),
			anchor: new google.maps.Point(20, 20),
			animation: google.maps.Animation.DROP
		},
		
		initialize: function(){
			var that = this ;
			
			this.network = new NetworkModel({
				id: this.id
			});
			this.routers = new RouterCollection([], {
				network: this.id
			});
			this.routers.on("add", this.onAddedRouter, this);
			this.routers.on("change", this.onChangedRouter, this);
			this.routers.on("remove", this.onDestroyedRouter, this);
			this.links = new LinkCollection([], {
				network: this.id
			});
			this.links.on("add", this.onAddedLink, this);
			this.links.on("change", this.onChangedLink, this);
			this.links.on("remove", this.onDestroyedLink, this);
			
			Backbone.history.on("route", function (route, router) {
				if (that.autoRefreshInt !== false && that.autoRefreshInt != null) {
					that.autoRefresh();
				}
			});
			
			this.bounds = new google.maps.LatLngBounds();
			
			this.markersArray = [];
			this.linksArray = [];
			
			this.network.fetch().done(function() {
				that.render();
				that.refresh();
			});
		},
		
		refresh: function() {
			var that = this;
			that.routers.fetch().done(function() {
				that.links.fetch().done(function() {
					that.$("#gmap-toolbar #refresh").prop('disabled', false);
					that.routers.each(function(router) {
						marker = that.markersArray[router.get('id')];
						if (marker.bubbleStillOpen) {
                          	google.maps.event.trigger(marker, 'click');
                        }
						if (router.get("justAnnouncedAPrefix")) {
							marker.setIcon = this.newRouterIcon;
                          	marker.setAnimation(google.maps.Animation.DROP);
						}
						else if (router.get("justWithdrawnAPrefix")) {
							marker.setIcon = this.withdrawRouterIcon;
                          	marker.setAnimation(google.maps.Animation.DROP);
						}
						else if (router.get("lost")) {
							marker.setIcon = this.lostRouterIcon;
						}
						else {
							marker.setIcon = this.defaultRouterIcon;
                          	marker.setAnimation(google.maps.Animation.DROP);
						}
					});
					that.links.each(function(link) {
						line = that.linksArray[link.get('id')];
						if (line.bubbleStillOpen) {
                          	google.maps.event.trigger(line, 'click',line.bubblePreviousPosition);
                        }
					});
				});
			});
		},
		
		autoRefresh: function() {
			var that = this;
			if (this.autoRefreshInt === false || this.autoRefreshInt == null) {
				this.autoRefreshInt = setInterval(function() {
					that.$("#gmap-toolbar #refresh").prop('disabled', true);
					that.refresh();
				}, 10000 * 2);
			} else {
				clearInterval(this.autoRefreshInt);
				this.autoRefreshInt = false;
			}
		},
		
		onAddedRouter: function(router, routers) {
			var that = this;
			var position = new google.maps.LatLng(router.get('latitude'), router.get('longitude'));
			this.bounds.extend(position);
			this.map.fitBounds(this.bounds);
			
			var marker = new google.maps.Marker({
				position: position,
				map: this.map,
				animation: google.maps.Animation.DROP,
				icon: this.defaultRouterIcon,
				routerId: router.get('id'),
				bubbleStillOpen: false
			});
			
			if (router.get("justAnnouncedAPrefix")) {
				marker.icon = this.newRouterIcon;
			}
			if (router.get("justWithdrawnAPrefix")) {
	           	marker.icon = this.withdrawRouterIcon;
			}
			
			var bubble = new InfoBubble(that.defaultRouterBubbleOptions);
			bubble.addListener('closeclick',function(){
					marker.bubbleStillOpen = false;
			});
			marker.bubbleId = bubble;
          	this.markersArray[router.get('id')] = marker;
			
			marker.addListener('click', function(e) {
				var marker = this;
				var router = that.routers.get(this.routerId);
				if (!router) return;
				this.bubbleStillOpen = true;
				var ipv4IgpRoutes = new Ipv4IgpRouteCollection([], {
					network: that.network.get('id'),
					router: router.get('id')
				});
				var ipv6IgpRoutes = new Ipv6IgpRouteCollection([], {
					network: that.network.get('id'),
					router: router.get('id')
				});
				
				var updateBubble = false;
				
				ipv4IgpRoutes.fetch().done(function() {
					ipv6IgpRoutes.fetch().done(function() {
					
						if (bubble.isOpen(bubble) || bubble.getContent(bubble)) {updateBubble = true;}
						
						if (!updateBubble) {bubble.addTab("Router", that.bubbleRouterTemplate(router.toJSON()));}
						else {bubble.updateTab(0,"Router", that.bubbleRouterTemplate(router.toJSON()));}
						
						var $interfacesTab = $("<div>").append(that.bubbleInterfacesTemplate(router.toJSON()));
						_.each(router.get("ipv4Interfaces"), function(routerInterface) {
							$interfacesTab.find("tbody").append($(that.bubbleInterfaceRowTemplate(routerInterface)));
						});
						
						if (!updateBubble) {bubble.addTab("Interfaces", $interfacesTab.html());}
						else {bubble.updateTab(1,"Interfaces", $interfacesTab.html());}
						
						var $routes4Tab = $("<div>").append(that.bubbleIpv4IgpRoutesTemplate(router.toJSON()));
						var $routes6Tab = $("<div>").append(that.bubbleIpv6IgpRoutesTemplate(router.toJSON()));
						ipv4IgpRoutes.each(function(route) {
							var now = Date.now();
							var data = route.toJSON();
							
							var ageMonths = typeof(data.date) != 'undefined' ? parseInt(((now - data.date)/(1000*60*60*24*30))%12) : 0;
							var ageDays = typeof(data.date) != 'undefined' ? parseInt(((now - data.date)/(1000*60*60*24))%30) : 0;
							var age = '';
							if (ageMonths > 0) {
								age += ageMonths+"m ";
							}
							if (ageDays > 0) {
								age += ageDays+"d ";
							}
							age +=
								("00"+parseInt(((now - data.date)/(1000*60*60))%24)+":").slice(-3)+
								("00"+parseInt(((now - data.date)/(1000*60))%60)+":").slice(-3)+
								("00"+parseInt((now - data.date)/1000)%60).slice(-2);
							data.age = age;
							
							var rowClass = '';
							if (data.justNew === true) {
								rowClass = 'class=bubbleNew';
							}
							else if (data.justLost === true) {
								rowClass = 'class=bubbleLost';
							}
							data.rowClass = rowClass;
							
							$routes4Tab.find("tbody").append($(that.bubbleIpv4IgpRouteRowTemplate(data)));
						});
						ipv6IgpRoutes.each(function(route) {
							var now = Date.now();
							var data = route.toJSON();
							
							var ageMonths = typeof(data.date) != 'undefined' ? parseInt(((now - data.date)/(1000*60*60*24*30))%12) : 0;
							var ageDays = typeof(data.date) != 'undefined' ? parseInt(((now - data.date)/(1000*60*60*24))%30) : 0;
							var age = '';
							if (ageMonths > 0) {
								age += ageMonths+"m ";
							}
							if (ageDays > 0) {
								age += ageDays+"d ";
							}
							age +=
								("00"+parseInt(((now - data.date)/(1000*60*60))%24)+":").slice(-3)+
								("00"+parseInt(((now - data.date)/(1000*60))%60)+":").slice(-3)+
								("00"+parseInt((now - data.date)/1000)%60).slice(-2);
							data.age = age;
							
							var rowClass = '';
							if (data.justNew === true) {
								rowClass = 'class=bubbleNew';
							}
							else if (data.justLost === true) {
								rowClass = 'class=bubbleLost';
							}
							data.rowClass = rowClass;
							
							$routes6Tab.find("tbody").append($(that.bubbleIpv6IgpRouteRowTemplate(data)));
						});
						if (!updateBubble) {
							bubble.addTab("IPv4 Prefixes", $routes4Tab.html());
							bubble.addTab("IPv6 Prefixes", $routes6Tab.html());
						}
						else {
							bubble.updateTab(2,"IPv4 Prefixes", $routes4Tab.html());
							bubble.updateTab(3,"IPv6 Prefixes", $routes6Tab.html());
						}
						
						bubble.open(that.map, marker);
					});
				});				
				
			});
			marker.addListener('rightclick', function() {
				var router = that.routers.get(this.routerId);
				if (!router) return;
				var dijkstraTree = new DijkstraPathCollection([], {
					network: that.network.get('id'),
					router: router.get('id')
				});
				dijkstraTree.fetch().done(function() {
					var tree = [];
					dijkstraTree.each(function(path) {
						tree[path.get('targetId')] = path.toJSON();
					});
					tree = tree.filter(Boolean)
                  	for (var i=0; i<tree.length; i++){
                  		var entropy = Math.random() - 0.5;
                      	var randColor = 'rgb(' + (Math.floor(Math.random() * 180)) + 
                      		',' + (Math.floor(Math.random() * 180)) + 
                      		',' + (Math.floor(Math.random() * 180)) + ')';
                      	var pathCoordinates = [];
                      	for (var j=0;j<tree[i].vertices.length; j++){
                      		if (j==0 || j==tree[i].vertices.length - 1) {
                      			pathCoordinates.push(new google.maps.LatLng(
                              			tree[i].vertices[j].latitude,
                              			tree[i].vertices[j].longitude)
                              	);
                      		}
                      		else {
                      			pathCoordinates.push(new google.maps.LatLng(
                              			tree[i].vertices[j].latitude + entropy,
                              			tree[i].vertices[j].longitude + entropy)
                              	);
                      		}
                        }
                      	if (that.markersArray[tree[i].targetId].bubbleStillOpen) {
	                      	addDynamicDijkstraPath(pathCoordinates,randColor,4000);
	                      	addDynamicWeightLabel(
	                          google.maps.geometry.spherical.interpolate(
	                            	pathCoordinates[pathCoordinates.length - 2], 
	                            	pathCoordinates[pathCoordinates.length - 1], 0.25),
	                            	randColor,tree[i].weight,4000
	                        );
                          	hideBubble(that.markersArray[tree[i].targetId],4000);
                      	}
                      	if (that.markersArray[router.get('id')].bubbleStillOpen) {
	                      	addDynamicDijkstraPath(pathCoordinates,randColor,8000);
	                      	addDynamicWeightLabel(
	                          google.maps.geometry.spherical.interpolate(
	                            	pathCoordinates[pathCoordinates.length - 2], 
	                            	pathCoordinates[pathCoordinates.length - 1], 0.25),
	                            	randColor,tree[i].weight,8000
	                        );
                          	hideBubble(that.markersArray[router.get('id')],8000);
                      	}
   	 				}
					function addDynamicDijkstraPath(pathCoordinates,pathColor,timeout) {
                  		var dijkstraPath = new google.maps.Polyline({
							path: pathCoordinates,
							strokeColor: pathColor,
							strokeOpacity: 1,
							strokeWeight: 3,
                      		icons: [{
            					icon: {path: google.maps.SymbolPath.FORWARD_CLOSED_ARROW},
            					offset: '95%'
        					}],
							map: that.map
                  		});
                  	    setTimeout(function () {
                  	    	dijkstraPath.setMap(null);
                  	        delete dijkstraPath;
                  	    }, timeout);
                  	    return dijkstraPath;
                  	}
					function addDynamicWeightLabel(coordinates,pathColor,weight,timeout) {
                        var myOptions = {
							disableAutoPan: true,
							maxWidth: 0,
							boxStyle: { 
                				background: pathColor,
                				color: 'rgb(255,255,255)',
                				'font-size': '13px',
               					 opacity: 0.9,
               					 width: "auto"
            				},
							closeBoxMargin: "10px 2px 2px 2px",
							closeBoxURL: "",
							infoBoxClearance: new google.maps.Size(1, 1),
							isHidden: false,
							enableEventPropagation: false,
                            content: weight,
                            position: coordinates,
                        };
                        
                        var weightLabel = new InfoBox(myOptions);
                        weightLabel.open(that.map);
	
                      	setTimeout(function () {
                      		weightLabel.setMap(null);
                  	        delete weightLabel;
                  	    }, timeout);
                  	    return weightLabel;
					}
					function hideBubble(marker,timeout) {
						marker.bubbleId.close();
                      	setTimeout(function () {
                      		google.maps.event.trigger(marker, 'click');
                  	    }, timeout);
                    }
				});
			});
			marker.addListener('mouseover', function() {
				var router = that.routers.get(this.routerId);
				if (!router) return;
				this.box = new InfoBox(that.defaultBoxOptions);
				this.box.setContent(that.routerTemplate(router.toJSON()));
				this.box.open(that.map, this);
			});
			marker.addListener('mouseout', function() {
				if (typeof this.box === "object") {
					this.box.close(true);
					delete(this.box);
				}
			});
			if (router.get("lost")) {
				marker.icon = this.lostRouterIcon;
              	marker.addListener('dblclick', function() {
                  	google.maps.event.trigger(marker,'mouseout');
                  	google.maps.event.clearInstanceListeners(marker);
                	marker.setMap(null);
                  	if (marker.bubbleStillOpen) {
                      	bubble.close();
                    }
                  	
                });
            }
		},
		
		onChangedRouter: function(router) {
			var marker = this.markersArray[router.get('id')];
			
			if (router.get("justAnnouncedAPrefix")) {
				marker.icon = this.newRouterIcon;
			}
			else if (router.get("justWithdrawnAPrefix")) {
				marker.icon = this.withdrawRouterIcon;
			}
			else {
				marker.icon = this.defaultRouterIcon;
			}
		},
		
		onDestroyedRouter: function(router) {
			var marker = this.markersArray[router.get('id')];
          	marker.icon = this.lostRouterIcon;
        },
		
		onAddedLink: function(link, links) {
			var that = this;
			var source = null;
			var target = null;
			
			this.routers.each(function(router) {
				if (link.get('localRouter').identifier == router.get('routerId').identifier) {
					source = router;
				}
				if (link.get('remoteRouter').identifier == router.get('routerId').identifier) {
					target = router;
				}
			});
			if (source != null && target != null) {
				var path = [
					{lat: source.get('latitude'), lng: source.get('longitude')},
					{lat: target.get('latitude'), lng: target.get('longitude')}
				];
				var line = new google.maps.Polyline({
					path: path,
					strokeColor: "#888888",
					strokeOpacity: 1,
					strokeWeight: 1,
					geodesic: true,
					map: this.map
				});
				var shadowLine = new google.maps.Polyline({
					path: path,
					strokeColor: "#888888",
					strokeOpacity: 0.05,
					strokeWeight: 15,
					bubbleStillOpen: false,
					bubblePreviousPosition: null,
					geodesic: true,
					map: this.map
				});
				
				this.linksArray[link.get('id')] = shadowLine;
				
				this.links.each(function(l) {
					if ((l.get('localRouter').identifier == target.get('routerId').identifier) &&
					(l.get('remoteRouter').identifier == source.get('routerId').identifier)) {
						
						var data = {
								link1: link.toJSON(),
								link2: l.toJSON(),
								router1: source.toJSON(),
								router2: target.toJSON(),
								gmaps: true
						};
						shadowLine.addListener('mouseover', function() {
							this.box1 = new InfoBox(that.defaultLinkBoxOptions);
                          	this.box1.setContent(link.getMetric());
							this.box1.setPosition(new google.maps.LatLng(source.get('latitude'), source.get('longitude')));
                          	this.box1.pixelOffset = new google.maps.Size(20, 20);
                          	this.box1.open(that.map);
                          	this.box2 = new InfoBox(that.defaultLinkBoxOptions);
                          	this.box2.setContent(l.getMetric());
							this.box2.setPosition(new google.maps.LatLng(target.get('latitude'), target.get('longitude')));
							this.box2.boxClass = 'linklabel';
							this.box2.open(that.map);
                          	
                          	
						});
						shadowLine.addListener('mouseout', function() {
							if (typeof this.box1 === "object") {
								this.box1.close(true);
								delete(this.box1);
							};
                          	if (typeof this.box2 === "object") {
								this.box2.close(true);
								delete(this.box2);
							}
						});
						
						var bubble = new InfoBubble(that.defaultLinkBubbleOptions);
						
						shadowLine.addListener('click', function(e) {

							if (shadowLine.bubbleStillOpen) {
								bubble.close(bubble);
							}
							bubble.setContent(that.bubbleLinkTemplate(data));
							if (e.latLng){
								bubble.setPosition(e.latLng);
                              	that.linksArray[link.get('id')].bubblePreviousPosition = e.latLng;
                            }
                          	else {
                            	bubble.setPosition(that.linksArray[link.get('id')].bubblePreviousPosition);
                            }
							bubble.addListener('closeclick',function(){
								shadowLine.bubbleStillOpen = false;
							});
							bubble.open(that.map);
							shadowLine.bubbleStillOpen = true;
						});
					}
				});
			}		
		},
		
		onChangedLink: function(link) {
		},
		
		onDestroyedLink: function(link) {
		},
		
		render: function(){
			var that = this;
			
			this.$el.show().html(this.template());
			
			this.map = new google.maps.Map(document.getElementById('gmap'), this.mapOptions);
			
			this.$("#gmap-toolbar #refresh").click(function() {
				that.$("#gmap-toolbar #refresh").prop('disabled', true);
				that.refresh();
				return false;
			}).prop('disabled', false);
			
			this.$("#gmap-toolbar #autorefresh-on").click(function() {
				that.$("#gmap-toolbar #autorefresh-on").prop('disabled', true);
				that.$("#gmap-toolbar #autorefresh-on").hide();
				that.autoRefresh();
				that.$("#gmap-toolbar #autorefresh-off").prop('disabled', false);
				that.$("#gmap-toolbar #autorefresh-off").show();
				return false;
			}).prop('disabled', false);	
			this.$("#gmap-toolbar #autorefresh-off").click(function() {
				that.$("#gmap-toolbar #autorefresh-off").prop('disabled', true);
				that.$("#gmap-toolbar #autorefresh-off").hide();
				that.autoRefresh();
				that.$("#gmap-toolbar #autorefresh-on").prop('disabled', false);
				that.$("#gmap-toolbar #autorefresh-on").show();
				return false;
			}).prop('disabled', false);	
			this.$("#gmap-toolbar #gotoblt").click(function() {
				that.$("#gmap-toolbar #gotoblt").prop('disabled', true);
				window.appRouter.navigate("map/" + that.network.id, { trigger: true });
				return false;
			}).prop('disabled', false);
			this.$("#gmap-toolbar #autorefresh-off").hide();
			
			return this;
		}
	});
});