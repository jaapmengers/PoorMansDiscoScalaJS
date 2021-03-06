var midi = require('../../../node_modules/midi/midi.js');
var generated = require('../../../target/scala-2.11/poormansdisco-scalajs-fastopt.js');
var MidiEvent = generated.__ScalaJSExportsNamespace.poormansdiscoscalajs.shared.MidiEvent;

var input = new midi.input();

input.on('message', function(deltaTime, message) {
    if(global.eventreceived){
        global.eventreceived(new MidiEvent(deltaTime, message));
    }
});

//Create a virtual input port.
input.openVirtualPort("midiPad");

input.ignoreTypes(false, false, false);