using MtgPairings.Functional;
using System.Collections.Generic;
using System;

namespace MtgPairings.Domain
{
    public class Pairing
    {
        public int Table { get; private set; }
        public Team Team1 { get; private set; }
        public Option<Team> Team2 { get; private set; }
        public Option<Result> Result { get; private set; }

        public Pairing(int table, Team team1, Option<Team> team2, Option<Result> result)
        {
            Table = table;
            Team1 = team1;
            Team2 = team2;
            Result = result;
        }

        public override bool Equals(object obj)
        {
            return this.Equals(obj as Pairing);
        }

        public bool Equals(Pairing other)
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
                return this.Table == other.Table &&
                       this.Team1.Equals(other.Team1) &&
                       this.Team2.Equals(other.Team2) &&
                       this.Result.Equals(other.Result);
            }
        }

        public class PairingEqualityComparer : IEqualityComparer<Pairing>
        {
            public bool Equals(Pairing left, Pairing right)
            {
                if (left == null || right == null)
                {
                    return left == null && right == null;
                }
                else if (ReferenceEquals(left, right))
                {
                    return true;
                }
                else
                {
                    return left.Table == right.Table &&
                           left.Team1.Equals(right.Team1) &&
                           left.Team2.Equals(right.Team2);
                }
            }

            public int GetHashCode(Pairing obj)
            {
                return obj.GetHashCode();
            }
        }
    }
}
