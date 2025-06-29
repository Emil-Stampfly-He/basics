package template;

import java.util.AbstractList;

public class MyStringList extends AbstractList<String> {

    private final String[] myList;

    public MyStringList(String[] myList) {
        this.myList = myList;
    }

    @Override
    public String get(int index) {
        return this.myList[index];
    }

    @Override
    public int size() {
        return this.myList.length;
    }

    @Override
    public String set(int index, String value) {
        String oldString = this.myList[index];
        this.myList[index] = value;
        return oldString;
    }
}
