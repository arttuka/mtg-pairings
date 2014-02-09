angular.module('pairings', ['ngTouch', 'ngRoute', 'ngResource', 'LocalStorageModule', 'ui.bootstrap', 
                            'controllers', 'services', 'resources', 'directives'])

.config(function($routeProvider) {
  $routeProvider.when('/tournaments/:tournament/pairings-:round', {controller: 'PairingsController',
                                                                   template: 'templates/pairings.html'});
  $routeProvider.when('/tournaments/:tournament/standings-:round', {controller: 'StandingsController',
                                                                   template: 'templates/standings.html'});
});