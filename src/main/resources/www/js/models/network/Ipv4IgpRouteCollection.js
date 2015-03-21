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
	  
	});

});
