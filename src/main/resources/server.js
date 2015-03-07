var express = require('express'),
  http = require('http'),
  path = require('path');

var app = module.exports = express();
var server = http.createServer(app);
var io = require('socket.io').listen(server, {log: false});

app.use(express.static(path.join(__dirname, '../../../', 'public')));

var port = process.env.PORT || 5000;

global.sendMessage = function(eventName, msg){
    io.sockets.emit(eventName, msg);
}

io.sockets.on('connection', function(socket){
    socket.on('getServerTime', function(){
        socket.emit('sendServerTime', new Date().getTime());
    });
});


global.app = app;

/* Listen */
server.listen(port);