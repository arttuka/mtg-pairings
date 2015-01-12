namespace MtgPairings.Domain
{
    class Result
    {
        public readonly int team1Wins;
        public readonly int team2Wins;
        public readonly int draws;

        public Result(int team1Wins, int team2Wins, int draws)
        {
            this.team1Wins = team1Wins;
            this.team2Wins = team2Wins;
            this.draws = draws;
        }
    }
}
