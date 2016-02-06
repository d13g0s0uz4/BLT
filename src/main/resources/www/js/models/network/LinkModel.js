/*******************************************************************************
 * Copyright (c) 2015 Netfishers - contact@netfishers.onl
 *******************************************************************************/
define([
	'underscore',
	'backbone',
], function(_, Backbone) {

	return Backbone.Model.extend({

		initialize: function(attr, options) {
			this.network = options.network;
		},

		urlRoot: function() {
			return "api/networks/" + this.network + "/links";
		},

		getShortName: function() {
			var name = this.get('localInterfaceName');
			if (typeof(name) == "undefined") {
				//return "?";
				return this.get('protocolId');
			}
			else {
				name = name.replace(/^Ethernet/, "Eth");
				name = name.replace(/GigabitEthernet/, "Gi");
				name = name.replace(/Bundle-Ether/, "BE");
				name = name.replace(/TenGigE/, "Te");
				return name;
			}
		},
		getMetric: function() {
			var name = this.get('metric');
			if (typeof(name) == "undefined") {
				return "...";
			}
			else {
				return name.toString();
			}
		},
		
		isLost: function() {
			var lost = this.get('lost');
			if (typeof(lost) != "undefined" && lost === true) {
				return true;
			}
			return false;
		}

	});

});
