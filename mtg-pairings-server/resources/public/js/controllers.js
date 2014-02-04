angular.module('controllers', [])

.controller('MenuController', function($scope, $http, $rootScope, localStorageService) {
  $scope.dci = localStorageService.get('dci') || '';
  $scope.loggedIn = ($scope.dci !== '');

  $scope.login = function() {
    localStorageService.add('dci', $scope.dci);
    $scope.loggedIn = true;
    $rootScope.$broadcast('loggedIn', $scope.dci);
  };
  $scope.logout = function() {
    localStorageService.remove('dci');
    $scope.dci = '';
    $scope.loggedIn = false;
    $rootScope.$broadcast('loggedOut');
  };
})

.controller('MainController', function ($scope, localStorageService) {
  $scope.dci = localStorageService.get('dci');
  $scope.loggedIn = ($scope.dci !== null);
  $scope.$on('loggedIn', function(event, dci) {
    $scope.loggedIn = true;
    $scope.dci = dci;
  });
  $scope.$on('loggedOut', function() {
    $scope.loggedIn = false;
  });
})

.controller('TournamentController', function($scope, TournamentResource) {
  $scope.tournaments = TournamentResource.query();
})

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