using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace MtgPairings.Domain
{
    public class TrackableTournament : INotifyPropertyChanged
    {
        public Boolean Tracking { get; set; }
        public Boolean AutoUpload { get; set; }
        public Tournament Tournament { get; set; }
        public Boolean TournamentUploaded { get; set; }
        private string _name;
        public string Name
        {
            get { return _name; }
            set
            {
                _name = value;
                OnPropertyChanged("name");
            }
        }

        public TrackableTournament(Tournament t)
        {
            Tracking = false;
            AutoUpload = true;
            Tournament = t;
            TournamentUploaded = false;
            Name = t.Name;
        }

        public event PropertyChangedEventHandler PropertyChanged;

        private void OnPropertyChanged(string name)
        {
            var handler = PropertyChanged;
            if( handler != null)
            {
                handler(this, new PropertyChangedEventArgs(name));
            }
        }
    }
}
