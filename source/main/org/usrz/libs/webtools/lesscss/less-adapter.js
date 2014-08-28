/* ========================================================================== */
/* READ UP AN @import DECLARATION                                             */
/* ========================================================================== */

less.Parser.fileLoader = function (file, currentFileInfo, callback, env) {

  var href = file;
  if (currentFileInfo && currentFileInfo.currentDirectory && !/^\//.test(file)) {
      href = less.modules.path.join(currentFileInfo.currentDirectory, file);
  }

  var path = less.modules.path.dirname(href);

  var newFileInfo = {
      currentDirectory: path + '/',
      filename: href
  };

  if (currentFileInfo) {
      newFileInfo.entryPath = currentFileInfo.entryPath;
      newFileInfo.rootpath = currentFileInfo.rootpath;
      newFileInfo.rootFilename = currentFileInfo.rootFilename;
      newFileInfo.relativeUrls = currentFileInfo.relativeUrls;
  } else {
      newFileInfo.entryPath = path;
      newFileInfo.rootpath = less.rootpath || path;
      newFileInfo.rootFilename = href;
      newFileInfo.relativeUrls = env.relativeUrls;
  }

  var j = file.lastIndexOf('/');
  if(newFileInfo.relativeUrls && !/^(?:[a-z-]+:|\/)/.test(file) && j != -1) {
      var relativeSubDirectory = file.slice(0, j+1);
      newFileInfo.rootpath = newFileInfo.rootpath + relativeSubDirectory; // append (sub|sup) directory path of imported file
  }
  newFileInfo.currentDirectory = path;
  newFileInfo.filename = href;

  var data = null;
  try {
    data = _less_file_getter.apply(href);
  } catch (e) {
    callback({ type: 'File', message: "'" + less.modules.path.basename(href) + "' could not be imported" });
    return;
  }

  try {
    callback(null, data, href, newFileInfo, { lastModified: 0 });
  } catch (e) {
    callback(e, null, href);
  }
}

/* ========================================================================== */
/* PROCESS A LESS FILE (main entry point)                                     */
/* ========================================================================== */

var _less_file_getter = { apply: function() { return null } };
var _less_process = function(input, optionsMap) {

  var options = {};
  for (var key in optionsMap) options[key] = optionsMap[key];

  var result;
  var error;

  new less.Parser(options).parse(input, function (e, root) {
    if (e) {
      error = e;
    } else {
      result = root.toCSS(options);
    }
  });


  if (error) throw _less_format_error(error);
  if (result == null) throw "No results from LESS processor";
  return result;
};

/* ========================================================================== */
/* FORMAT AN ERROR MESSAGE                                                    */
/* ========================================================================== */

function _less_format_error(ctx, options) {
  options = options || {};

  var message = "";
  var extract = ctx.extract;
  var error = [];

  // var stylize = options.color ? require('./lessc_helper').stylize : function (str) { return str; };
  var stylize = function (str) { return str; };

  // only output a stack if it isn't a less error
  if (ctx.stack && !ctx.type) { return stylize(ctx.stack, 'red'); }

  if (!ctx.hasOwnProperty('index') || !extract) {
      return ctx.stack || ctx.message;
  }

  if (typeof(extract[0]) === 'string') {
      error.push(stylize((ctx.line - 1) + ' ' + extract[0], 'grey'));
  }

  if (typeof(extract[1]) === 'string') {
      var errorTxt = ctx.line + ' ';
      if (extract[1]) {
          errorTxt += extract[1].slice(0, ctx.column) +
                  stylize(stylize(stylize(extract[1][ctx.column], 'bold') +
                          extract[1].slice(ctx.column + 1), 'red'), 'inverse');
      }
      error.push(errorTxt);
  }

  if (typeof(extract[2]) === 'string') {
      error.push(stylize((ctx.line + 1) + ' ' + extract[2], 'grey'));
  }
  error = error.join('\n') + stylize('', 'reset') + '\n';

  message += stylize(ctx.type + 'Error: ' + ctx.message, 'red');
  if (ctx.filename) {
   message += stylize(' in ', 'red') + ctx.filename +
          stylize(' on line ' + ctx.line + ', column ' + (ctx.column + 1) + ':', 'grey');
  }

  message += '\n' + error;

  if (ctx.callLine) {
      message += stylize('from ', 'red') + (ctx.filename || '') + '/n';
      message += stylize(ctx.callLine, 'grey') + ' ' + ctx.callExtract + '/n';
  }

  return message;
}

