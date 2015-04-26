var express = require('express'),
  http = require('http'),
  request = require('request'),
  path = require('path');

var app = express();
var server = http.createServer(app);
var port = process.env.PORT || 5000;

app.use(express.static(path.join(__dirname, '../../../', 'public')));

console.log('Running on port ' + port);

global.requestInstance = request;
global.app = app;
global.SocketManager = require('socket.io').listen(server, {log: false});

server.listen(port);