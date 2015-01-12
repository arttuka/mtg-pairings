using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Data;
using System.Data.OleDb;

namespace MtgPairings.Data
{
    class DatabaseReader
    {
        private string connectionString;

        public DatabaseReader(string path)
        {
            connectionString = "Provider=Microsoft.Jet.OLEDB.4.0;Data Source=" + path;
        }

        private static IEnumerable<T> OleDbFetch<T>(Func<OleDbDataReader, T> formatter, string connectionString, string query)
        {
            using (var conn = new OleDbConnection(connectionString))
            {
                conn.Open();
                using (var cmd = new OleDbCommand(query, conn))
                using (var reader = cmd.ExecuteReader())
                {
                    while (reader.Read())
                        yield return formatter(reader);
                }
            }
        }
    }
}
