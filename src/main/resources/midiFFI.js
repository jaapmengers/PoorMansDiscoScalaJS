var midi = require('../../../node_modules/midi/midi.js');
var generated = require('../../../target/scala-2.11/poormansdisco-scalajs-fastopt.js');
var Event = generated.__ScalaJSExportsNamespace.poormansdiscoscalajs.server.Event;

global.input = new midi.input();

global.eventreceived;

input.on('message', function(deltaTime, message) {
    if(global.eventreceived){
        global.eventreceived(new Event(deltaTime, message[0]));
    }
});

//Create a virtual input port.
input.openVirtualPort("midiPad");

input.ignoreTypes(false, false, false);

