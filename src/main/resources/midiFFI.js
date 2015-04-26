var midi = require('../../../node_modules/midi/midi.js');
var generated = require('../../../target/scala-2.11/poormansdisco-scalajs-fastopt.js');
var MidiEvent = generated.__ScalaJSExportsNamespace.poormansdiscoscalajs.shared.MidiEvent;

var input = new midi.input();

input.openVirtualPort("midiPad");
input.ignoreTypes(false, false, false);
global.midiInput = input;