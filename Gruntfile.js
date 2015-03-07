module.exports = function (grunt) {
  "use strict";

  // Utils
  grunt.util.linefeed = '\r\n';

  // Load tasks
  grunt.loadNpmTasks("grunt-contrib-copy");
  grunt.loadNpmTasks("grunt-contrib-clean");
  grunt.loadNpmTasks('grunt-contrib-watch');

  var gruntConfig = {
    clean: {
      js: {
        src: 'public/lib'
      }
    },
    copy: {
      js: {
        src: 'target/scala-2.11/poormansdisco-scalajs-fastopt.js',
        dest: 'public/lib/poormansdisco.js'
      }
    },
    watch: {
      js: {
        tasks: ['clean:js', 'copy:js'],
        files: 'target/scala-2.11/poormansdisco-scalajs-fastopt.js'
      }
    }
  };

  grunt.registerTask('default', ['watch']);
  // Init
  grunt.initConfig(gruntConfig);
}