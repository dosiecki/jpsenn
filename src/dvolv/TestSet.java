package dvolv;

import java.util.ArrayList;
import java.util.List;

public abstract class TestSet {

    public String name = "";

    public List<TestCase> cases = new ArrayList<TestCase>();

    public abstract TestCase randomCase();

    public abstract void regen();
}
