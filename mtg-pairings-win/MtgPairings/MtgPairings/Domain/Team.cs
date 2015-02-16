using System.Collections.Immutable;
using System.Linq;

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

        public override bool Equals(object obj)
        {
            return this.Equals(obj as Team);
        }

        public bool Equals(Team other)
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
                return this.Id == other.Id &&
                       this.Name == other.Name &&
                       this.Players.SequenceEqual(other.Players);
            }
        }
    }
}
