angular.module('resources', [])

.factory('TournamentResource', function($resource) {
  return $resource('/tournament/:id', {'id': '@id'}, {

  });
});