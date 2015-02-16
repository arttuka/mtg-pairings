using NodaTime;
using System.Collections.Immutable;
using System.Linq;

namespace MtgPairings.Domain
{
    public class Tournament
    {
        public int TournamentId { get; private set; }
        public string Name { get; private set; }
        public int RoundCount { get; private set; }
        public string SanctionNumber { get; private set; }
        public LocalDate Date { get; private set; }
        public ImmutableList<Round> Rounds { get; private set; }
        public ImmutableList<Team> Teams { get; private set; }
        public ImmutableList<Seating> Seatings { get; private set; }

        public Tournament(int tournamentId, string sanctionNumber, string name, int roundCount, LocalDate date, ImmutableList<Round> rounds, ImmutableList<Team> teams, ImmutableList<Seating> seatings)
        {
            TournamentId = tournamentId;
            SanctionNumber = sanctionNumber;
            Name = name;
            RoundCount = roundCount;
            Date = date;
            Rounds = rounds;
            Teams = teams;
            Seatings = seatings;
        }

        public override bool Equals(object obj)
        {
            return this.Equals(obj as Tournament);
        }

        public bool Equals(Tournament other)
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
                return this.TournamentId == other.TournamentId &&
                       this.SanctionNumber == other.SanctionNumber &&
                       this.Name == other.Name &&
                       this.RoundCount == other.RoundCount &&
                       this.Date == other.Date &&
                       this.Rounds.SequenceEqual(other.Rounds) &&
                       this.Teams.SequenceEqual(other.Teams) &&
                       this.Seatings.SequenceEqual(other.Seatings);
            }
        }
    }
}
