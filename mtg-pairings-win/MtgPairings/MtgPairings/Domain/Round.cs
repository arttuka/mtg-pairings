using System.Collections.Immutable;

namespace MtgPairings.Domain
{
    public class Round
    {
        public readonly int Number;
        public readonly ImmutableList<Pairing> Pairings;

        public Round(int number, ImmutableList<Pairing> pairings)
        {
            this.Number = number;
            this.Pairings = pairings;
        }
    }
}
