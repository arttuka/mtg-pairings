using System.Collections.Immutable;

namespace MtgPairings.Domain
{
    public class Team
    {
        public int Id { get; private set; }
        public string Name { get; private set; }
        public ImmutableList<Player> Players { get; private set; }

        public Team(int id, string name, ImmutableList<Player> players)
        {
            Id = id;
            Name = name;
            Players = players;
        }
    }
}
