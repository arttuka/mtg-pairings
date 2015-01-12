using System.Collections.Immutable;

namespace MtgPairings.Domain
{
    class Round
    {
        public readonly int number;
        public readonly ImmutableList<Pairing> pairings;

        public Round(int number, ImmutableList<Pairing> pairings)
        {
            this.number = number;
            this.pairings = pairings;
        }
    }
}
