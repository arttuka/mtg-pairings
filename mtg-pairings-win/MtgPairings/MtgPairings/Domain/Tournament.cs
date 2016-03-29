using NodaTime;
using System.Collections.Immutable;
using System.Linq;

namespace MtgPairings.Domain
{
    public class Tournament
    {
        public int TournamentId { get; private set; }
        public string Name { get; private set; }
        public string Information { get; private set; }
        public string Organizer { get; private set; }
        public int RoundCount { get; private set; }
        public string SanctionNumber { get; private set; }
        public bool Active { get; private set; }
        public LocalDate Date { get; private set; }
        public ImmutableList<Round> Rounds { get; private set; }
        public ImmutableList<Team> Teams { get; private set; }
        public ImmutableList<Seating> Seatings { get; private set; }
        public ImmutableList<PodRound> Pods { get; private set; }

        public Tournament(int tournamentId, string sanctionNumber, string name, string information, string organizer, int roundCount, bool active, LocalDate date, ImmutableList<Round> rounds, ImmutableList<Team> teams, ImmutableList<Seating> seatings, ImmutableList<PodRound> pods)
        {
            TournamentId = tournamentId;
            SanctionNumber = sanctionNumber;
            Name = name;
            Information = information;
            Organizer = organizer;
            RoundCount = roundCount;
            Active = active;
            Date = date;
            Rounds = rounds;
            Teams = teams;
            Seatings = seatings;
            Pods = pods;
        }

        public Tournament WithName(string name)
        {
            return new Tournament(TournamentId, SanctionNumber, name, Information, Organizer, RoundCount, Active, Date, Rounds, Teams, Seatings, Pods);
        }

        public Tournament WithActive(bool active)
        {
            return new Tournament(TournamentId, SanctionNumber, Name, Information, Organizer, RoundCount, active, Date, Rounds, Teams, Seatings, Pods);
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
                       this.Information == other.Information &&
                       this.Organizer == other.Organizer &&
                       this.RoundCount == other.RoundCount &&
                       this.Active == other.Active &&
                       this.Date.Equals(other.Date) &&
                       this.Rounds.SequenceEqual(other.Rounds) &&
                       this.Teams.SequenceEqual(other.Teams) &&
                       this.Seatings.SequenceEqual(other.Seatings) &&
                       this.Pods.SequenceEqual(other.Pods);
            }
        }
    }
}
