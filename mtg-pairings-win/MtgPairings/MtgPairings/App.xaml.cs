using System;
using System.Collections.Generic;
using System.Configuration;
using System.Data;
using System.Linq;
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
            var apiKey = Settings.Default.Apikey;
            Console.WriteLine("Api key: " + apiKey);
            if(apiKey == "")
            {
                apiKey = ShowApikeyDialog();
                Settings.Default.Apikey = apiKey;
                Settings.Default.Save();
            }
            Uploader uploader = new Uploader(Settings.Default.ServerURL, Settings.Default.Apikey);
            string dbPath = Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData) + "\\Wizards of the Coast\\Event Reporter\\TournamentData.dat";
            DatabaseReader reader = new DatabaseReader(dbPath);

            MainWindow window = new MainWindow(reader, uploader);
            window.Show();
        }

        private string ShowApikeyDialog()
        {
            Form dialog = new Form();
            dialog.Width = 500;
            dialog.Height = 150;
            dialog.Text = "Syötä API key";
            Label textLabel = new Label() { Width = 400, Left = 50, Top = 20, Text = "Syötä API key. Löydät sen osoitteesta http://mtgsuomi.fi/apikey." };
            TextBox inputBox = new TextBox() { Left = 50, Top = 50, Width = 400 };
            Button ok = new Button() { Text = "OK", Left = 350, Top = 80, Width = 100 };
            ok.Click += (sender, e) => { dialog.Close(); };
            dialog.Controls.Add(ok);
            dialog.Controls.Add(textLabel);
            dialog.Controls.Add(inputBox);
            dialog.FormBorderStyle = FormBorderStyle.FixedDialog;
            dialog.MinimizeBox = false;
            dialog.MaximizeBox = false;
            dialog.ShowDialog();
            return inputBox.Text;
        }
    }
}
