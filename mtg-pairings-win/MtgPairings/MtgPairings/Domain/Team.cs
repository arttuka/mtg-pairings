using System.Collections.Immutable;

namespace MtgPairings.Domain
{
    class Team
    {
        public readonly int id;
        public readonly string name;
        public readonly ImmutableList<Player> players;

        public Team(int id, string name, ImmutableList<Player> players)
        {
            this.id = id;
            this.name = name;
            this.players = players;
        }
    }
}
