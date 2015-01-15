using System;
using System.Collections.Generic;
using System.Collections.Immutable;
using System.Linq;
using System.Data.OleDb;
using MtgPairings.Domain;
using MtgPairings.Service;

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

        public ImmutableList<TeamPlayer> getPlayersInTournament(int tournamentId)
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

        public ImmutableList<Team> getTeamsInTournament(int tournamentId)
        {
            ILookup<int, Player> players = getPlayersInTournament(tournamentId).ToLookup(p => p.teamId, p => p.player);
            return OleDbFetch(
                t => new Team(Convert.ToInt32(t["TeamId"]), t["Name"].ToString(), players[Convert.ToInt32(t["TeamId"])].ToImmutableList()),
                "SELECT Team.TeamId, Team.Name " +
                "FROM   Team " +
                "WHERE (Team.TournamentId = ?)",
                new object[] { tournamentId })
                .OrderBy(t => t.name)
                .ToImmutableList();
        }

        private Result resultFromData(int wins, int draws, int losses, bool bye, bool winByDrop, bool lossByDrop)
        {
            if (bye)
            {
                return new Result(2, 0, 0);
            }
            else if (winByDrop)
            {
                return new Result(2, 0, 0);
            }
            else if (lossByDrop)
            {
                return new Result(0, 2, 0);
            }
            else
            {
                return new Result(wins, losses, draws);
            }
        }

        public ImmutableList<Round> getRoundsInTournament(int tournamentId)
        {
            var teams = getTeamsInTournament(tournamentId).ToImmutableDictionary(t => t.id, t => t);

            var results = OleDbFetch(
                r => new { match = new { roundId = Convert.ToInt32(r["RoundId"]),
                                         table = Convert.ToInt32(r["TableNumber"]),
                                         matchId = Convert.ToInt32(r["MatchId"])},
                           teamId = Convert.ToInt32(r["TeamId"]),
                           wins = Convert.ToInt32(r["GameWins"]),
                           draws = Convert.ToInt32(r["GameDraws"]),
                           losses = Convert.ToInt32(r["GameLosses"]),
                           bye = Convert.ToBoolean(r["IsBye"]),
                           winByDrow = Convert.ToBoolean(r["WinByDrop"]),
                           lossByDrop = Convert.ToBoolean(r["LossByDrop"])},
                "SELECT       Match.RoundId, Match.TableNumber, Match.MatchId, TeamMatchResult.* " +
                "FROM         ((Round INNER JOIN " +
                "               Match ON Round.RoundId = Match.RoundId) INNER JOIN " +
                "               TeamMatchResult ON Match.MatchId = TeamMatchResult.MatchId " +
                "WHERE        (Round.TournamentId = ?)) ");
            
            var pairings = from r in results
                           orderby r.teamId
                           group r by r.match into m
                           let match = m.Key
                           let matchResults = m.ToList()
                           let result = m.First()
                           let team1 = teams[result.teamId]
                           let team2 = matchResults.Get(1).Select(r => teams[r.teamId]).ValueOrElse(null)
                           select new Pairing(match.table,
                                              team1,
                                              team2,
                                              resultFromData(result.wins, result.draws, result.losses,
                                                             result.bye, result.winByDrow, result.lossByDrop));



            return OleDbFetch<Round>(
                r => null,
                "SELECT       Match.RoundId, Match.TableNumber, Round.Number " +
                "FROM         (Round INNER JOIN " +
                "              Match ON Round.RoundId = Match.RoundId) " +
                "WHERE        (Round.TournamentId = ?)").ToImmutableList();
        } 

        public Tournament getTournament(int tournamentId)
        {
            ImmutableList<Team> teams = getTeamsInTournament(tournamentId);
            return OleDbFetch(
                t => new Tournament(tournamentId,
                                    t["SanctionId"].ToString(),
                                    t["Title"].ToString(),
                                    Convert.ToInt32(t["NumberOfRounds"]),
                                    ImmutableList<Round>.Empty,
                                    teams),
                "SELECT SanctionId, Title, NumberOfRounds FROM Tournament " +
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
                                    ImmutableList<Round>.Empty,
                                    ImmutableList<Team>.Empty),
                "SELECT TournamentId, SanctionId, Title, NumberOfRounds FROM Tournament"
              ).ToImmutableList();
        }


    }
}
