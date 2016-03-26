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

        public UploadFailedException()
        {
        }

        public UploadFailedException(HttpStatusCode status, string message)
            : base(status + " " + message)
        {
        }
    }
}
