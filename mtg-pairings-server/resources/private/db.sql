begin;

create table if not exists trader_user (
  id serial primary key,
  uuid uuid not null unique
);

create table player(
  dci varchar(10) primary key,
  name varchar(100) not null
);

create table tournament(
  id serial primary key,
  sanctionid varchar(20) not null unique,
  name text not null,
  organizer text not null,
  day date not null,
  rounds int not null,
  owner int not null references trader_user (id)
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
  playoff boolean default false,
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
  hidden boolean default false,
  primary key (tournament, round)
);

create table seating (
  id serial primary key,
  tournament int not null references tournament(id),
  table_number int not null,
  team int not null references team(id)
);

create table pod_round (
  id serial primary key,
  tournament int not null references tournament(id),
  round int references round(id)
);

create table pod (
  id serial primary key,
  pod_round int not null references pod_round(id),
  number int not null
);

create table pod_seat (
  id serial primary key,
  team int not null references team(id),
  pod int not null references pod(id),
  seat int not null
);

create table decklist_tournament (
  id char(22) not null primary key,
  "user" integer not null references smf_members(id_member),
  name text not null,
  date date not null,
  deadline timestamptz,
  format text not null
);

create table decklist (
  id char(22) not null primary key,
  tournament char(22) not null references decklist_tournament(id),
  submitted timestamptz not null,
  name text,
  "last-name" text,
  "first-name" text,
  dci text,
  email text
);

create table decklist_card (
  decklist char(22) not null references decklist(id),
  maindeck boolean not null,
  card int not null references trader_card(id),
  index int not null,
  quantity int not null,
  primary key (decklist, maindeck, card)
);

commit;
