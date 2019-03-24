using MtgPairings.Properties;
using RestSharp;
using System;
using System.Reflection;
using System.Diagnostics;

namespace MtgPairings.Service
{
    public class VersionChecker
    {
        class VersionResponse
        {
            public string version { get; set; }
        }

        public static bool IsNewVersionAvailable()
        {
            var client = new RestClient(Settings.Default.ServerURL);
            var request = new RestRequest(Method.GET);
            request.Resource = "/api/client-version";
            var response = client.Execute<VersionResponse>(request);
            if (response.StatusCode == System.Net.HttpStatusCode.OK)
            {
                var availableVersion = new Version(response.Data.version);
                var currentVersion = Assembly.GetExecutingAssembly().GetName().Version;
                return availableVersion.CompareTo(currentVersion) > 0;
            }
            else if (response.ErrorException != null)
            {
                throw response.ErrorException;
            }
            else
            {
                return false;
            }
        }
    }
}
