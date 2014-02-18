angular.module('directives', [])

.directive('ownTournament', function() {
  return {
    restrict: 'E',
    replace: true,
    scope: {
      model: '='
    },
    templateUrl: 'templates/own_tournament.html',
    controller: function($scope) {
      $scope.isCollapsed = true;
    }
  };
})

.directive('tournament', function() {
  return {
    restrict: 'E',
    replace: true,
    scope: {
      model: '='
    },
    templateUrl: 'templates/tournament.html'
  };
})

.directive('pairing', function() {
  return {
    restrict: 'E',
    replace: true,
    scope: {
      model: '=',
      even: '=?',
      displayRound: '=?',
      isPairing: '=?'
    },
    templateUrl : 'templates/pairing.html',
    controller: function($scope) {
      $scope.even = $scope.even || false;
      if($scope.isPairing === undefined) {
        $scope.isPairing = true;
      }
    }
  };
});