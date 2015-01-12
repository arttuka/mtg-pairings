using System.Collections.Immutable;

namespace MtgPairings.Domain
{
    class Team
    {
        public readonly string name;
        public readonly ImmutableList<Player> players;

        public Team(string name, ImmutableList<Player> players)
        {
            this.name = name;
            this.players = players;
        }
    }
}
