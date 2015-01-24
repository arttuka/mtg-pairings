namespace MtgPairings.Domain
{
    public class Seating
    {
        public readonly int Table;
        public readonly Team Team;

        public Seating(int table, Team team)
        {
            this.Table = table;
            this.Team = team;
        }
    }
}
