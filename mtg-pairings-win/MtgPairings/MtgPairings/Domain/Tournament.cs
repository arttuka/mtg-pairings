using NodaTime;
using System.Collections.Immutable;

namespace MtgPairings.Domain
{
    public class Tournament
    {
        public readonly int TournamentId;
        public readonly string Name;
        public readonly int RoundCount;
        public readonly string SanctionNumber;
        public readonly LocalDate Date;
        public readonly ImmutableList<Round> Rounds;
        public readonly ImmutableList<Team> Teams;
        public readonly ImmutableList<Seating> Seatings;

        public Tournament(int tournamentId, string sanctionNumber, string name, int roundCount, LocalDate date, ImmutableList<Round> rounds, ImmutableList<Team> teams, ImmutableList<Seating> seatings)
        {
            this.TournamentId = tournamentId;
            this.SanctionNumber = sanctionNumber;
            this.Name = name;
            this.RoundCount = roundCount;
            this.Date = date;
            this.Rounds = rounds;
            this.Teams = teams;
            this.Seatings = seatings;
        }
    }
}
