package muster;

import java.util.List;
import java.util.Map;

/**
 * User: ivan
 * Date: 1/4/14
 * Time: 6:15 PM
 */
public class JavaListOfMapOfList {
    private String name;
    private List<Map<String, List<Integer>>> lst;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Map<String, List<Integer>>> getLst() {
        return lst;
    }

    public void setLst(List<Map<String, List<Integer>>> lst) {
        this.lst = lst;
    }
}
