using System;
using System.Collections.Generic;
using System.Configuration;
using System.Data;
using System.Linq;
using System.Threading.Tasks;
using System.Windows;
using MtgPairings.Service;
using MtgPairings.Properties;

using MtgPairings.Data;
using MtgPairings.Domain;

namespace MtgPairings
{
    /// <summary>
    /// Interaction logic for App.xaml
    /// </summary>
    public partial class App : Application
    {
        private void ApplicationStartup(object sender, StartupEventArgs e)
        {
            Uploader uploader = new Uploader(Settings.Default.ServerURL, Settings.Default.Apikey);
            string dbPath = Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData) + "\\Wizards of the Coast\\Event Reporter\\TournamentData.dat";
            DatabaseReader reader = new DatabaseReader(dbPath);

            MainWindow window = new MainWindow(reader, uploader);
            window.Show();
        }
    }
}
