using System;
using System.Collections.Generic;
using System.Collections.Immutable;
using System.Linq;
using System.Data.OleDb;
using MtgPairings.Domain;

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
                t => new Team(t["Name"].ToString(), players[Convert.ToInt32(t["TeamId"])].ToImmutableList()),
                "SELECT Team.TeamId, Team.Name " +
                "FROM   Team " +
                "WHERE (Team.TournamentId = ?)",
                new object[] { tournamentId })
                .OrderBy(t => t.name)
                .ToImmutableList();
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
