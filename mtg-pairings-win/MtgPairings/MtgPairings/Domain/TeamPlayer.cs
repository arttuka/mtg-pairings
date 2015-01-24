namespace MtgPairings.Domain
{
    public class TeamPlayer
    {
        public Player Player { get; private set; }
        public int TeamId { get; private set; }

        public TeamPlayer(Player player, int teamId)
        {
            Player = player;
            TeamId = teamId;
        }
    }
}
