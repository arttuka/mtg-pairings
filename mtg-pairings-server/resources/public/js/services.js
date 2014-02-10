angular.module('services', [])

.factory('UserService', function() {
  var sdo = {
    loggedIn: false,
    dci: ''
  };

  return sdo;
})

.filter('round', function() {
  return function(num, decimals) {
    return num.toFixed(decimals);
  };
});