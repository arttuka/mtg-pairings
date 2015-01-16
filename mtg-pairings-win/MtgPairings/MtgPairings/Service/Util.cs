using MtgPairings.Functional;
using NodaTime;
using System;
using System.Collections.Generic;

namespace MtgPairings.Service
{
    public static class Util
    {
        public static Option<V> Get<K, V>(this IDictionary<K, V> source, K key)
        {
            V value;
            if (source.TryGetValue(key, out value))
            {
                return Option.Of(value);
            }
            else
            {
                return Option<V>.Empty;
            }
        }

        public static Option<T> Get<T>(this IList<T> source, int index)
        {
            if (source.Count > index)
            {
                return Option.Of(source[index]);
            }
            else
            {
                return Option<T>.Empty;
            }
        }

        public static LocalDate ToLocalDate(this DateTime source)
        {
            return new LocalDate(source.Year, source.Month, source.Day);
        }
    }
}
