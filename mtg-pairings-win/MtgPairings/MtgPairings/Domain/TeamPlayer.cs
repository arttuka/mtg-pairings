namespace MtgPairings.Domain
{
    public class TeamPlayer
    {
        public readonly Player Player;
        public readonly int TeamId;

        public TeamPlayer(Player player, int teamId)
        {
            this.Player = player;
            this.TeamId = teamId;
        }
    }
}
