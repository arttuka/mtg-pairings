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
    }
}
