using MtgPairings.Domain;
using MtgPairings.Functional;
using MtgPairings.Service;
using System;
using System.Collections.Generic;
using System.Collections.Immutable;
using System.Data.OleDb;
using System.Linq;

namespace MtgPairings.Data
{
    class DatabaseReader
    {
        private string connectionString;

        public DatabaseReader(string path)
        {
            connectionString = "Provider=Microsoft.Jet.OLEDB.4.0;Data Source=" + path;
        }

        private IEnumerable<T> OleDbFetch<T>(Func<OleDbDataReader, T> formatter, string query, object[] parameters)
        {
            using (var conn = new OleDbConnection(connectionString))
            {
                conn.Open();
                using (var cmd = new OleDbCommand(query, conn))
                {
                    int i = 0;
                    foreach (var p in parameters)
                    {
                        cmd.Parameters.AddWithValue("param" + i++, p);
                    }
                    using (var reader = cmd.ExecuteReader())
                    {
                        while (reader.Read())
                            yield return formatter(reader);
                    }
                }
            }
        }

        private IEnumerable<T> OleDbFetch<T>(Func<OleDbDataReader, T> formatter, string query)
        {
            return OleDbFetch(formatter, query, new object[0]);
        }

        private ImmutableList<TeamPlayer> getPlayersInTournament(int tournamentId)
        {
            return OleDbFetch(
                p => new TeamPlayer(new Player(p["PrimaryDciNumber"].ToString(), p["LastName"].ToString() + ", " + p["FirstName"].ToString()), Convert.ToInt32(p["TeamId"])),
                "SELECT Person.FirstName, Person.LastName, Person.PrimaryDciNumber, Team.TeamId " +
                "FROM   ((Person INNER JOIN " +
                "         TeamPlayers ON Person.PersonId = TeamPlayers.PersonId) INNER JOIN " +
                "         Team ON TeamPlayers.TeamId = Team.TeamId) " +
                "WHERE (Team.TournamentId = ?)",
                new object[] {tournamentId}).ToImmutableList();
        }

        private ImmutableList<Team> getTeamsInTournament(int tournamentId)
        {
            ILookup<int, Player> players = getPlayersInTournament(tournamentId).ToLookup(p => p.TeamId, p => p.Player);
            return OleDbFetch(
                t => new Team(Convert.ToInt32(t["TeamId"]), t["Name"].ToString(), players[Convert.ToInt32(t["TeamId"])].ToImmutableList()),
                "SELECT Team.TeamId, Team.Name " +
                "FROM   Team " +
                "WHERE (Team.TournamentId = ?)",
                new object[] { tournamentId })
                .OrderBy(t => t.Name)
                .ToImmutableList();
        }

        private Option<Result> resultFromData(int wins, int draws, int losses, bool bye, bool winByDrop, bool lossByDrop)
        {
            if (bye)
            {
                return Option.Of(new Result(2, 0, 0));
            }
            else if (winByDrop)
            {
                return Option.Of(new Result(2, 0, 0));
            }
            else if (lossByDrop)
            {
                return Option.Of(new Result(0, 2, 0));
            }
            else if(wins > -1 && draws > -1 && losses > -1)
            {
                return Option.Of(new Result(wins, losses, draws));
            }
            else
            {
                return Option<Result>.Empty;
            }
        }

        private ImmutableList<Round> getRoundsInTournament(int tournamentId)
        {
            var teams = getTeamsInTournament(tournamentId).ToImmutableDictionary(t => t.Id, t => t);

            var results = OleDbFetch(
                r => new {
                    match = new {
                        roundId = Convert.ToInt32(r["RoundId"]),
                        table = Convert.ToInt32(r["TableNumber"]),
                        matchId = Convert.ToInt32(r["MatchId"])},
                    teamId = Convert.ToInt32(r["TeamId"]),
                    wins = Convert.ToInt32(r["GameWins"]),
                    draws = Convert.ToInt32(r["GameDraws"]),
                    losses = Convert.ToInt32(r["GameLosses"]),
                    bye = Convert.ToBoolean(r["IsBye"]),
                    winByDrow = Convert.ToBoolean(r["WinByDrop"]),
                    lossByDrop = Convert.ToBoolean(r["LossByDrop"])},
                "SELECT        TeamMatchResult.*, [Match].RoundId, [Match].TableNumber " +
                "FROM            ((Round INNER JOIN " +
                "                 [Match] ON Round.RoundId = [Match].RoundId) INNER JOIN " +
                "                 TeamMatchResult ON [Match].MatchId = TeamMatchResult.MatchId) " +
                "WHERE        (Round.TournamentId = ?)",
                new object[] { tournamentId });
            
            var pairings = from r in results
                           orderby r.teamId
                           group r by r.match into m
                           let match = m.Key
                           let matchResults = m.ToList()
                           let result = m.First()
                           let team1 = teams[result.teamId]
                           let team2 = from res in matchResults.Get(1)
                                       select teams[res.teamId]
                           orderby match.table
                           select new {pairing = new Pairing(match.table,
                                                             team1,
                                                             team2,
                                                             resultFromData(result.wins, result.draws, result.losses,
                                                                            result.bye, result.winByDrow, result.lossByDrop)),
                                       roundId = match.roundId};

            var pairingsByRound = pairings.ToLookup(p => p.roundId, p => p.pairing);

            return OleDbFetch<Round>(
                r => new Round(Convert.ToInt32(r["Number"]), 
                               pairingsByRound[Convert.ToInt32(r["RoundId"])].ToImmutableList()),
                "SELECT Round.RoundId, Round.Number " +
                "FROM   Round " +
                "WHERE  (Round.TournamentId = ?)",
                new object[] {tournamentId}).OrderBy(r => r.Number).ToImmutableList();
        } 

        public Tournament getTournament(int tournamentId)
        {
            ImmutableList<Team> teams = getTeamsInTournament(tournamentId);
            ImmutableList<Round> rounds = getRoundsInTournament(tournamentId);
            return OleDbFetch(
                t => new Tournament(tournamentId,
                                    t["SanctionId"].ToString(),
                                    t["Title"].ToString(),
                                    Convert.ToInt32(t["NumberOfRounds"]),
                                    Convert.ToDateTime(t["StartDate"]).ToLocalDate(),
                                    rounds,
                                    teams),
                "SELECT SanctionId, Title, NumberOfRounds, StartDate FROM Tournament " +
                "WHERE (TournamentId = ?)",
                new object[] {tournamentId}).First();
        }

        public ImmutableList<Tournament> getAllTournaments()
        {
            return OleDbFetch<Tournament>(
                t => new Tournament(Convert.ToInt32(t["TournamentId"]),
                                    t["SanctionId"].ToString(),
                                    t["Title"].ToString(),
                                    Convert.ToInt32(t["NumberOfRounds"]),
                                    Convert.ToDateTime(t["StartDate"]).ToLocalDate(),
                                    ImmutableList<Round>.Empty,
                                    ImmutableList<Team>.Empty),
                "SELECT TournamentId, SanctionId, Title, NumberOfRounds FROM Tournament"
              ).ToImmutableList();
        }


    }
}
