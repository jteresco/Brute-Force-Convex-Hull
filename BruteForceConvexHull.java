/*
  Brute force convex hull implementation to use METAL graph
  data vertices as the inputs.

  @author Jim Teresco
  @version Spring 2024

  Originally implemented for
  Spring 2011, CSIS 385, Siena College
  Updated for Spring 2017, Spring 2018

*/

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

class Point {

    // instance variables for label and coordinates
    private String label;
    private double x;
    private double y;

    public Point(String label, double x, double y) {
        this.label = label;
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public String getLabel() {
        return label;
    }

    public String toString() {
        return label + " (" + x + "," + y + ")";
    }

    public String toStringTMG() {
        return label + " " + x + " " + y;
    }

    public boolean equals(Object o) {
        Point other = (Point)o;
        return (x == other.x) &&
        (y == other.y) &&
        label.equals(other.label);
    }

    public double squaredDistance(Point other) {

        double dx, dy;
        dx = x-other.x;
        dy = y-other.y;
        return dx*dx + dy*dy;
    }

    /**
    Check if this point is directly in between the two given
    points.  Note: the assumption is that they are colinear.

    @param o1 one of the points
    @param o2 the other point
    @return whether this point is between the two given points
     */
    public boolean isBetween(Point o1, Point o2) {

        double sqDisto1o2 = o1.squaredDistance(o2);
        return (squaredDistance(o1) < sqDisto1o2) &&
        (squaredDistance(o2) < sqDisto1o2);
    }
}

class LineSegment {

    // store the endpoints
    private Point start;
    private Point end;

    public LineSegment(Point a, Point b) {

        start = a;
        end = b;
    }

    public Point getStart() {

        return start;
    }

    public Point getEnd() {

        return end;
    }

    public String toString() {
        return "Segment from " + start + " to " + end;
    }
}

public class BruteForceConvexHull {

    /**
    Read in a list of points:
    label x y
    from the file in args[0] (second line contains number of points)

    Then compute convex hull using brute force, print out in
    readable text (args[1].equals("text") or nothing), as data
    plottable in METAL's HDX (args[1].equals("tmg")), or print
    a timing/op count summary (args[1].equals("timings")).
     */

