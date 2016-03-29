using System;
using System.Collections.Immutable;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Threading;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Navigation;
using System.Windows.Shapes;
using MtgPairings.Service;
using MtgPairings.Data;
using MtgPairings.Domain;
using System.Collections.Generic;
using System.Collections.Concurrent;
using System.ComponentModel;
using MtgPairings.Functional;
using System.Collections.ObjectModel;
using MtgPairings.Properties;

namespace MtgPairings
{
    /// <summary>
    /// Interaction logic for MainWindow.xaml
    /// </summary>
    public partial class MainWindow : Window
    {
        public ObservableCollection<TrackableTournament> Tournaments { get; private set; }
        public ObservableCollection<LogItem> Events { get; private set; }
        public ConcurrentQueue<UploadEvent> UploadQueue { get; private set; }
        private bool _activeOnly;
        public bool ActiveOnly
        {
            get { return _activeOnly; }
            set
            {
                if (value != _activeOnly)
                {
                    _activeOnly = value;
                    RefreshTournamentView();
                }
            }
        }
        private Object _eventLock = new Object();
        private DatabaseReader _reader;
        private Uploader _uploader;
        private UploadWorker _worker;
        private Thread _workerThread;

        public delegate void CheckTournamentsDelegate();

        public MainWindow(DatabaseReader reader, Uploader uploader)
        {
            InitializeComponent();
            this.DataContext = this;
            _reader = reader;
            _uploader = uploader;
            Events = new ObservableCollection<LogItem>();
            UploadQueue = new ConcurrentQueue<UploadEvent>();
            ActiveOnly = true;
            ((CollectionViewSource)this.Resources["FilteredTournaments"]).Filter += (sender, e) =>
            {
                TrackableTournament t = e.Item as TrackableTournament;
                e.Accepted = !ActiveOnly || t.Tournament.Active;
            };
            _worker = new UploadWorker(this.UploadQueue, this.AddEvent);
            _workerThread = new Thread(_worker.DoUpload);
            _workerThread.IsBackground = true;
            _workerThread.Start();
            Tournaments = new ObservableCollection<TrackableTournament>(from t in _reader.getAllTournaments()
                                                                        select new TrackableTournament(t));
            new CheckTournamentsDelegate(CheckTournaments).BeginInvoke(null, null);
        }

        private void RefreshTournamentView()
        {
            var view = ((CollectionViewSource)this.Resources["FilteredTournaments"]).View;
            if (view != null)
            {
                view.Refresh();
            }
        }

        private void AddEvent(LogItem i)
        {
            this.Dispatcher.Invoke(() =>
            {
                Events.Add(i);
                EventList.ScrollIntoView(EventList.Items[EventList.Items.Count - 1]);
            });
        }

        private void AddNewAndActiveTournaments()
        {
            this.Dispatcher.Invoke(() =>
            {
                var oldTournaments = Tournaments.ToDictionary(t => t.Tournament.TournamentId);
                var newTournaments = _reader.getAllTournaments();
                foreach(var tournament in (from t in newTournaments
                                           where !oldTournaments.ContainsKey(t.TournamentId)
                                           select new TrackableTournament(t)))
                {
                    Tournaments.Add(tournament);
                }
                foreach(var tournament in newTournaments)
                {
                    if(oldTournaments.ContainsKey(tournament.TournamentId))
                    {
                        var oldTournament = oldTournaments[tournament.TournamentId];
                        oldTournament.Tournament = oldTournament.Tournament.WithActive(tournament.Active);
                    }
                }
                RefreshTournamentView();
            });
        }

