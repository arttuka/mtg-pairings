namespace MtgPairings.Domain
{
    public class Seat
    {
        public int Number { get; private set; }
        public Team Team { get; private set; }

        public Seat(int number, Team team)
        {
            this.Number = number;
            this.Team = team;
        }

        public override bool Equals(object obj)
        {
            return this.Equals(obj as Seat);
        }

        public bool Equals(Seat other)
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
                return this.Number == other.Number &&
                       this.Team.Equals(other.Team);
            }
        }
    }
}
