using System.Collections.Immutable;
using System.Linq;

namespace MtgPairings.Domain
{
    public class Round
    {
        public int Number { get; private set; }
        public ImmutableList<Pairing> Pairings { get; private set; }
        public bool Playoff { get; private set; }

        public Round(int number, bool playoff, ImmutableList<Pairing> pairings)
        {
            Number = number;
            Playoff = playoff;
            Pairings = pairings;
        }

        public override bool Equals(object obj)
        {
            return this.Equals(obj as Round);
        }

        public bool Equals(Round other)
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
                       this.Playoff == other.Playoff &&
                       this.Pairings.SequenceEqual(other.Pairings);
            }
        }
    }
}
