using System;
using System.Collections;
using System.Collections.Generic;

namespace MtgPairings.Functional
{
    public sealed class Option<T> : IEnumerable<T>
    {
        private readonly T value;
        private readonly bool hasValue;
        public static readonly Option<T> Empty = new Option<T>();

        public T Value
        {
            get
            {
                if (!hasValue)
                {
                    throw new InvalidOperationException("Option doesn't have a value");
                }
                return value;
            }
        }

        public bool HasValue
        {
            get { return hasValue; }
        }

        public Option(T value)
        {
            if (value == null)
            {
                this.value = default(T);
                this.hasValue = false;
            }
            else
            {
                this.value = value;
                this.hasValue = true;
            }
        }

        private Option()
        {
            this.value = default(T);
            this.hasValue = false;
        }

        public T ValueOrElse(T defaultValue)
        {
            if (hasValue)
            {
                return value;
            }
            else
            {
                return defaultValue;
            }
        }

        public IEnumerator<T> GetEnumerator()
        {
            if (hasValue)
            {
                yield return value;
            }
        }

        IEnumerator IEnumerable.GetEnumerator()
        {
            return GetEnumerator();
        }

        public Option<S> Select<S>(Func<T, S> f)
        {
            if (hasValue)
            {
                return Option.Of(f(value));
            }
            else
            {
                return Option<S>.Empty;
            }
        }

        public Option<T> Where(Predicate<T> pred)
        {
            if (!hasValue || pred(value))
            {
                return this;
            }
            else
            {
                return Empty;
            }
        }

        public override bool Equals(object obj)
        {
            return this.Equals(obj as Option<T>);
        }

        public bool Equals(Option<T> other)
        {
            if (other == null)
            {
                return false;
            }
            else if (ReferenceEquals(other, this))
            {
                return true;
            }
            else if(this.HasValue)
            {
                return other.HasValue && EqualityComparer<T>.Default.Equals(this.Value, other.Value);
            }
            else
            {
                return !other.HasValue;
            }
        }
    }

    public static class Option
    {
        public static Option<T> Of<T>(T value)
        {
            return new Option<T>(value);
        }
    }
}
