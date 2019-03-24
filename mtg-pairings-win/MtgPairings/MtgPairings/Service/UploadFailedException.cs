using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Net;

namespace MtgPairings.Service
{
    public class UploadFailedException: System.Exception
    {
        public HttpStatusCode Status { get; private set; }
        public string Content { get; private set; }

        public UploadFailedException()
        {
        }

        public UploadFailedException(HttpStatusCode status, string content)
            : base(status + " " + content)
        {
            this.Status = status;
            this.Content = content;
        }
    }
}
