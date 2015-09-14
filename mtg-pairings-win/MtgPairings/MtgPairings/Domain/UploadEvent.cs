using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace MtgPairings.Domain
{
    public class UploadEvent
    {
        public enum Type {Tournament, Teams, Seatings, Pairings, Results};

        public Action UploadAction { get; private set; }
        public Boolean Upload { get; set; }
        public Tournament Tournament { get; private set; }
        public Type UploadType { get; private set; }
        public int Round { get; private set; }
        public string EventText
        {
            get
            {
                switch(UploadType) {
                    case Type.Tournament:
                        return "Turnaus " + Tournament.Name + " lähetetty.";
                    case Type.Teams:
                        return "Turnauksen " + Tournament.Name + " tiimit lähetetty.";
                    case Type.Seatings:
                        return "Turnauksen " + Tournament.Name + " seatingit lähetetty.";
                    case Type.Pairings:
                        return "Turnauksen " + Tournament.Name + " pairingit " + Round + " lähetetty.";
                    case Type.Results:
                        return "Turnauksen " + Tournament.Name + " tulokset " + Round + " lähetetty.";
                    default:
                        return "";
                }
            }
        }

        public UploadEvent(Action uploadAction, Boolean upload, Tournament tournament, Type uploadType, int round)
        {
            this.UploadAction = uploadAction;
            this.Upload = upload;
            this.Tournament = tournament;
            this.UploadType = uploadType;
            this.Round = round;
        }
    }
}
