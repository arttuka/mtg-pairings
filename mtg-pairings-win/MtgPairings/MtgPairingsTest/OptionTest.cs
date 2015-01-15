using System;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using MtgPairings.Functional;

namespace MtgPairingsTest
{
    [TestClass]
    public class OptionTest
    {
        [TestMethod]
        public void TestCreateOption()
        {
            Option<int> nonempty = Option.Of(1);
            Option<int> empty = Option<int>.Empty;

            Assert.AreEqual(nonempty.Value, 1);
            Assert.IsFalse(empty.HasValue);
            try
            {
                int i = empty.Value;
                Assert.Fail("Should have thrown");
            }
            catch (InvalidOperationException) { }

        }

        [TestMethod]
        public void TestLinq()
        {
            Option<int> nonempty = Option.Of(1);
            Option<int> empty = Option<int>.Empty;

            Assert.AreEqual(nonempty.Select(i => i + 1).Value, 2);
            Assert.AreEqual(empty, empty.Select(i => i + 1));

            Assert.AreEqual(nonempty.Where(i => i > 0).Value, 1);
            Assert.IsFalse(nonempty.Where(i => i < 0).HasValue);
            Assert.AreEqual(empty.Where(i => i > 0), empty);

        }
    }
}
