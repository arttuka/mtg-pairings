angular.module('services', [])

.factory('PairingService', function() {
  function duplicatePairing(p) {
    return {
      draws: p.draws,
      team1_wins: p.team2_wins,
      team2_wins: p.team1_wins,
      team1_name: p.team2_name,
      team2_name: p.team1_name,
      team1_points: p.team2_points,
      team2_points: p.team1_points,
      table_number: p.table_number
    };
  }

  return {
    duplicatePairings: function(pairings) {
      return _.filter(pairings.concat(_.map(pairings, duplicatePairing)),
        function(pairing) {
          return pairing.team1_name !== '***BYE***';
        });
    }
  };
})

.filter('round', function() {
  return function(num, decimals) {
    return num.toFixed(decimals);
  };
});