using System;
using System.Collections.Generic;
using System.Configuration;
using System.Data;
using System.Linq;
using System.Net;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Forms;
using MtgPairings.Service;
using MtgPairings.Properties;

using MtgPairings.Data;
using MtgPairings.Domain;

namespace MtgPairings
{
    /// <summary>
    /// Interaction logic for App.xaml
    /// </summary>
    public partial class App : System.Windows.Application
    {
        private void ApplicationStartup(object sender, StartupEventArgs e)
        {
            ServicePointManager.SecurityProtocol = SecurityProtocolType.Tls12;
            Current.ShutdownMode = ShutdownMode.OnExplicitShutdown;
            if(Settings.Default.Apikey == "")
            {
                ApiKeyDialog dialog = new ApiKeyDialog();
                dialog.ShowDialog();
                Settings.Default.Apikey = dialog.ApiKey;
                Settings.Default.Save();
            }
            Current.ShutdownMode = ShutdownMode.OnMainWindowClose;
            Uploader uploader = new Uploader();
            string dbPath = Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData) + "\\Wizards of the Coast\\Event Reporter\\TournamentData.dat";
            DatabaseReader reader = new DatabaseReader(dbPath);

            MainWindow window = new MainWindow(reader, uploader);
            window.Show();
        }
    }
}
