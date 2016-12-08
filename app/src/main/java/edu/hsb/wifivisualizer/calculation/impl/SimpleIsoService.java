package edu.hsb.wifivisualizer.calculation.impl;

import com.google.common.collect.Lists;

import java.util.List;

import edu.hsb.wifivisualizer.calculation.IIsoService;
import edu.hsb.wifivisualizer.model.Isoline;
import edu.hsb.wifivisualizer.model.Triangle;

// TODO: Add implementation for isoline extraction
public class SimpleIsoService implements IIsoService {

    @Override
    public List<Isoline> extractIsolines(List<Triangle> triangleList) {
        return Lists.newArrayList();
    }
}