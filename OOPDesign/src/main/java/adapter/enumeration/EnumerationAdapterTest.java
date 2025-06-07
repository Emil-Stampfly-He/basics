package adapter.enumeration;

import java.util.Enumeration;
import java.util.List;

public class EnumerationAdapterTest {
    public static void main(String[] args) {
        List<String> list = List.of("a", "b", "c");
        Enumeration<String> en = new IteratorEnumeration<>(list.iterator());

        while (en.hasMoreElements()) {
            System.out.println(en.nextElement());
        }
    }
}
