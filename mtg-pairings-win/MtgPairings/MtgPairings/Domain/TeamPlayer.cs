using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace MtgPairings.Domain
{
    class TeamPlayer
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
