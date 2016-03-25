angular.module('resources', [])

.factory('TournamentResource', function($resource) {

  return $resource('/api/tournament/:id', {'id': '@id'}, {
    pairings: {
      method: 'GET',
      url: '/api/tournament/:id/round-:round/pairings',
      isArray: true
    },
    standings: {
      method: 'GET',
      url: '/api/tournament/:id/round-:round/standings',
      isArray: true
    },
    seatings: {
      method: 'GET',
      url: '/api/tournament/:id/seatings',
      isArray: true
    },
    pods: {
      method: 'GET',
      url: '/api/tournament/:id/pods-:number',
      isArray: true
    }
  });
})

.factory('PlayerResource', function($resource) {
  return $resource('/api/player/:dci', {'dci': '@dci'}, {
    tournaments: {
      method: 'GET',
      url: '/api/player/:dci/tournaments',
      isArray: true
    }
  });
});