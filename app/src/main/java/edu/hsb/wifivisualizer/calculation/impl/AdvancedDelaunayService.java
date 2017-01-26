package edu.hsb.wifivisualizer.calculation.impl;

import com.google.common.collect.Lists;

import java.util.List;

import edu.hsb.wifivisualizer.calculation.IDelaunayService;
import edu.hsb.wifivisualizer.model.Point;
import edu.hsb.wifivisualizer.model.Triangle;

public class AdvancedDelaunayService implements IDelaunayService {

    private Triangle activeTriangle = null;
    private List<Triangle> graph;


    public AdvancedDelaunayService(Triangle triangle) {

        graph =  Lists.newArrayList();
        graph.add(triangle);
        activeTriangle = triangle;

    }

    //wahr wenn dreieck zur triangulation dazu gehoert
    public boolean contains(Object triangle) {
        return graph.contains(triangle);
    }

    //gegenueber vom nachbarn
    /*
    public Triangle triangleOpposite(Triangle triangle, List<Point> pos) {
        for(Triangle neighbor : graph.contains(triangle)) {
            if(!nicht die nachbarsposition) {
                return triangle;
            }
        }
        return null;
    }
    */

    //liste der nachbarn wiedergeben??
    public List<Triangle> neighbours(Triangle triangle) {
        //return graph.contains(triangle);
        return null;
    }

    //dreiecke wiedergeben in der liste
    /*
    public List<Triangle> getTriangleList(Triangle triangle) {

        if(wenn dreieck nicht in dem punkt vorhanden ist ) {
            throw new IllegalArgumentException("Punkt nicht gefunden");
        }
        List<Triangle> list = new ArrayList<Triangle>();
        Triangle start = triangle;

        while(true) {
            list.add(triangle);
            Triangle prev = triangle;
            triangle = this.triangleOpposite(triangle, triangle.getDefiningPointList());
            if(triangle == start) break;
        }

        return list;
    }
    */



    //dreiecke lokalisieren fuer aktualisierung (calculate)
    /*
    public Triangle locateTriangle() {
        Triangle triangle = activeTriangle;
        if(!this.contains(triangle)) triangle = null;

        //laeuft dann im uhrzeigersinn nach den dreiecken
        List<Triangle> visitedOnce;
        while(triangle!=null) {
            visitedOnce.add(triangle);

        }
    }
    */



    @Override
    public List<Triangle> calculate(List<Point> triangleList) {
        /*
        triangleList.add(new Point(null, new LatLng(0,0), 0));
        triangleList.add(new Point(null, new LatLng(1,1), 1));
        triangleList.add(new Point(null, new LatLng(2,2), 2));


        for(Point p : triangleList) {
            if(triangleList.contains(p)) {
                //find triangle where p is in it
                if(PointInTriangle(p, triangleList.get(0), triangleList.get(1), triangleList.get(2))) {
                    triangleList.add(p);
                    //neues Dreieck
                }
            }
        }

        /*
        boolean isTriangle = true;
        //3 points w new point
        for(int i = 0; i < testList.size(); i++) {
            for(int j = i+1; j  < testList.size(); j++) {
                for(int k = j+1; k < testList.size(); k++) {
                    //for new point
                    for(int a = 0; a < testList.size(); a++) {
                        if(!(a==i || a==j || a==k)) break;
                        if(testList.get(a).getPosition() == current pos) {
                            isTriangle = false;
                            break;
                        }

                    }
                    //draw triangle
                    if(isTriangle) {

                    }
                }
            }
        }
        */
        return null;
    }


    //how to get the x, y position??
    /*
    private float sign(Point p1, Point p2, Point p3) {
        return (p1.x - p3.x) * (p2.y - p3.y) - (p2.x - p3.x) * (p1.y - p3.y);
    }

    private boolean PointInTriangle(Point pr, Point p1, Point p2, Point p3) {
        boolean b1, b2, b3;
        b1 = sign(pr, p1, p2) < 0;
        b2 = sign(pr, p2, p3) < 0;
        b3 = sign(pr, p3, p1) < 0;

        return (b1 == b2) && (b2 == b3);
    }
    */
}