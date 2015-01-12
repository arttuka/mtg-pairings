namespace MtgPairings.Domain
{
    class Player
    {
        public readonly string dciNumber;
        public readonly string name;

        public Player(string dciNumber, string name)
        {
            this.dciNumber = dciNumber;
            this.name = name;
        }
    }
}
