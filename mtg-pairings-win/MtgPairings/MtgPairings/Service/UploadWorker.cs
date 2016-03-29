using System;
using System.Threading;
using MtgPairings.Domain;
using System.Collections.Concurrent;

namespace MtgPairings.Service
{
    public class UploadWorker
    {
        private volatile Boolean _running;
        public Boolean Running { get { return _running; } set { _running = value; } }
        private ConcurrentQueue<UploadEvent> _events;
        private Action<LogItem> _addEvent;
        
        public UploadWorker(ConcurrentQueue<UploadEvent> events, Action<LogItem> addEvent) {
            this._events = events;
            this._addEvent = addEvent;
            this.Running = true;
        }

        private LogItem SuccessEvent(UploadEvent e)
        {
            switch (e.UploadType)
            {
                case UploadEvent.Type.Tournament:
                    return new LogItem("Turnaus " + e.Tournament.Name + " lähetetty.");
                case UploadEvent.Type.Teams:
                    return new LogItem("Turnauksen " + e.Tournament.Name + " tiimit lähetetty.");
                case UploadEvent.Type.Seatings:
                    return new LogItem("Turnauksen " + e.Tournament.Name + " seatingit lähetetty.");
                case UploadEvent.Type.Pairings:
                    return new LogItem("Turnauksen " + e.Tournament.Name + " pairingit " + e.Round + " lähetetty.");
                case UploadEvent.Type.Results:
                    return new LogItem("Turnauksen " + e.Tournament.Name + " tulokset " + e.Round + " lähetetty.");
                case UploadEvent.Type.Pods:
                    return new LogItem("Turnauksen " + e.Tournament.Name + " podit lähetetty");
                default:
                    return new LogItem("");
            }
        }

        public void DoUpload() {
            while(Running) {
                UploadEvent e;
                if (_events.TryPeek(out e))
                {
                    try
                    {
                        e.UploadAction();
                        _addEvent(SuccessEvent(e));
                        _events.TryDequeue(out e);
                    }
                    catch (UploadFailedException ex)
                    {
                        _addEvent(new LogItem(ex.Message));
                        Thread.Sleep(10000);
                    }
                    catch (System.Net.WebException)
                    {
                        _addEvent(new LogItem("Ei yhteyttä palvelimeen"));
                        Thread.Sleep(10000);
                    }
                }
                else
                {
                    Thread.Sleep(1000);
                }
                
            }
        }
    }
}
