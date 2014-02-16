# WER pairings backend API #

## Add tournament ##

### URL ###
/tournament
### Method ###
`POST`
### Request params ###
`application/json`
``` json
{"name": "Testing tournament"
 "day": "2014-01-01"
 "rounds": 5}
```
### Success response ###
``` json
{"id": 1}
```

## Get tournament by id ##

### URL ###
/tournament/:id
### Method ###
`GET`
### URL Params ###
`id: integer` (Tournament id)
### Success response ###
``` json
{"id": 1,
 "teams": [{"name": "Smith, John", "players": [{"name": "Smith, John", "dci": 12345678}]}],
 "rounds": { "1": ... }
 "standings": [ ... ]}
```

## Add teams to tournament ##

### URL ###
/tournament/:id/teams
### Method ###
`PUT`
### URL Params ###
`id: integer` (Tournament id)
### Request params ###
`application/json`
``` json
[{"name": "Smith, John",
  "players": [{"name": "Smith, John", "dci": 12345678}]},
  ...]
```
### Success response ###
HTTP 200


## Get pairings ##

### URL ###
/tournament/:id/round-:round/pairings
### Method ###
`GET`
### URL Params ###
`id: integer` (Tournament id)
`round: integer` (Round number)
### Success response ###
``` json
[{"team1": "John Smith",
  "team2": "Jane Smith",
  "team1_points": 0,
  "team2_points": 0,
  "table": 1}]
```

## Get results ##

### URL ###
/tournament/:id/round-:round/results
### Method ###
`GET`
### URL Params ###
`id: integer` (Tournament id)
`round: integer` (Round number)
### Success response ###
``` json
[{"team1": "Smith, John",
  "team2": "Smith, Jane",
  "team1_points": 0,
  "team2_points": 0,
  "table": 1,
  "team1_wins": 2,
  "team2_wins": 1,
  "draws": 0},
  ...]
```

## Add pairings ##

### URL ###
/tournament/:id/round-:round/pairings
### Method ###
`PUT`
### URL Params ###
`id: integer` (Tournament id)
`round: integer` (Round number)
### Request params ###
`application/json`
``` json
[{"team1": "Smith, John",
  "team2": "Smith, Jane",
  "table_number": 1},
  ...]
```
### Success response ###
HTTP 200

## Add results ##

### URL ###
/tournament/:id/round-:round/results
### Method ###
`PUT`
### URL Params ###
`id: integer` (Tournament id)
`round: integer` (Round number)
### Request params ###
`application/json`
``` json
[{"table_number": 1,
  "team1_wins": 2,
  "team2_wins": 1,
  "draws": 0},
  ...]
```
### Success response ###
HTTP 200

## Get standings ##

### URL ###
/tournament/:id/standings
### Method ###
`GET`
### URL Params ###
`id: integer` (Tournament id)
### Success response ###
``` json
[{"rank": 1,
  "team": "Smith, John",
  "points": 9,
  "omw": 0.55444,
  "pgw": 0.85714,
  "ogw": 0.46138},
 ...]
 ```
