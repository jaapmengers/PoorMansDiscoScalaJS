var midi = require('../../../lib/node-midi-master/midi.js');

global.input = new midi.input();
// Create a virtual input port.
input.openVirtualPort("midiPad");

input.ignoreTypes(false, false, false);

