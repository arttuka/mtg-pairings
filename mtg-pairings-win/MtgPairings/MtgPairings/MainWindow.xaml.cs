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

namespace MtgPairings
{
    /// <summary>
    /// Interaction logic for MainWindow.xaml
    /// </summary>
    public partial class MainWindow : Window
    {
        public List<TrackableTournament> Tournaments { get; private set; }
        public List<UploadEvent> Events { get; private set; }
        public BlockingCollection<UploadEvent> UploadQueue { get; private set; }
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
            Events = new List<UploadEvent>();
            UploadQueue = new BlockingCollection<UploadEvent>();
            _worker = new UploadWorker(this.UploadQueue);
            _workerThread = new Thread(_worker.DoUpload);
            _workerThread.IsBackground = true;
            _workerThread.Start();
            Tournaments = _reader.getAllTournaments().Select(t => new TrackableTournament(t)).ToList();
            foreach (Tournament t in Tournaments.Select(t => t.Tournament))
            {
                Console.WriteLine(t.TournamentId + ": " + t.Name);
            }
            new CheckTournamentsDelegate(CheckTournaments).BeginInvoke(null, null);
        }

        public void CheckTournaments()
        {
            Console.WriteLine("Checking tournaments...");
            //ImmutableHashSet<string> currentTournaments = (from t in Tournaments
            //                                               select t.Tournament.SanctionNumber).ToImmutableHashSet();
            //ImmutableHashSet<string> trackedTournaments = (from t in Tournaments
            //                                               where t.Tracking
            //                                               select t.Tournament.SanctionNumber).ToImmutableHashSet();
            //ImmutableList<TrackableTournament> newTournaments = (from t in _reader.getAllTournaments()
            //                                                     where !currentTournaments.Contains(t.SanctionNumber)
            //                                                     select new TrackableTournament(t)).ToImmutableList();

            foreach (TrackableTournament t in Tournaments.Where(t => t.Tracking))
            {
                Console.WriteLine("Checking " + t.Tournament.Name);
                Tournament oldTournament = t.Tournament;
                Tournament newTournament = _reader.getTournament(t.Tournament.TournamentId);
                if (!t.TournamentUploaded)
                {
                    UploadEvent e = new UploadEvent(() => _uploader.UploadTournament(newTournament), t.AutoUpload, newTournament, UploadEvent.Type.Tournament, 0);
                    UploadQueue.Add(e);
                    Events.Add(e);
                    t.TournamentUploaded = true;
                }
                if (!oldTournament.Equals(newTournament))
                {
                    Boolean uploadAll = false;
                    if (!oldTournament.Teams.SequenceEqual(newTournament.Teams))
                    {
                        UploadEvent e = new UploadEvent(() => _uploader.UploadTeams(newTournament.SanctionNumber, newTournament.Teams),
                                                        t.AutoUpload, newTournament, UploadEvent.Type.Teams, 0);
                        UploadQueue.Add(e);
                        Events.Add(e);
                        uploadAll = true;
                    }
                    if (!oldTournament.Seatings.SequenceEqual(newTournament.Seatings) || !newTournament.Seatings.IsEmpty && uploadAll)
                    {
                        UploadEvent e = new UploadEvent(() => _uploader.UploadSeatings(newTournament.SanctionNumber, newTournament.Seatings),
                                                        t.AutoUpload, newTournament, UploadEvent.Type.Seatings, 0);
                        UploadQueue.Add(e);
                        Events.Add(e);
                    }
                    if (!oldTournament.Rounds.SequenceEqual(newTournament.Rounds) || !newTournament.Rounds.IsEmpty && uploadAll)
                    {
                        foreach (var round in oldTournament.Rounds.ZipAll(newTournament.Rounds, (r1, r2) => new { OldRound = r1, NewRound = r2 }))
                        {
                            if (round.OldRound == null || !round.OldRound.Pairings.SequenceEqual(round.NewRound.Pairings) || round.NewRound != null && uploadAll)
                            {
                                UploadEvent e = new UploadEvent(() => _uploader.UploadPairings(newTournament.SanctionNumber, round.NewRound.Number, round.NewRound.Pairings),
                                                                t.AutoUpload, newTournament, UploadEvent.Type.Pairings, round.NewRound.Number);
                                UploadQueue.Add(e);
                                Events.Add(e);
                                uploadAll = true;
                            }
                            if (round.OldRound == null || !round.OldRound.Pairings.Select(p => p.Result).SequenceEqual(round.NewRound.Pairings.Select(p => p.Result)) || round.NewRound != null && uploadAll)
                            {
                                UploadEvent e = new UploadEvent(() => _uploader.UploadResults(newTournament.SanctionNumber, round.NewRound.Number, round.NewRound.Pairings),
                                                                t.AutoUpload, newTournament, UploadEvent.Type.Results, round.NewRound.Number);
                                UploadQueue.Add(e);
                                Events.Add(e);
                                uploadAll = true;
                            }
                        }
                    }
                    t.Tournament = newTournament;
                }
            }

            Thread.Sleep(1000);
            new CheckTournamentsDelegate(CheckTournaments).BeginInvoke(null, null);
        }
    }
}
