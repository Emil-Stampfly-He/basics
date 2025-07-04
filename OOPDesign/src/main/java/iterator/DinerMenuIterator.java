package iterator;

import java.util.Iterator;

public class DinerMenuIterator implements Iterator<MenuItem> {
    MenuItem[] items;
    int position = 0;

    public DinerMenuIterator(MenuItem[] items) {
        this.items = items;
    }

    @Override
    public MenuItem next() {
        return items[position++];
    }

    @Override
    public boolean hasNext() {
        return position < items.length && items[position] != null;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("You should not remove any item from this menu");
    }
}