    public static void main(String args[]) {

        int numPoints = 0;
        boolean debug = false;

        if ((args.length < 1) || (args.length > 2) ||
	    ((args.length == 2) && !args[1].equals("list")
	     && !args[1].equals("tmg") && !args[1].equals("timings"))) {
            System.err.println("Usage: java BruteForceConvexHull filename [type]");
	    System.err.println("type can be list, tmg, or timings, default is list");
            System.exit(1);
        }

        // create a list of points
        ArrayList<Point> points = new ArrayList<Point>();

        try {
            Scanner s = new Scanner(new File(args[0]));
            // skip header line (would be good to do error checking)
            s.nextLine();
            // second line is number of waypoints and connections
            numPoints = s.nextInt();
            // skip the rest of the line
            s.nextLine();
            // read the lines
            for (int i = 0; i<numPoints; i++) {
                points.add(new Point(s.next(), s.nextDouble(), s.nextDouble()));
            }

        }
        catch (FileNotFoundException e) {
            System.err.println(e);
            System.exit(1);
        }

        // we will build the line segments that form the hull in this list
        ArrayList<LineSegment> hull = new ArrayList<LineSegment>();

        // consider each pair of points
        for (int i = 0; i < numPoints-1; i++) {

            Point v1 = points.get(i);
            for (int j = i+1; j < numPoints; j++) {

                Point v2 = points.get(j);
                // from here, we need to see if all other points are
                // on the same side of the line connecting v1 and v2
                double a = v2.getY() - v1.getY();
                double b = v1.getX() - v2.getX();
                double c = v1.getX() * v2.getY() - v1.getY() * v2.getX();
                // now check all other points to see if they're on the
                // same side -- stop as soon as we find they're not
                int lookingFor = 0; // UNKNOWN from the HDX AV
                boolean eliminated = false;

                for (int k = 0; k < numPoints; k++) {

                    Point vtest = points.get(k);

                    if (v1.equals(vtest) || v2.equals(vtest)) 
                        continue;
                    double checkVal = a * vtest.getX() + b * vtest.getY() - c;
                    if (debug)
                        System.out.println("Checking " + vtest + 
                            " for segment from " + v1 + 
                            " to " + v2);	
                    if (checkVal == 0) {
                        // if in between, continue, otherwise skip this pair
                        // since we'll catch it elsewhere
                        if (vtest.isBetween(v1, v2)) {
                            continue;
                        }
                        else {
                            if (debug) 
                                System.out.println("Found colinear point " + 
                                    vtest + " directly between " +
                                    v1 + " and " + v2);
                            eliminated = true;
                            break;
                        }
                    }
                    if (lookingFor == 0) {
                        lookingFor = (checkVal > 0 ? 1 : -1);  // POSITIVE or NEGATIVE from HDX AV
                    }
                    else {
                        if (((lookingFor > 0) && (checkVal < 0) ||
			     ((lookingFor < 0) && (checkVal > 0)))) {
                            // segment not on hull, jump out of innermost loop
                            if (debug)
                                System.out.println("Found points on opposite sides of line between " +
                                    v1 + " and " + v2);
                            eliminated = true;
                            break;
                        }	
                    }
                }
                // we didn't find a reason that this segment was not on the
                // hull, so we add it
                if (!eliminated) hull.add(new LineSegment(v1, v2));
            }
        }

        // we now have a list of line segments that form the hull
        if (debug) {
            System.out.println("Convex hull is formed from line segments:");
            for (LineSegment l : hull)
                System.out.println(l);
        }

        // we pull out the points and list them in order, repeating the last
        // so we can easily draw the hull
        ArrayList<Point> hullPoints = new ArrayList<Point>();
        // we'll start with the first segment in the list
        LineSegment firstSegment = hull.get(0);
        Point firstHullPoint = firstSegment.getStart();
        hullPoints.add(firstHullPoint);
        Point nextSegmentPoint = firstSegment.getEnd();
        hullPoints.add(nextSegmentPoint);
        hull.remove(firstSegment);
        while (!hull.isEmpty()) {
            for (LineSegment l : hull) {
                if (l.getStart().equals(nextSegmentPoint)) {
                    nextSegmentPoint = l.getEnd();
                    hullPoints.add(nextSegmentPoint);
                    hull.remove(l);
                    break;
                }
                if (l.getEnd().equals(nextSegmentPoint)) {
                    nextSegmentPoint = l.getStart();
                    hullPoints.add(nextSegmentPoint);
                    hull.remove(l);
                    break;
                }
            }
        }

        // print the results
        if ((args.length == 1) || args[1].equals("list")) {
            System.out.println("Convex hull polygon:");
            // all points along the way
            for (Point p : hullPoints) {
                System.out.println(p);
            }
        }
        else if (args[1].equals("tmg")) {
            // TMG file header
            System.out.println("TMG 1.0 simple");
            System.out.println(hullPoints.size() + " " + hullPoints.size());
            // all points along the way
            for (Point p : hullPoints) {
                System.out.println(p.toStringTMG());
            }
            if ((args.length > 1) && args[1].equals("tmg")) {
                // add in "graph edges", really just connections between 
                // each adjacent point
                for (int i = 0; i < hullPoints.size(); i++) {
                    System.out.print(i + " ");
                    if (i < hullPoints.size()-1) {
                        System.out.print(i+1);
                    }
                    else {
                        System.out.print(0);
                    }
                    System.out.println(" HullSeg" + i);
                }
            }
        }
	else if (args[1].equals("timings")) {

	    System.out.println("Replace this printout with your timing results line for this graph!");
	}
	else {
            System.err.println("This statement should never be reached.");		
        }
    }
}
