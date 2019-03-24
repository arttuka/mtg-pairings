using System.Diagnostics;
using System.Windows;
using System.Windows.Navigation;

namespace MtgPairings
{
    /// <summary>
    /// Interaction logic for Page1.xaml
    /// </summary>
    public partial class VersionDialog : Window
    {
        public VersionDialog()
        {
            InitializeComponent();
        }

        private void Hyperlink_RequestNavigate(object sender, RequestNavigateEventArgs e)
        {
            Process.Start(new ProcessStartInfo(e.Uri.AbsoluteUri));
            e.Handled = true;
        }

        private void CloseButton_Click(object sender, RoutedEventArgs e)
        {
            Close();
        }
    }
}
