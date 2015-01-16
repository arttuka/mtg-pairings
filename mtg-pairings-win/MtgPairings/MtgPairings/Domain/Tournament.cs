using System.Collections.Immutable;
using NodaTime;

namespace MtgPairings.Domain
{
    class Tournament
    {
        public readonly int TournamentId;
        public readonly string Name;
        public readonly int RoundCount;
        public readonly string SanctionNumber;
        public readonly LocalDate Date;
        public readonly ImmutableList<Round> Rounds;
        public readonly ImmutableList<Team> Teams;

        public Tournament(int tournamentId, string sanctionNumber, string name, int roundCount, LocalDate date, ImmutableList<Round> rounds, ImmutableList<Team> teams)
        {
            this.TournamentId = tournamentId;
            this.SanctionNumber = sanctionNumber;
            this.Name = name;
            this.RoundCount = roundCount;
            this.Date = date;
            this.Rounds = rounds;
            this.Teams = teams;
        }
    }
}
