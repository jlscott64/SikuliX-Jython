# Copyright 2010-2011, Sikuli.org
# Released under the MIT License.
from org.sikuli.script.compare import *

def byDistanceTo(m):
    """ Method to compare two Region objects by distance to m. This method is deprecated and should not be used. Use distanceComparator() instead """
    return distanceComparator(m)

def byX(m):
    """ Method to compare two Region objects by x value. This method is deprecated and should not be used. Use horizontalComparator() instead """
    return m.x

def byY(m):
    """ Method to compare two Region objects by y value. This method is deprecated and should not be used. Use verticalComparator() instead """
    return m.y

def verticalComparator():
    """ Method to compare two Region objects by y value. """
    return VerticalComparator().compare

def horizontalComparator():
    """ Method to compare two Region objects by x value. """
    return HorizontalComparator().compare

def distanceComparator(x, y=None):
    """ Method to compare two Region objects by distance to a specific point. """
    if y is None:
        return DistanceComparator(x).compare # x is Region or Location
    return DistanceComparator(x, y).compare # x/y as coordinates
