var express = require('express'),
  http = require('http'),
  request = require('request'),
  path = require('path');

var app = module.exports = express();
var server = http.createServer(app);
var io = require('socket.io').listen(server, {log: false});

app.use(express.static(path.join(__dirname, '../../../', 'public')));

var port = process.env.PORT || 5000;

global.request = request;

global.sendMessage = function(eventName, msg){
  //console.log('Sending message', eventName, msg);
  io.sockets.emit(eventName, msg);
};

io.sockets.on('connection', function(socket){
  console.log('Received connection');
});


global.app = app;

/* Listen */
server.listen(port);