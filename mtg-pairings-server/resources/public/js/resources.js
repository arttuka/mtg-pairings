angular.module('resources', [])

.factory('TournamentResource', function($resource) {
  return $resource('/tournament/:id', {'id': '@id'}, {

  });
})

.factory('PlayerResource', function($resource) {
  return $resource('/player/:dci', {'dci': '@dci'}, {
    tournaments: {
      method: 'GET',
      url: '/player/:dci/tournaments',
      isArray: true
    }
  });
});