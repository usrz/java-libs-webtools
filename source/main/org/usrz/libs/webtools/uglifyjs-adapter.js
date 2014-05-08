var _uglify_process = function(input, optionsMap) {

  var t = Date.now();
  var options = {};
  for (var key in optionsMap) options[key] = optionsMap[key];

  __logger.debug("Parsing script @%.0f", t);
  var ast = UglifyJS.parse(input, {});
  ast.figure_out_scope();
  
  if (options['compress'] == true) {
    __logger.debug("Compressing script @%.0f", t);
    var ast = ast.transform(UglifyJS.Compressor({}));
  }

  if (options['mangle'] == true) {
    __logger.debug("Mangling script @%.0f", t);
    ast.figure_out_scope();
    ast.compute_char_frequency();
    ast.mangle_names();
  }
  
  __logger.debug("Serializing script @%.0f", t);
  return ast.print_to_string({});
};
