begin;
create table player(
  dci varchar(10) primary key,
  name varchar(100) not null
);

create table tournament(
  id serial primary key,
  name varchar(100) not null,
  day date not null,
  rounds int not null
);

create table team(
  id serial primary key,
  name varchar(200) not null,
  tournament int not null references tournament(id),
  unique (tournament, name)
);

create table team_players(
  team int not null references team(id),
  player varchar(10) not null references player(dci),
  primary key (team, player)
);

create table round(
  id serial primary key,
  num int not null,
  tournament int not null references tournament(id),
  unique (tournament, num)
);

create table pairing(
  id serial primary key,
  round int not null references round(id),
  team1 int not null references team(id),
  team2 int references team(id),
  team1_points int not null,
  team2_points int not null,
  table_number int not null
);

create table result(
  pairing int primary key references pairing(id),
  team1_wins int not null,
  team2_wins int not null,
  draws int not null
);

create table standings(
  tournament int references tournament(id),
  round int not null,
  standings text not null,
  primary key (tournament, round)
);
commit;