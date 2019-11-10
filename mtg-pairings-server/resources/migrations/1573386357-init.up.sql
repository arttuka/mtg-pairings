CREATE TABLE decklist (
    id character(22) NOT NULL,
    tournament character(22) NOT NULL,
    name text,
    "last-name" text,
    "first-name" text,
    dci text,
    submitted timestamp with time zone NOT NULL,
    email text
);

CREATE TABLE decklist_card (
    decklist character(22) NOT NULL,
    maindeck boolean NOT NULL,
    card integer NOT NULL,
    quantity integer NOT NULL
);

CREATE TABLE decklist_tournament (
    id character(22) NOT NULL,
    name text NOT NULL,
    date date NOT NULL,
    format text NOT NULL,
    deadline timestamp with time zone NOT NULL,
    "user" integer NOT NULL
);

CREATE TABLE pairing (
    id serial NOT NULL,
    round integer NOT NULL,
    team1 integer NOT NULL,
    team2 integer,
    team1_points integer NOT NULL,
    team2_points integer,
    table_number integer NOT NULL
);

CREATE TABLE player (
    dci character varying(10) NOT NULL,
    name character varying(100) NOT NULL
);

CREATE TABLE pod (
    id serial NOT NULL,
    pod_round integer NOT NULL,
    number integer NOT NULL
);

CREATE TABLE pod_round (
    id serial NOT NULL,
    tournament integer NOT NULL,
    round integer
);

CREATE TABLE pod_seat (
    id serial NOT NULL,
    team integer NOT NULL,
    pod integer NOT NULL,
    seat integer NOT NULL
);

CREATE TABLE result (
    pairing integer NOT NULL,
    team1_wins integer NOT NULL,
    team2_wins integer NOT NULL,
    draws integer NOT NULL
);

CREATE TABLE round (
    id serial NOT NULL,
    num integer NOT NULL,
    tournament integer NOT NULL,
    playoff boolean DEFAULT false
);


CREATE TABLE seating (
    id serial NOT NULL,
    tournament integer NOT NULL,
    table_number integer NOT NULL,
    team integer NOT NULL
);

CREATE TABLE smf_members (
    id_member serial NOT NULL,
    member_name character varying(80) DEFAULT ''::character varying NOT NULL,
    passwd character varying(64) DEFAULT ''::character varying NOT NULL
);

CREATE TABLE standings (
    tournament integer NOT NULL,
    round integer NOT NULL,
    standings text NOT NULL,
    hidden boolean DEFAULT false
);


CREATE TABLE team (
    id serial NOT NULL,
    name character varying(200) NOT NULL,
    tournament integer NOT NULL
);

CREATE TABLE team_players (
    team integer NOT NULL,
    player character varying(10) NOT NULL
);

CREATE TABLE tournament (
    id serial NOT NULL,
    sanctionid character varying(20) NOT NULL,
    name text NOT NULL,
    day date NOT NULL,
    rounds integer NOT NULL,
    owner integer NOT NULL,
    organizer text DEFAULT ''::text NOT NULL
);

CREATE TABLE trader_card (
    id serial NOT NULL,
    name text NOT NULL,
    standard boolean DEFAULT false,
    modern boolean DEFAULT false,
    legacy boolean DEFAULT false,
    types text[]
);

CREATE TABLE trader_user (
    id integer NOT NULL,
    username text NOT NULL,
    name text,
    uuid uuid NOT NULL
);

ALTER TABLE ONLY decklist_card
    ADD CONSTRAINT decklist_card_pkey PRIMARY KEY (decklist, maindeck, card);

ALTER TABLE ONLY decklist
    ADD CONSTRAINT decklist_pkey PRIMARY KEY (id);

ALTER TABLE ONLY decklist_tournament
    ADD CONSTRAINT decklist_tournament_pkey PRIMARY KEY (id);

ALTER TABLE ONLY pairing
    ADD CONSTRAINT pairing_pkey PRIMARY KEY (id);

ALTER TABLE ONLY player
    ADD CONSTRAINT player_pkey PRIMARY KEY (dci);

ALTER TABLE ONLY pod
    ADD CONSTRAINT pod_pkey PRIMARY KEY (id);

ALTER TABLE ONLY pod_round
    ADD CONSTRAINT pod_round_pkey PRIMARY KEY (id);

ALTER TABLE ONLY pod_seat
    ADD CONSTRAINT pod_seat_pkey PRIMARY KEY (id);

ALTER TABLE ONLY result
    ADD CONSTRAINT result_pkey PRIMARY KEY (pairing);

ALTER TABLE ONLY round
    ADD CONSTRAINT round_pkey PRIMARY KEY (id);

ALTER TABLE ONLY round
    ADD CONSTRAINT round_tournament_num_key UNIQUE (tournament, num);

ALTER TABLE ONLY seating
    ADD CONSTRAINT seating_pkey PRIMARY KEY (id);

