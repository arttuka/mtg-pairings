angular.module('controllers', [])

.controller('Testi', function($scope, $window) {
  $scope.number = 1;
  $scope.$on('fooEvent', function() {
    $scope.number = $scope.number + 1;
    $scope.$apply();
  });
  $scope.open = function() {
    $scope.number = 2;
    $scope.win = $window.open('test.html', 'test');
  }
})

.controller('Toinen', function($scope) {
  $scope.$openerScope = window.opener.angular.element('#body').scope();
  $scope.send = function() {
    $scope.$openerScope.$emit('fooEvent');
  };
});