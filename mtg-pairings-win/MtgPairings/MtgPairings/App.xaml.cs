using System;
using System.Net;
using System.Windows;
using MtgPairings.Data;
using MtgPairings.Properties;
using MtgPairings.Service;

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
            if(VersionChecker.IsNewVersionAvailable())
            {
                VersionDialog dialog = new VersionDialog();
                dialog.ShowDialog();
                Current.Shutdown();
                return;
            }
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
