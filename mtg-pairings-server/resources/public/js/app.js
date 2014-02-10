angular.module('pairings', ['ngTouch', 'ngRoute', 'ngResource', 'LocalStorageModule', 'ui.bootstrap', 
                            'controllers', 'services', 'resources', 'directives'])

.config(function($routeProvider) {
  $routeProvider.
    when('/tournaments/:tournament/pairings-:round', {controller: 'PairingsController',
                                                      templateUrl: 'templates/pairings.html'}).
    when('/tournaments/:tournament/standings-:round', {controller: 'StandingsController',
                                                       templateUrl: 'templates/standings.html'}).
    otherwise({controller: 'TournamentController',
               templateUrl: 'templates/main.html'});
});