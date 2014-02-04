angular.module('services', [])

.factory('UserService', function() {
  var sdo = {
    loggedIn: false,
    dci: ''
  };

  return sdo;
});