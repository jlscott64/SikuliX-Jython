'''
Created on 16.05.2013
'''
import unittest
from sikuli import Region
from org.sikuli.script import Location
from sikuli import Helper

class TestHelpers(unittest.TestCase):

    def testHelpers(self):
        baseList = [Region(500,500,100,100), Region(0,0,100,100), Region(100,100,100,100)]

        testList = sorted(baseList, key=Helper.byX)
        
        assert testList[0] == baseList[1]
        assert testList[1] == baseList[2]
        assert testList[2] == baseList[0]
        
        testList = sorted(baseList, key=Helper.byY)
        
        assert testList[0] == baseList[1]
        assert testList[1] == baseList[2]
        assert testList[2] == baseList[0]
        
        testList = sorted(baseList, Helper.byDistanceTo(Location(0,0)))
        
        assert testList[0] == baseList[1]
        assert testList[1] == baseList[2]
        assert testList[2] == baseList[0]
        
        testList = sorted(baseList, Helper.verticalComparator())
        
        assert testList[0] == baseList[1]
        assert testList[1] == baseList[2]
        assert testList[2] == baseList[0]
        
        testList = sorted(baseList, Helper.horizontalComparator())
        
        assert testList[0] == baseList[1]
        assert testList[1] == baseList[2]
        assert testList[2] == baseList[0]
        

if __name__ == "__main__":
    unittest.main()
