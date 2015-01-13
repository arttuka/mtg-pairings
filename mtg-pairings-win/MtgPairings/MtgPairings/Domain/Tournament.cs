using System.Collections.Immutable;

namespace MtgPairings.Domain
{
    class Tournament
    {
        public readonly int tournamentId;
        public readonly string name;
        public readonly int roundCount;
        public readonly string sanctionNumber;
        public readonly ImmutableList<Round> rounds;
        public readonly ImmutableList<Team> teams;

        public Tournament(int tournamentId, string sanctionNumber, string name, int roundCount, ImmutableList<Round> rounds, ImmutableList<Team> teams)
        {
            this.tournamentId = tournamentId;
            this.sanctionNumber = sanctionNumber;
            this.name = name;
            this.roundCount = roundCount;
            this.rounds = rounds;
            this.teams = teams;
        }
    }
}
