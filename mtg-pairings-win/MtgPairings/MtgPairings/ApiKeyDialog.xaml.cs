using MtgPairings.Properties;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Shapes;

namespace MtgPairings
{
    /// <summary>
    /// Interaction logic for ApiKeyDialog.xaml
    /// </summary>
    public partial class ApiKeyDialog : Window
    {
        public ApiKeyDialog()
        {
            InitializeComponent();
            ApiKeyTextbox.Text = Settings.Default.Apikey;
        }

        public string ApiKey { get { return ApiKeyTextbox.Text; } }

        private void OkButton_Click(object sender, RoutedEventArgs e)
        {
            Close();
        }
    }
}
