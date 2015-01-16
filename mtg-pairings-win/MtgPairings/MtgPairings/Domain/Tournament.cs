using System.Collections.Immutable;
using NodaTime;

namespace MtgPairings.Domain
{
    class Tournament
    {
        public readonly int tournamentId;
        public readonly string name;
        public readonly int roundCount;
        public readonly string sanctionNumber;
        public readonly LocalDate date;
        public readonly ImmutableList<Round> rounds;
        public readonly ImmutableList<Team> teams;

        public Tournament(int tournamentId, string sanctionNumber, string name, int roundCount, LocalDate date, ImmutableList<Round> rounds, ImmutableList<Team> teams)
        {
            this.tournamentId = tournamentId;
            this.sanctionNumber = sanctionNumber;
            this.name = name;
            this.roundCount = roundCount;
            this.date = date;
            this.rounds = rounds;
            this.teams = teams;
        }
    }
}
