angular.module('pairings', ['ngTouch', 'ngRoute', 'ngResource', 'LocalStorageModule', 'ui.bootstrap',
                            'controllers', 'services', 'resources', 'directives'])

.config(function($routeProvider) {
  $routeProvider.
    when('/tournaments/', {controller: 'TournamentsController',
                           templateUrl: 'templates/tournaments.html'}).
    when('/tournaments/:tournament', {controller: 'TournamentController',
                                      templateUrl: 'templates/tournament.html'}).
    when('/tournaments/:tournament/seatings', {controller: 'SeatingsController',
                                               templateUrl: 'templates/seatings.html'}).
    when('/tournaments/:tournament/pairings-:round', {controller: 'PairingsController',
                                                      templateUrl: 'templates/pairings.html'}).
    when('/tournaments/:tournament/standings-:round', {controller: 'StandingsController',
                                                       templateUrl: 'templates/standings.html'}).
    when('/tournaments/:tournament/pods-:number', {controller: 'PodsController',
                                                   templateUrl: 'templates/pods.html'}).
    when('/tournaments/:tournament/organizer', {controller: 'OrganizerController',
                                                templateUrl: 'templates/organizer.html'}).
    when('/tournaments/:tournament/coverage', {controller: 'CoverageController',
                                               templateUrl: 'templates/coverage.html'}).
    otherwise({controller: 'MainController',
               templateUrl: 'templates/main.html'});
});