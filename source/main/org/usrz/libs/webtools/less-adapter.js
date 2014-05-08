var _less_process = function(input, optionsMap) {

  var options = {};
  for (var key in optionsMap) options[key] = optionsMap[key];

  var result;
  var error;

  new less.Parser(options).parse(input, function (e, root) {
    if (e) error = e;
    else result = root.toCSS(options);
  });

  if (error) throw error;
  return result;
};
