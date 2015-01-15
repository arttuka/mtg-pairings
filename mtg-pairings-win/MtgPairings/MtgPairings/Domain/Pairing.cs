using MtgPairings.Functional;

namespace MtgPairings.Domain
{
    class Pairing
    {
        public readonly int table;
        public readonly Team team1;
        public readonly Option<Team> team2;
        public readonly Option<Result> result;

        public Pairing(int table, Team team1, Option<Team> team2, Option<Result> result)
        {
            this.table = table;
            this.team1 = team1;
            this.team2 = team2;
            this.result = result;
        }
    }
}
