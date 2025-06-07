package adapter.enumeration;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

public class EnumerationIterator<E> implements Iterator<E> {
    Enumeration<E> enumeration;

    public EnumerationIterator(Enumeration<E> enumeration) {
        this.enumeration = enumeration;
    }

    @Override
    public boolean hasNext() {
        return this.enumeration.hasMoreElements();
    }

    @Override
    public E next() {
        return this.enumeration.nextElement();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    public static void main(String[] args) {
        List<String> list = List.of("apple", "banana", "cherry");
        Enumeration<String> en = Collections.enumeration(list);

        EnumerationIterator<String> adapter = new EnumerationIterator<>(en);
        while (adapter.hasNext()) {
            System.out.println(adapter.next());
        }
    }
}
