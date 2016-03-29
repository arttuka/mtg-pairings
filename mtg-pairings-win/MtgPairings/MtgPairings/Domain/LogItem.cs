using System;

namespace MtgPairings.Domain
{
    public class LogItem
    {
        public string Message { get; private set; }
        public DateTime Time { get; private set; }
        public LogItem(string message)
        {
            Message = message;
            Time = DateTime.Now;
        }
    }
}
