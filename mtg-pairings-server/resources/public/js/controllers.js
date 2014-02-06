angular.module('controllers', [])

.controller('MenuController', function($scope, $http, $rootScope, PlayerResource, localStorageService) {
  $scope.dci = localStorageService.get('dci') || '';
  $scope.name = localStorageService.get('name') || '';
  $scope.loggedIn = ($scope.dci !== '');

  $scope.login = function() {
    PlayerResource.get({dci: $scope.dci}, function(data) {
      localStorageService.add('dci', data.dci);
      localStorageService.add('name', data.name);
      $scope.loggedIn = true;
      $scope.name = data.name;
      $rootScope.$broadcast('loggedIn', $scope.dci);
      console.log("Logged in: " + data.dci + " " + data.name);
    }, function() {
      $scope.dci = '';
      $scope.loggedIn = false;
      console.log("Failed to log in");
    });
  };
  $scope.logout = function() {
    localStorageService.remove('dci');
    localStorageService.remove('name');
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

.controller('TournamentController', function($scope, localStorageService, TournamentResource, PlayerResource) {
  var loadTournaments = function() {
    if($scope.loggedIn) {
      $scope.tournaments = PlayerResource.tournaments({dci: $scope.dci});
    } else {
      $scope.tournaments = TournamentResource.query();
    }
  };
  $scope.dci = localStorageService.get('dci') || '';
  $scope.loggedIn = ($scope.dci !== '');
  $scope.$on('loggedIn', function(event, dci) {
    $scope.loggedIn = true;
    $scope.dci = dci;
    loadTournaments();
  });
  $scope.$on('loggedOut', function() {
    $scope.loggedIn = false;
    loadTournaments();
  });
  loadTournaments();
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