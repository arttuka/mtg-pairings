using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace MtgPairings.Domain
{
    class TeamPlayer
    {
        public readonly Player player;
        public readonly int teamId;

        public TeamPlayer(Player player, int teamId)
        {
            this.player = player;
            this.teamId = teamId;
        }
    }
}
