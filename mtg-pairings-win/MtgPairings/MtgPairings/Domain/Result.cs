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

        public override bool Equals(object obj)
        {
            return this.Equals(obj as Result);
        }

        public bool Equals(Result other)
        {
            if (other == null)
            {
                return false;
            }
            else if (ReferenceEquals(other, this))
            {
                return true;
            }
            else
            {
                return this.Team1Wins == other.Team1Wins &&
                       this.Team2Wins == other.Team2Wins &&
                       this.Draws == other.Draws;
            }
        }
    }
}
