using MtgPairings.Functional;

namespace MtgPairings.Domain
{
    public class Pairing
    {
        public int Table { get; private set; }
        public Team Team1 { get; private set; }
        public Option<Team> Team2 { get; private set; }
        public Option<Result> Result { get; private set; }

        public Pairing(int table, Team team1, Option<Team> team2, Option<Result> result)
        {
            Table = table;
            Team1 = team1;
            Team2 = team2;
            Result = result;
        }
    }
}
