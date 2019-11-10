#!/usr/bin/env bash

set -euo pipefail

echo "Generating test tournament"

status=$(curl -s -o /dev/null http://localhost:8080/api/client-version --write-out '%{http_code}')

if [[ "$status" -ne 200 ]]; then
  echo "Can't reach server, are you sure it is running?"
  exit 1
fi

key="5ec35a0e-851a-4cf6-b126-e7f24cf5e371"
sanctionid="12345678"
p1="4050100000"
p2="1010200000"
p3="5060300000"
p4="7010400000"
p5="1060500000"
p6="8020600000"
p7="6060700000"
p8="4020800000"


curl -f -XPOST -H "Content-Type: application/json; charset=utf-8" \
"http://localhost:8080/api/tournament?key=$key" -d @- << EOF
{
  "sanctionid": "$sanctionid",
  "name": "Test tournament",
  "organizer": "Test organizer",
  "day": "$(date +"%Y-%m-%d")",
  "rounds": 3
}
EOF
echo
echo "Add teams"
curl -f -XPUT -H "Content-Type: application/json; charset=utf-8" \
"http://localhost:8080/api/tournament/$sanctionid/teams?key=$key" -d @- << EOF
{
  "teams": [
    {
      "name": "Player 1",
      "players": [{
        "dci": "$p1",
        "name": "Player 1"
      }]
    },
    {
      "name": "Player 2",
      "players": [{
        "dci": "$p2",
        "name": "Player 2"
      }]
    },
    {
      "name": "Player 3",
      "players": [{
        "dci": "$p3",
        "name": "Player 3"
      }]
    },
    {
      "name": "Player 4",
      "players": [{
        "dci": "$p4",
        "name": "Player 4"
      }]
    },
    {
      "name": "Player 5",
      "players": [{
        "dci": "$p5",
        "name": "Player 5"
      }]
    },
    {
      "name": "Player 6",
      "players": [{
        "dci": "$p6",
        "name": "Player 6"
      }]
    },
    {
      "name": "Player 7",
      "players": [{
        "dci": "$p7",
        "name": "Player 7"
      }]
    },
    {
      "name": "Player 8",
      "players": [{
        "dci": "$p8",
        "name": "Player 8"
      }]
    }
  ]
}
EOF
echo
echo "Add R1 pairings"
curl -f -XPUT -H "Content-Type: application/json; charset=utf-8" \
"http://localhost:8080/api/tournament/$sanctionid/round-1/pairings?key=$key" -d @- << EOF
{
  "pairings": [
    {
      "team1": ["$p1"],
      "team2": ["$p2"],
      "table_number": 1
    },
    {
      "team1": ["$p3"],
      "team2": ["$p4"],
      "table_number": 2
    },
    {
      "team1": ["$p5"],
      "team2": ["$p6"],
      "table_number": 3
    },
    {
      "team1": ["$p7"],
      "team2": ["$p8"],
      "table_number": 4
    }
  ],
  "playoff": false
}
EOF
echo
echo "Add R1 results"
curl -f -XPUT -H "Content-Type: application/json; charset=utf-8" \
"http://localhost:8080/api/tournament/$sanctionid/round-1/results?key=$key" -d @- << EOF
{
  "results": [
    {
      "team1": ["$p1"],
      "team2": ["$p2"],
      "table_number": 1,
      "team1_wins": 2,
      "team2_wins": 1,
      "draws": 0
    },
    {
      "team1": ["$p3"],
      "team2": ["$p4"],
      "table_number": 2,
      "team1_wins": 2,
      "team2_wins": 1,
      "draws": 0
    },
    {
      "team1": ["$p5"],
      "team2": ["$p6"],
      "table_number": 3,
      "team1_wins": 2,
      "team2_wins": 0,
      "draws": 0
    },
    {
      "team1": ["$p7"],
      "team2": ["$p8"],
      "table_number": 4,
      "team1_wins": 2,
      "team2_wins": 0,
      "draws": 0
    }
  ]
}
EOF
echo
echo "Add R2 pairings"
curl -f -XPUT -H "Content-Type: application/json; charset=utf-8" \
"http://localhost:8080/api/tournament/$sanctionid/round-2/pairings?key=$key" -d @- << EOF
{
  "pairings": [
    {
      "team1": ["$p1"],
      "team2": ["$p3"],
      "table_number": 1
    },
    {
      "team1": ["$p5"],
      "team2": ["$p7"],
      "table_number": 2
    },
    {
      "team1": ["$p2"],
      "team2": ["$p4"],
      "table_number": 3
    },
    {
      "team1": ["$p6"],
      "team2": ["$p8"],
      "table_number": 4
    }
  ],
  "playoff": false
}
EOF
echo
echo "Add R2 results"
curl -f -XPUT -H "Content-Type: application/json; charset=utf-8" \
"http://localhost:8080/api/tournament/$sanctionid/round-2/results?key=$key" -d @- << EOF
{
  "results": [
    {
      "team1": ["$p1"],
      "team2": ["$p3"],
      "table_number": 1,
      "team1_wins": 2,
      "team2_wins": 1,
      "draws": 0
    },
    {
      "team1": ["$p5"],
      "team2": ["$p7"],
      "table_number": 2,
      "team1_wins": 2,
      "team2_wins": 1,
      "draws": 0
    },
    {
      "team1": ["$p2"],
      "team2": ["$p4"],
      "table_number": 3,
      "team1_wins": 1,
      "team2_wins": 1,
      "draws": 1
    },
    {
      "team1": ["$p6"],
      "team2": ["$p8"],
      "table_number": 4,
      "team1_wins": 2,
      "team2_wins": 0,
      "draws": 0
    }
  ]
}
EOF
echo
echo "Add R3 pairings"
curl -f -XPUT -H "Content-Type: application/json; charset=utf-8" \
"http://localhost:8080/api/tournament/$sanctionid/round-3/pairings?key=$key" -d @- << EOF
{
  "pairings": [
    {
      "team1": ["$p1"],
      "team2": ["$p5"],
      "table_number": 1
    },
    {
      "team1": ["$p3"],
      "team2": ["$p7"],
      "table_number": 2
    },
    {
      "team1": ["$p2"],
      "team2": ["$p6"],
      "table_number": 3
    },
    {
      "team1": ["$p4"],
      "team2": null,
      "table_number": 0
    }
  ],
  "playoff": false
}
EOF
echo
echo "Add R3 results"
curl -f -XPUT -H "Content-Type: application/json; charset=utf-8" \
"http://localhost:8080/api/tournament/$sanctionid/round-3/results?key=$key" -d @- << EOF
{
  "results": [
    {
      "team1": ["$p1"],
      "team2": ["$p5"],
      "table_number": 1,
      "team1_wins": 2,
      "team2_wins": 0,
      "draws": 0
    },
    {
      "team1": ["$p3"],
      "team2": ["$p7"],
      "table_number": 2,
      "team1_wins": 2,
      "team2_wins": 1,
      "draws": 0
    },
    {
      "team1": ["$p2"],
      "team2": ["$p6"],
      "table_number": 3,
      "team1_wins": 2,
      "team2_wins": 0,
      "draws": 0
    },
    {
      "team1": ["$p4"],
      "team2": null,
      "table_number": 0,
      "team1_wins": 2,
      "team2_wins": 0,
      "draws": 0
    }
  ]
}
EOF
echo
echo "Add semifinal pairings"
curl -f -XPUT -H "Content-Type: application/json; charset=utf-8" \
"http://localhost:8080/api/tournament/$sanctionid/round-4/pairings?key=$key" -d @- << EOF
{
  "pairings": [
    {
      "team1": ["$p1"],
      "team2": ["$p2"],
      "table_number": 1
    },
    {
      "team1": ["$p3"],
      "team2": ["$p5"],
      "table_number": 2
    }
  ],
  "playoff": true
}
EOF
echo
echo "Add semifinal results"
curl -f -XPUT -H "Content-Type: application/json; charset=utf-8" \
"http://localhost:8080/api/tournament/$sanctionid/round-4/results?key=$key" -d @- << EOF
{
  "results": [
    {
      "team1": ["$p1"],
      "team2": ["$p2"],
      "table_number": 1,
      "team1_wins": 2,
      "team2_wins": 0,
      "draws": 0
    },
    {
      "team1": ["$p3"],
      "team2": ["$p5"],
      "table_number": 2,
      "team1_wins": 2,
      "team2_wins": 0,
      "draws": 0
    }
  ]
}
EOF
echo
echo "Add final pairings"
curl -f -XPUT -H "Content-Type: application/json; charset=utf-8" \
"http://localhost:8080/api/tournament/$sanctionid/round-5/pairings?key=$key" -d @- << EOF
{
  "pairings": [
    {
      "team1": ["$p1"],
      "team2": ["$p3"],
      "table_number": 1
    }
  ],
  "playoff": true
}
EOF
echo
echo "Add final results"
curl -f -XPUT -H "Content-Type: application/json; charset=utf-8" \
"http://localhost:8080/api/tournament/$sanctionid/round-5/results?key=$key" -d @- << EOF
{
  "results": [
    {
      "team1": ["$p1"],
      "team2": ["$p3"],
      "table_number": 1,
      "team1_wins": 2,
      "team2_wins": 0,
      "draws": 0
    }
  ]
}
EOF
echo
echo "All done!"
