namespace MtgPairings.Domain
{
    public class Result
    {
        public int Team1Wins { get; private set; }
        public int Team2Wins { get; private set; }
        public int Draws { get; private set; }

        public Result(int team1Wins, int team2Wins, int draws)
        {
            Team1Wins = team1Wins;
            Team2Wins = team2Wins;
            Draws = draws;
        }
    }
}
