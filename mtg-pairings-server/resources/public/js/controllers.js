angular.module('controllers', [])

.controller('MenuController', function($scope, $http, $rootScope, PlayerResource, localStorageService) {
  $scope.dci = localStorageService.get('dci') || '';
  $scope.name = localStorageService.get('name') || '';
  $scope.loggedIn = ($scope.dci !== '');
  $scope.menuCollapsed = true;

  $scope.login = function() {
    PlayerResource.get({dci: $scope.dci}, function(data) {
      localStorageService.add('dci', data.dci);
      localStorageService.add('name', data.name);
      $scope.loggedIn = true;
      $scope.name = data.name;
      $rootScope.$broadcast('loggedIn', $scope.dci);
      console.log("Logged in: " + data.dci + " " + data.name);
    }, function() {
      $scope.dci = '';
      $scope.loggedIn = false;
      console.log("Failed to log in");
    });
  };
  $scope.logout = function() {
    localStorageService.remove('dci');
    localStorageService.remove('name');
    $scope.dci = '';
    $scope.loggedIn = false;
    $rootScope.$broadcast('loggedOut');
  };
})

.controller('TournamentController', function($scope, $routeParams, TournamentResource) {
  TournamentResource.get({id: $routeParams.tournament}).$promise.then(function(tournament) {
    $scope.model = tournament;
    $scope.model.round_nums = _.union(tournament.round, tournament.standings);
  });
})

.controller('TournamentsController', function($scope, TournamentResource) {
  TournamentResource.query().$promise.then(function(tournaments) {
    $scope.tournaments = _.map(tournaments, function(tournament) {
      tournament.round_nums = _.union(tournament.round, tournament.standings);
      return tournament;
    });
  });
})

.controller('MainController', function($scope, localStorageService, TournamentResource, PlayerResource) {
  var loadTournaments = function() {
    if($scope.loggedIn) {
      PlayerResource.tournaments({dci: $scope.dci}).$promise.then(function(tournaments) {
        if(tournaments.length > 0) {
          var latestTournament = tournaments[0];
          if(latestTournament.pairings.length > 0) {
            $scope.latestPairing = latestTournament.pairings[0];
            $scope.latestPairing.tournament = latestTournament.name;
            $scope.latestPairing.day = latestTournament.day;
          } else {
            $scope.latestPairing = latestTournament.seating;
          }
        } else {
          $scope.latestPairing = null;
        }
        $scope.tournaments = tournaments
      });
    } else {
      TournamentResource.query().$promise.then(function(tournaments) {
        $scope.tournaments = _.map(tournaments, function(tournament) {
          tournament.round_nums = _.union(tournament.round, tournament.standings);
          return tournament;
        });
      });
    }
  };
  $scope.dci = localStorageService.get('dci') || '';
  $scope.loggedIn = ($scope.dci !== '');
  $scope.$on('loggedIn', function(event, dci) {
    $scope.loggedIn = true;
    $scope.dci = dci;
    loadTournaments();
  });
  $scope.$on('loggedOut', function() {
    $scope.loggedIn = false;
    loadTournaments();
  });
  loadTournaments();
})

.controller('PairingsController', function($scope, $routeParams, TournamentResource, PairingService) {
  $scope.allPairings = TournamentResource.pairings({id: $routeParams.tournament,
                                                    round: $routeParams.round});
  $scope.tournament = TournamentResource.get({id: $routeParams.tournament});
  $scope.round = $routeParams.round;
  $scope.sort = 'table_number';
  $scope.$watch('sort', sortPairings);
  $scope.$watchCollection('allPairings', sortPairings);



  function sortPairings() {
    if($scope.sort == 'team1_name') {
      $scope.pairings = PairingService.duplicatePairings($scope.allPairings);
    } else {
      $scope.pairings = $scope.allPairings;
    }
  }
})

.controller('StandingsController', function($scope, $routeParams, TournamentResource) {
  $scope.standings = TournamentResource.standings({id: $routeParams.tournament,
                                                   round: $routeParams.round});
  $scope.tournament = TournamentResource.get({id: $routeParams.tournament});
  $scope.round = $routeParams.round;
})

