(function(root) {
	
	var modules = {};
	var cache = {};

	root.require = function(name) {
		if (!Object.hasOwnProperty.call(cache, name)) {
			var module = {
				id: name,
			};
			var exports = {};
			var require = root.require;

			modules[name].call(module, require, module, exports);

			cache[name] = (Object.hasOwnProperty.call(module, 'exports')) ? module.exports : exports;
		}

		return cache[name];
	};

	// factory (require, module, exports)
	root.require.register = function(name, factory) {
		modules[name] = factory;
	};

})(window);