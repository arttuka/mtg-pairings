namespace MtgPairings.Domain
{
    class Pairing
    {
        public readonly int table;
        public readonly Team team1;
        public readonly Team team2;
        public readonly Result result;

        public Pairing(int table, Team team1, Team team2, Result result)
        {
            this.table = table;
            this.team1 = team1;
            this.team2 = team2;
            this.result = result;
        }
    }
}
