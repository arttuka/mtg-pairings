using System.Collections.Immutable;

namespace MtgPairings.Domain
{
    class Team
    {
        public readonly int Id;
        public readonly string Name;
        public readonly ImmutableList<Player> Players;

        public Team(int id, string name, ImmutableList<Player> players)
        {
            this.Id = id;
            this.Name = name;
            this.Players = players;
        }
    }
}
