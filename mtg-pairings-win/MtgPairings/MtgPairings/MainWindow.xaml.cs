﻿using System;
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
        public List<TrackableTournament> Tournaments { get; private set; }
        public ObservableCollection<UploadEvent> Events { get; private set; }
        public ConcurrentQueue<UploadEvent> UploadQueue { get; private set; }
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
            Events = new ObservableCollection<UploadEvent>();
            UploadQueue = new ConcurrentQueue<UploadEvent>();
            _worker = new UploadWorker(this.UploadQueue);
            _workerThread = new Thread(_worker.DoUpload);
            _workerThread.IsBackground = true;
            _workerThread.Start();
            Tournaments = _reader.getAllTournaments().Select(t => new TrackableTournament(t)).ToList();
            new CheckTournamentsDelegate(CheckTournaments).BeginInvoke(null, null);
        }

        private void AddEvent(UploadEvent e)
        {
            this.Dispatcher.Invoke(() =>
            {
                Events.Add(e);
                EventList.ScrollIntoView(EventList.Items[EventList.Items.Count - 1]);
            });
        }

        public void CheckTournaments()
        {
            foreach (TrackableTournament t in Tournaments.Where(t => t.Tracking))
            {
                Tournament oldTournament = t.Tournament;
                Tournament newTournament = _reader.getTournament(t.Tournament.TournamentId);
                if (!t.TournamentUploaded)
                {
                    UploadEvent e = new UploadEvent(() => _uploader.UploadTournament(newTournament), t.AutoUpload, newTournament, UploadEvent.Type.Tournament, 0);
                    UploadQueue.Enqueue(e);
                    AddEvent(e);
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
                        AddEvent(e);
                        uploadAll = true;
                    }
                    if (!oldTournament.Seatings.SequenceEqual(newTournament.Seatings) || !newTournament.Seatings.IsEmpty && uploadAll)
                    {
                        UploadEvent e = new UploadEvent(() => _uploader.UploadSeatings(newTournament.SanctionNumber, newTournament.Seatings),
                                                        t.AutoUpload, newTournament, UploadEvent.Type.Seatings, 0);
                        UploadQueue.Enqueue(e);
                        AddEvent(e);
                    }
                    if (!oldTournament.Pods.SequenceEqual(newTournament.Pods) || !newTournament.Pods.IsEmpty && uploadAll)
                    {
                        UploadEvent e = new UploadEvent(() => _uploader.UploadPods(newTournament.SanctionNumber, newTournament.Pods),
                                                        t.AutoUpload, newTournament, UploadEvent.Type.Pods, 0);
                        UploadQueue.Enqueue(e);
                        AddEvent(e);
                    }
                    if (!oldTournament.Rounds.SequenceEqual(newTournament.Rounds) || !newTournament.Rounds.IsEmpty && uploadAll)
                    {
                        foreach (var round in oldTournament.Rounds.ZipAll(newTournament.Rounds, (r1, r2) => new { OldRound = r1, NewRound = r2 }))
                        {
                            if ( round.NewRound == null)
                            {
                                UploadEvent e = new UploadEvent(() => _uploader.DeleteRound(newTournament.SanctionNumber, round.OldRound.Number), t.AutoUpload, newTournament, round.OldRound.Number);
                            }
                            else
                            {
                                if (round.OldRound == null || round.NewRound == null || !round.OldRound.Pairings.SequenceEqual(round.NewRound.Pairings) || round.NewRound != null && uploadAll)
                                {
                                    UploadEvent e = new UploadEvent(() => _uploader.UploadPairings(newTournament.SanctionNumber, round.NewRound.Number, round.NewRound.Pairings),
                                                                    t.AutoUpload, newTournament, UploadEvent.Type.Pairings, round.NewRound.Number);
                                    UploadQueue.Enqueue(e);
                                    AddEvent(e);
                                    uploadAll = true;
                                }
                                if (round.OldRound == null || !round.OldRound.Pairings.Select(p => p.Result).SequenceEqual(round.NewRound.Pairings.Select(p => p.Result)) || round.NewRound != null && uploadAll)
                                {
                                    UploadEvent e = new UploadEvent(() => _uploader.UploadResults(newTournament.SanctionNumber, round.NewRound.Number, round.NewRound.Pairings),
                                                                    t.AutoUpload, newTournament, UploadEvent.Type.Results, round.NewRound.Number);
                                    UploadQueue.Enqueue(e);
                                    AddEvent(e);
                                    uploadAll = true;
                                }

                            }
                        }
                    }
                    t.Tournament = newTournament;
                }
            }

            Thread.Sleep(1000);
            new CheckTournamentsDelegate(CheckTournaments).BeginInvoke(null, null);
        }

        private void MenuItem_Click(object sender, RoutedEventArgs e)
        {
            ApiKeyDialog dialog = new ApiKeyDialog();
            dialog.ShowDialog();
            Settings.Default.Apikey = dialog.ApiKey;
            Settings.Default.Save();
        }
    }
}
