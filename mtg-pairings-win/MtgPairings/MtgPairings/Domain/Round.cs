using System.Collections.Immutable;

namespace MtgPairings.Domain
{
    public class Round
    {
        public int Number { get; private set; }
        public ImmutableList<Pairing> Pairings { get; private set; }

        public Round(int number, ImmutableList<Pairing> pairings)
        {
            Number = number;
            Pairings = pairings;
        }
    }
}
