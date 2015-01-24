namespace MtgPairings.Domain
{
    public class Result
    {
        public readonly int Team1Wins;
        public readonly int Team2Wins;
        public readonly int Draws;

        public Result(int team1Wins, int team2Wins, int draws)
        {
            this.Team1Wins = team1Wins;
            this.Team2Wins = team2Wins;
            this.Draws = draws;
        }
    }
}
