namespace MtgPairings.Domain
{
    class Seating
    {
        public readonly int table;
        public readonly Team team;

        public Seating(int table, Team team)
        {
            this.table = table;
            this.team = team;
        }
    }
}
