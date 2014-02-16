angular.module('services', [])

.factory('PairingService', function() {
  function duplicatePairing(p) {
    return {
      draws: p.draws,
      losses: p.wins,
      wins: p.losses,
      team_1_name: p.team_2_name,
      team_2_name: p.team_1_name,
      team_1_points: p.team_2_points,
      team_2_points: p.team_1_points,
      table_number: p.table_number
    };
  }

  return {
    duplicatePairings: function(pairings) {
      return pairings.concat(_.map(pairings, duplicatePairing));
    }
  };
})

.filter('round', function() {
  return function(num, decimals) {
    return num.toFixed(decimals);
  };
});