using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace MtgPairings.Functional
{
    public static class Enumerable
    {
        public static IEnumerable<TResult> ZipAll<TFirst, TSecond, TResult>(
            this IEnumerable<TFirst> first,
            IEnumerable<TSecond> second,
            Func<TFirst, TSecond, TResult> resultSelector)
        {
            if (first == null) throw new ArgumentNullException("first");
            if (second == null) throw new ArgumentNullException("second");

            using (var iterFirst = first.GetEnumerator())
            using (var iterSecond = second.GetEnumerator())
            {
                Boolean moreFirst = iterFirst.MoveNext();
                Boolean moreSecond = iterSecond.MoveNext();
                while (moreFirst || moreSecond)
                {
                    TFirst f = moreFirst ? iterFirst.Current : default(TFirst);
                    TSecond s = moreSecond ? iterSecond.Current : default(TSecond);
                    yield return resultSelector(f, s);
                    moreFirst = iterFirst.MoveNext();
                    moreSecond = iterSecond.MoveNext();
                }
                
            }
        }
    }
}
