using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace MtgPairings.Domain
{
    public class TrackableTournament
    {
        public Boolean Tracking { get; set; }
        public Boolean AutoUpload { get; set; }
        public Tournament Tournament { get; set; }

        public TrackableTournament(Tournament t)
        {
            Tracking = false;
            AutoUpload = true;
            Tournament = t;
        }
    }
}
