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
      PlayerResource.tournaments({dci: $scope.dci}).$promise.then(function(tournaments) {
        if(tournaments.length > 0) {
          var latestTournament = tournaments[0];
          if(latestTournament.pairings.length > 0) {
            $scope.latestPairing = latestTournament.pairings[0];
            $scope.latestPairing.tournament = latestTournament.name;
            $scope.latestPairing.day = latestTournament.day;
          } else {
            $scope.latestPairing = latestTournament.seating;
          }
        } else {
          $scope.latestPairing = null;
        }
        $scope.tournaments = tournaments
      });
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

.controller('PairingsController', function($scope, $routeParams, TournamentResource) {
  $scope.allPairings = TournamentResource.pairings({id: $routeParams.tournament,
                                                    round: $routeParams.round});
  $scope.tournament = TournamentResource.get({id: $routeParams.tournament});
  $scope.round = $routeParams.round;
  $scope.sort = 'table_number';
  $scope.$watch('sort', sortPairings);
  $scope.$watchCollection('allPairings', sortPairings);

  function duplicatePairing(p) {
    return {
      draws: p.draws,
      losses: p.wins,
      wins: p.losses,
      team_1_name: p.team_2_name,
      team_2_name: p.team_1_name,
      team_1_points: p.team_2_points,
      team_2_points: p.team_1_points,
      table_number: p.table_number
    };
  }

  function sortPairings() {
    if($scope.sort == 'team_1_name') {
      $scope.pairings = $scope.allPairings.concat(_.map($scope.allPairings, duplicatePairing));
    } else {
      $scope.pairings = $scope.allPairings;
    }
  }
})

.controller('StandingsController', function($scope, $routeParams, TournamentResource) {
  $scope.standings = TournamentResource.standings({id: $routeParams.tournament,
                                                   round: $routeParams.round});
  $scope.tournament = TournamentResource.get({id: $routeParams.tournament});
  $scope.round = $routeParams.round;
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