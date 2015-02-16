namespace MtgPairings.Domain
{
    public class Player
    {
        public string DciNumber { get; private set; }
        public string Name { get; private set; }

        public Player(string dciNumber, string name)
        {
            DciNumber = dciNumber;
            Name = name;
        }

        public override bool Equals(object obj)
        {
            return this.Equals(obj as Player);
        }

        public bool Equals(Player other)
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
                return this.DciNumber == other.DciNumber &&
                       this.Name == other.Name;
            }
        }
    }
}
