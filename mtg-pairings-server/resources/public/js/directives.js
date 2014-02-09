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

.directive('pairing', function() {
  return {
    restrict: 'E',
    replace: true,
    scope: {
      model: '=',
      even: '=?'
    },
    templateUrl : 'templates/pairing.html',
    link: function(scope, element, attrs) {
      scope.isPairing = true;
    },
    controller: function($scope) {
      $scope.even = $scope.even || false;
    }
  };
});