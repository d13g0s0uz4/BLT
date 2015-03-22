define([
	'underscore',
	'backbone',
	'models/network/Ipv4IgpRouteModel'
], function(_, Backbone, Ipv4IgpRouteModel) {

	return Backbone.Collection.extend({

	  model: function(attrs, options) {
			return new Ipv4IgpRouteModel(attrs, {
				network: options.collection.network,
				router: options.collection.router
			});
	  },
		
	  initialize: function(models, options) {
	  	this.network = options.network;
	  	this.router = options.router;
	  },
	  
	  url: function() {
	  	return "api/networks/" + this.network + "/routers/" + this.router + "/ipv4igproutes";
	  },
	  
	  comparator: function(i) {
			var value = 0;
			if (i.isIsisL1()) {
				value += 10000000000;
			}
			else if (i.isIsisL2()) {
				value += 20000000000;
			}
			//value += i.getIpv4NetworkAddressToInt();
			return value;
	  }
	  
	});

});
