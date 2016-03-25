using System.Collections.Immutable;
using System.Linq;

namespace MtgPairings.Domain
{
    public class Pod
    {
        public ImmutableList<Seat> Seats { get; private set; }
        public int Number { get; private set; }
        public Pod(int number, ImmutableList<Seat> seats)
        {
            this.Number = number;
            this.Seats = seats;
        }

        public override bool Equals(object obj)
        {
            return this.Equals(obj as Pod);
        }

        public bool Equals(Pod other)
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
                       this.Seats.SequenceEqual(other.Seats);
            }
        }
    }
}
