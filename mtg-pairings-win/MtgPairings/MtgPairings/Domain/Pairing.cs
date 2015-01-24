using MtgPairings.Functional;

namespace MtgPairings.Domain
{
    public class Pairing
    {
        public readonly int Table;
        public readonly Team Team1;
        public readonly Option<Team> Team2;
        public readonly Option<Result> Result;

        public Pairing(int table, Team team1, Option<Team> team2, Option<Result> result)
        {
            this.Table = table;
            this.Team1 = team1;
            this.Team2 = team2;
            this.Result = result;
        }
    }
}
