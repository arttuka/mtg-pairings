namespace MtgPairings.Domain
{
    public class Seating
    {
        public int Table { get; private set; }
        public Team Team { get; private set; }

        public Seating(int table, Team team)
        {
            Table = table;
            Team = team;
        }
    }
}