.controller('SeatingsController', function($scope, $routeParams, TournamentResource) {
  $scope.seatings = TournamentResource.seatings({id: $routeParams.tournament});
  $scope.tournament = TournamentResource.get({id: $routeParams.tournament});
})

.controller('OrganizerController', function($scope, $routeParams, $window, TournamentResource, PairingService) {
  var tournamentId = $routeParams.tournament;
  var menuWindow;
  var menuScope;
  $scope.displayMenu = true;
  $scope.newStandings = false;
  $scope.newPairings = false;
  $scope.popupMenu = popupMenu;
  $scope.showPairings = showPairings;
  $scope.$on('showPairings', function(event, round) {
    $scope.pairings_round = round;
    showPairings();
  });
  $scope.showStandings = showStandings;
  $scope.$on('showStandings', function(event, round) {
    $scope.standings_round = round;
    showStandings();
  });
  $scope.showClock = showClock;
  $scope.$on('showClock', showClock);
  $scope.setClock = setClock;
  $scope.$on('setClock', function(event, minutes) {
    $scope.minutes = minutes;
    setClock();
  });
  $scope.startClock = startClock;
  $scope.$on('startClock', startClock);
  $scope.stopClock = stopClock;
  $scope.$on('stopClock', stopClock);
  $scope.clock = {
    timeout: false,
    running: false,
    lastTime: new Date(),
    seconds: 0,
    text: "00:00"
  };
  $scope.$on('menuClosed', function() {
    $scope.displayMenu = true;
    setTimeout(function(){ $scope.$apply(); });
  });
  $scope.minutes = 50;
  loadTournament();
  var interval = setInterval(loadTournament, 5000);

  function arraysIdentical(a, b) {
    if(a === undefined || b === undefined) return false;
    var i = a.length;
    if (i != b.length) return false;
    while (i--) {
      if (a[i] !== b[i]) return false;
    }
    return true;
  };

  function pad(num) {
    if(Math.abs(num) >= 10) return "" + num;
    else if(num < 0) return "-0" + Math.abs(num);
    else return "0" + num;
  }

  function loadTournament() {
    TournamentResource.get({id: tournamentId}).$promise.then(function(t) {
      $scope.tournament = t;
      if(!arraysIdentical($scope.pairing_rounds, t.round)) {
        $scope.newPairings = true;
        $scope.pairings_round = t.round[t.round.length - 1];
      }
      $scope.pairing_rounds = t.round;
      if(!arraysIdentical($scope.standings_rounds, t.standings)) {
        $scope.newStandings = true;
        $scope.standings_round = t.standings[t.standings.length - 1];
      }
      $scope.standings_rounds = t.standings;
      if(menuScope) {
        menuScope.$emit('tournament', {
          newPairings: $scope.newPairings,
          newStandings: $scope.newStandings,
          pairing_rounds: $scope.pairing_rounds,
          standings_rounds: $scope.standings_rounds
        });
      }
    });
  }

  function updateClock() {
    if($scope.clock.running) {
      var now = new Date();
      var diff = (now.getTime() - $scope.clock.lastTime.getTime()) / 1000;
      $scope.clock.lastTime = now;
      $scope.clock.seconds -= diff;
      setTimeout(updateClock, 200);
    }
    $scope.clock.timeout = ($scope.clock.seconds < 0);
    var minutes;
    if($scope.clock.timeout) minutes = Math.ceil($scope.clock.seconds / 60);
    else minutes = Math.floor($scope.clock.seconds / 60);
    var seconds = Math.floor($scope.clock.seconds % 60);
    var sign = "";
    if($scope.clock.timeout) sign = "-";
    $scope.clock.text = sign + pad(Math.abs(minutes)) + ":" + pad(Math.abs(seconds));
    setTimeout(function(){ $scope.$apply(); });
  }

  function showPairings() {
    var round = $scope.pairings_round;
    TournamentResource.pairings({id: tournamentId, round: round}).$promise.then(function(pairings) {
      var duplicated = _.sortBy(PairingService.duplicatePairings(pairings), "team1_name");
      var pairings = [];
      var perColumn = Math.ceil(duplicated.length / Math.ceil(duplicated.length / 45));
      while(!_.isEmpty(duplicated)) {
        pairings.push(_.take(duplicated, perColumn));
        duplicated = _.drop(duplicated, perColumn);
      }
      $scope.pairings = pairings;
      $scope.displayStandings = false;
      $scope.displayClock = false;
      $scope.displayPairings = true;
      $scope.newPairings = false;
    });
  }

  function showStandings() {
    var round = $scope.standings_round;
    TournamentResource.standings({id: tournamentId, round: round}).$promise.then(function(data) {
      var standings = [];
      var perColumn = Math.ceil(data.length / Math.ceil(data.length / 45));
      while(!_.isEmpty(data)) {
        standings.push(_.take(data, perColumn));
        data = _.drop(data, perColumn);
      }
      $scope.standings = standings;
      $scope.displayClock = false;
      $scope.displayPairings = false;
      $scope.displayStandings = true;
      $scope.newStandings = false;
    });
  }

  function stopClock() {
    $scope.clock.running = false;
  }

  function startClock() {
    $scope.clock.running = true;
    $scope.clock.lastTime = new Date();
    setTimeout(updateClock, 100);
  }

  function setClock() {
    $scope.clock.seconds = $scope.minutes * 60;
    updateClock();
  }

  function showClock() {
    $scope.displayPairings = false;
    $scope.displayStandings = false;
    $scope.displayClock = true;
    setTimeout(function(){ $scope.$apply(); });
  }

  function popupMenu() {
    menuWindow = $window.open('menu.html', 'menu');
    setTimeout(function() { menuScope = menuWindow.angular.element('#menu').scope(); }, 1000);
    $scope.displayMenu = false;
  }
})