        public void CheckTournaments()
        {
            AddNewAndActiveTournaments();
            foreach (TrackableTournament t in Tournaments.Where(t => t.Tracking))
            {
                Tournament oldTournament = t.Tournament;
                Tournament newTournament = _reader.getTournament(t.Tournament.TournamentId).WithName(t.Name);
                if (!t.TournamentUploaded)
                {
                    UploadEvent e = new UploadEvent(() => _uploader.UploadTournament(newTournament), t.AutoUpload, newTournament, UploadEvent.Type.Tournament, 0);
                    UploadQueue.Enqueue(e);
                    t.TournamentUploaded = true;
                }
                if (!oldTournament.Equals(newTournament))
                {
                    Boolean uploadAll = false;
                    if (!oldTournament.Teams.SequenceEqual(newTournament.Teams))
                    {
                        UploadEvent e = new UploadEvent(() => _uploader.UploadTeams(newTournament.SanctionNumber, newTournament.Teams),
                                                        t.AutoUpload, newTournament, UploadEvent.Type.Teams, 0);
                        UploadQueue.Enqueue(e);
                        uploadAll = true;
                    }
                    if (!oldTournament.Seatings.SequenceEqual(newTournament.Seatings) || !newTournament.Seatings.IsEmpty && uploadAll)
                    {
                        UploadEvent e = new UploadEvent(() => _uploader.UploadSeatings(newTournament.SanctionNumber, newTournament.Seatings),
                                                        t.AutoUpload, newTournament, UploadEvent.Type.Seatings, 0);
                        UploadQueue.Enqueue(e);
                    }
                    if (!oldTournament.Pods.SequenceEqual(newTournament.Pods) || !newTournament.Pods.IsEmpty && uploadAll)
                    {
                        UploadEvent e = new UploadEvent(() => _uploader.UploadPods(newTournament.SanctionNumber, newTournament.Pods),
                                                        t.AutoUpload, newTournament, UploadEvent.Type.Pods, 0);
                        UploadQueue.Enqueue(e);
                    }
                    if (!oldTournament.Rounds.SequenceEqual(newTournament.Rounds) || !newTournament.Rounds.IsEmpty && uploadAll)
                    {
                        foreach (var round in oldTournament.Rounds.ZipAll(newTournament.Rounds, (r1, r2) => new { OldRound = r1, NewRound = r2 }))
                        {
                            if ( round.NewRound == null)
                            {
                                UploadEvent e = new UploadEvent(() => _uploader.DeleteRound(newTournament.SanctionNumber, round.OldRound.Number),
                                                                t.AutoUpload, newTournament, UploadEvent.Type.Round, round.OldRound.Number);
                                UploadQueue.Enqueue(e);
                            }
                            else
                            {
                                if (round.OldRound == null || round.NewRound == null || !round.OldRound.Pairings.SequenceEqual(round.NewRound.Pairings, new Pairing.PairingEqualityComparer()) || round.NewRound != null && uploadAll)
                                {
                                    UploadEvent e = new UploadEvent(() => _uploader.UploadPairings(newTournament.SanctionNumber, round.NewRound.Number, round.NewRound.Pairings),
                                                                    t.AutoUpload, newTournament, UploadEvent.Type.Pairings, round.NewRound.Number);
                                    UploadQueue.Enqueue(e);
                                    uploadAll = true;
                                }
                                if (round.OldRound == null || !round.OldRound.Pairings.Select(p => p.Result).SequenceEqual(round.NewRound.Pairings.Select(p => p.Result)) || round.NewRound != null && uploadAll)
                                {
                                    UploadEvent e = new UploadEvent(() => _uploader.UploadResults(newTournament.SanctionNumber, round.NewRound.Number, round.NewRound.Pairings),
                                                                    t.AutoUpload, newTournament, UploadEvent.Type.Results, round.NewRound.Number);
                                    UploadQueue.Enqueue(e);
                                    uploadAll = true;
                                }

                            }
                        }
                    }
                    t.Tournament = newTournament;
                }
            }

            Thread.Sleep(5000);
            new CheckTournamentsDelegate(CheckTournaments).BeginInvoke(null, null);
        }

        private void MenuItem_Click(object sender, RoutedEventArgs e)
        {
            ApiKeyDialog dialog = new ApiKeyDialog();
            dialog.ShowDialog();
            Settings.Default.Apikey = dialog.ApiKey;
            Settings.Default.Save();
        }

        private void Tallenna_Click(object sender, RoutedEventArgs e)
        {
            var tournament = (TrackableTournament)TournamentList.SelectedItem;
            if (tournament.Tracking && tournament.Name != tournament.Tournament.Name)
            {
                UploadEvent ev = new UploadEvent(() => _uploader.UploadName(tournament.Tournament.SanctionNumber, tournament.Name),
                                                 tournament.AutoUpload, tournament.Tournament.WithName(tournament.Name), UploadEvent.Type.Name, 0);
                UploadQueue.Enqueue(ev);
                tournament.Tournament = tournament.Tournament.WithName(tournament.Name);
            }
        }
    }
}
