using System;

namespace MtgPairings.Domain
{
    public class UploadEvent
    {
        public enum Type {Tournament, DeleteTournament, ResetTournament, Name, Teams, Seatings, Pairings, Results, Pods, Round};

        public Action UploadAction { get; private set; }
        public Tournament Tournament { get; private set; }
        public Type UploadType { get; private set; }
        public int Round { get; private set; }

        public UploadEvent(Action uploadAction, Tournament tournament, Type uploadType, int round)
        {
            this.UploadAction = uploadAction;
            this.Tournament = tournament;
            this.UploadType = uploadType;
            this.Round = round;
        }
    }
}
