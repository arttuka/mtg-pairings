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

        public override bool Equals(object obj)
        {
            return this.Equals(obj as Seating);
        }

        public bool Equals(Seating other)
        {
            if (other == null)
            {
                return false;
            }
            else if (ReferenceEquals(other, this))
            {
                return true;
            }
            else
            {
                return this.Table == other.Table &&
                       this.Team.Equals(other.Team);
            }
        }
    }
}
