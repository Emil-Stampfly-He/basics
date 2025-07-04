package iterator;

import java.util.ArrayList;
import java.util.Iterator;

public class PancakeHouseMenuIterator implements Iterator<MenuItem> {
    ArrayList<MenuItem> menuItems;
    int position = 0;

    public PancakeHouseMenuIterator(ArrayList<MenuItem> menuItems) {
        this.menuItems = menuItems;
    }

    @Override
    public boolean hasNext() {
        return position < menuItems.size();
    }

    @Override
    public MenuItem next() {
        return menuItems.get(position++);
    }
}
