using System;

namespace MtgPairings.Domain
{
    public class UploadEvent
    {
        private Action p;
        private bool autoUpload;
        private Tournament newTournament;
        private int number;

        public enum Type {Tournament, Name, Teams, Seatings, Pairings, Results, Pods};

        public Action UploadAction { get; private set; }
        public Boolean Upload { get; set; }
        public Tournament Tournament { get; private set; }
        public Type UploadType { get; private set; }
        public int Round { get; private set; }

        public UploadEvent(Action uploadAction, Boolean upload, Tournament tournament, Type uploadType, int round)
        {
            this.UploadAction = uploadAction;
            this.Upload = upload;
            this.Tournament = tournament;
            this.UploadType = uploadType;
            this.Round = round;
        }

        public UploadEvent(Action p, bool autoUpload, Tournament newTournament, int number)
        {
            this.p = p;
            this.autoUpload = autoUpload;
            this.newTournament = newTournament;
            this.number = number;
        }
    }
}
