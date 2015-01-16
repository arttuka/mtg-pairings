namespace MtgPairings.Domain
{
    class Player
    {
        public readonly string DciNumber;
        public readonly string Name;

        public Player(string dciNumber, string name)
        {
            this.DciNumber = dciNumber;
            this.Name = name;
        }
    }
}
