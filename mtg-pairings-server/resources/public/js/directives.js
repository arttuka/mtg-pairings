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
})

.directive('stringToNumber', function() {
  return {
    require: 'ngModel',
    link: function(scope, element, attrs, ngModel) {
      ngModel.$parsers.push(function(value) {
        return '' + value;
      });
      ngModel.$formatters.push(function(value) {
        return parseFloat(value, 10);
      });
    }
  };
});