ALTER TABLE ONLY smf_members
    ADD CONSTRAINT smf_members_pkey PRIMARY KEY (id_member);

ALTER TABLE ONLY standings
    ADD CONSTRAINT standings_pkey PRIMARY KEY (tournament, round);

ALTER TABLE ONLY team
    ADD CONSTRAINT team_pkey PRIMARY KEY (id);

ALTER TABLE ONLY team_players
    ADD CONSTRAINT team_players_pkey PRIMARY KEY (team, player);

ALTER TABLE ONLY tournament
    ADD CONSTRAINT tournament_pkey PRIMARY KEY (id);

ALTER TABLE ONLY tournament
    ADD CONSTRAINT tournament_sanctionid_key UNIQUE (sanctionid);

ALTER TABLE ONLY trader_card
    ADD CONSTRAINT trader_card_name_key UNIQUE (name);

ALTER TABLE ONLY trader_card
    ADD CONSTRAINT trader_card_pkey PRIMARY KEY (id);

ALTER TABLE ONLY trader_user
    ADD CONSTRAINT trader_user_pkey PRIMARY KEY (id);

ALTER TABLE ONLY trader_user
    ADD CONSTRAINT trader_user_uuid_unique UNIQUE (uuid);

CREATE INDEX trader_card_legacy_name ON trader_card USING btree (legacy, name text_pattern_ops);

CREATE INDEX trader_card_modern_name ON trader_card USING btree (modern, name text_pattern_ops);

CREATE INDEX trader_card_standard_name ON trader_card USING btree (standard, name text_pattern_ops);

ALTER TABLE ONLY decklist_card
    ADD CONSTRAINT decklist_card_card_fkey FOREIGN KEY (card) REFERENCES trader_card(id);

ALTER TABLE ONLY decklist_card
    ADD CONSTRAINT decklist_card_decklist_fkey FOREIGN KEY (decklist) REFERENCES decklist(id);

ALTER TABLE ONLY decklist
    ADD CONSTRAINT decklist_tournament_fkey FOREIGN KEY (tournament) REFERENCES decklist_tournament(id);

ALTER TABLE ONLY decklist_tournament
    ADD CONSTRAINT decklist_tournament_user_fkey FOREIGN KEY ("user") REFERENCES smf_members(id_member);

ALTER TABLE ONLY pairing
    ADD CONSTRAINT pairing_round_fkey FOREIGN KEY (round) REFERENCES round(id);

ALTER TABLE ONLY pairing
    ADD CONSTRAINT pairing_team1_fkey FOREIGN KEY (team1) REFERENCES team(id);

ALTER TABLE ONLY pairing
    ADD CONSTRAINT pairing_team2_fkey FOREIGN KEY (team2) REFERENCES team(id);

ALTER TABLE ONLY pod
    ADD CONSTRAINT pod_pod_round_fkey FOREIGN KEY (pod_round) REFERENCES pod_round(id);

ALTER TABLE ONLY pod_round
    ADD CONSTRAINT pod_round_round_fkey FOREIGN KEY (round) REFERENCES round(id);

ALTER TABLE ONLY pod_round
    ADD CONSTRAINT pod_round_tournament_fkey FOREIGN KEY (tournament) REFERENCES tournament(id);

ALTER TABLE ONLY pod_seat
    ADD CONSTRAINT pod_seat_pod_fkey FOREIGN KEY (pod) REFERENCES pod(id);

ALTER TABLE ONLY pod_seat
    ADD CONSTRAINT pod_seat_team_fkey FOREIGN KEY (team) REFERENCES team(id);

ALTER TABLE ONLY result
    ADD CONSTRAINT result_pairing_fkey FOREIGN KEY (pairing) REFERENCES pairing(id);

ALTER TABLE ONLY round
    ADD CONSTRAINT round_tournament_fkey FOREIGN KEY (tournament) REFERENCES tournament(id);

ALTER TABLE ONLY seating
    ADD CONSTRAINT seating_team_fkey FOREIGN KEY (team) REFERENCES team(id);

ALTER TABLE ONLY seating
    ADD CONSTRAINT seating_tournament_fkey FOREIGN KEY (tournament) REFERENCES tournament(id);

ALTER TABLE ONLY standings
    ADD CONSTRAINT standings_tournament_fkey FOREIGN KEY (tournament) REFERENCES tournament(id);

ALTER TABLE ONLY team_players
    ADD CONSTRAINT team_players_player_fkey FOREIGN KEY (player) REFERENCES player(dci);

ALTER TABLE ONLY team_players
    ADD CONSTRAINT team_players_team_fkey FOREIGN KEY (team) REFERENCES team(id);

ALTER TABLE ONLY team
    ADD CONSTRAINT team_tournament_fkey FOREIGN KEY (tournament) REFERENCES tournament(id);

ALTER TABLE ONLY tournament
    ADD CONSTRAINT tournament_owner_fkey FOREIGN KEY (owner) REFERENCES trader_user(id);