.controller('OrganizerMenuController', function($scope, TournamentResource) {
  var openerScope = window.opener.angular.element('#content').scope();
  var tournamentId;
  $scope.$on('tournament', function(event, tournament) {
    $scope.newPairings = tournament.newPairings;
    $scope.newStandings = tournament.newStandings;
    $scope.pairing_rounds = tournament.pairing_rounds;
    $scope.standings_rounds = tournament.standings_rounds;
    if(tournament.newPairings) $scope.pairings_round = tournament.pairing_rounds[tournament.pairing_rounds.length - 1];
    if(tournament.newStandings) $scope.standings_round = tournament.standings_rounds[tournament.standings_rounds.length - 1];
    setTimeout(function(){ $scope.$apply(); });
  });
  $scope.newPairings = false;
  $scope.newStandings = false;
  $scope.running = false;
  $scope.minutes = 50;
  $scope.showPairings = showPairings;
  $scope.showStandings = showStandings;
  $scope.showSeatings = showSeatings;
  $scope.showClock = showClock;
  $scope.setClock = setClock;
  $scope.startClock = startClock;
  $scope.stopClock = stopClock;
  $scope.close = close;


  function arraysIdentical(a, b) {
    if(a === undefined || b === undefined) return false;
    var i = a.length;
    if (i != b.length) return false;
    while (i--) {
      if (a[i] !== b[i]) return false;
    }
    return true;
  };

  function loadTournament() {
    TournamentResource.get({id: tournamentId}).$promise.then(function(t) {
      $scope.tournament = t;
      if(!arraysIdentical($scope.pairing_rounds, t.round)) {
        $scope.newPairings = true;
        $scope.pairings_round = t.round[t.round.length - 1];
      }
      $scope.pairing_rounds = t.round;
      if(!arraysIdentical($scope.standings_rounds, t.standings)) {
        $scope.newStandings = true;
        $scope.standings_round = t.standings[t.standings.length - 1];
      }
      $scope.standings_rounds = t.standings;
    });
  }

  function send(event, data) {
    openerScope.$emit(event, data);
  }
  function close() {
    send('menuClosed');
    window.close();
  }

  function showPairings() {
    send('showPairings', $scope.pairings_round);
  }
  function showStandings() {
    send('showStandings', $scope.standings_round);
  }
  function showSeatings() {
    send('showSeatings');
  }
  function showClock() {
    send('showClock');
  }
  function setClock() {
    send('setClock', $scope.minutes);
  }
  function startClock() {
    $scope.running = true;
    send('startClock');
  }
  function stopClock() {
    $scope.running = false;
    send('stopClock');
  }
});
