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
        
        public UploadWorker(ConcurrentQueue<UploadEvent> events) {
            this._events = events;
            this.Running = true;
        }

        public void DoUpload() {
            while(Running) {
                UploadEvent e;
                if (_events.TryPeek(out e))
                {
                    e.UploadAction();
                    Console.WriteLine("Uploaded " + e.UploadType + " for tournament " + e.Tournament.SanctionNumber);
                    _events.TryDequeue(out e);
                }
                else
                {
                    Thread.Sleep(1000);
                }
                
            }
        }
    }
}
