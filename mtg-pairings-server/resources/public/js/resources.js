angular.module('resources', [])

.factory('TournamentResource', function($resource) {
  return $resource('/tournament/:id', {'id': '@id'}, {
    pairings: {
      method: 'GET',
      url: '/tournament/:id/round-:round/pairings',
      isArray: true
    }
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