using System.Collections.Immutable;
using System.Linq;

namespace MtgPairings.Domain
{
    public class PodRound
    {
        public ImmutableList<Pod> Pods { get; private set; }
        public int RoundNumber { get; private set; }

        public PodRound(ImmutableList<Pod> pods, int roundNumber)
        {
            this.Pods = pods;
            this.RoundNumber = roundNumber;
        }

        public override bool Equals(object obj)
        {
            return this.Equals(obj as PodRound);
        }

        public bool Equals(PodRound other)
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
                return this.RoundNumber == other.RoundNumber && this.Pods.SequenceEqual(other.Pods);
            }
        }
    }
}
