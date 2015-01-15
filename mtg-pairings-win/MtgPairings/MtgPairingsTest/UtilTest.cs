using System;
using System.Collections.Generic;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using MtgPairings.Service;
using MtgPairings.Functional;

namespace MtgPairingsTest
{
    [TestClass]
    public class UtilTest
    {
        [TestMethod]
        public void TestDictionaryGet()
        {
            IDictionary<int, int> dict = new Dictionary<int, int>() {{1, 1}};

            Assert.AreEqual(dict.Get(1).Value, 1);
            Assert.IsFalse(dict.Get(0).HasValue);
        }

        [TestMethod]
        public void TestListGet()
        {
            IList<int> list = new List<int>() {1};

            Assert.AreEqual(list.Get(0).Value, 1);
            Assert.IsFalse(list.Get(1).HasValue);
        }
    }
}
