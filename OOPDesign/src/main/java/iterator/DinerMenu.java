package iterator;

import lombok.Getter;

import java.util.Iterator;

@Getter
public class DinerMenu implements Menu {
    static final int MAX_ITEMS = 6;
    int numberOfItems = 0;
    MenuItem[] menuItems;

    public DinerMenu() {
        this.menuItems = new MenuItem[MAX_ITEMS];

        this.addItem("Vegetarian BLT",
                "(Fakin') Bacon with lettuce & tomato on whole wheat",
                true,
                2.99);
        this.addItem("BLT",
                "Bacon with lettuce & tomato on whole wheat",
                false,
                2.99);
        this.addItem("Soup of the day",
                "Soup of the day, with a side of potato salad",
                false,
                3.29);
    }

    public void addItem(String name, String description, boolean vegetarian, double price) {
        MenuItem item = new MenuItem(name, description, vegetarian, price);

        if (numberOfItems >= MAX_ITEMS) {
            System.err.println("Sorry, menu is full! Cannot add item to menu");
        } else {
            menuItems[numberOfItems] = item;
            numberOfItems++;
        }
    }

    @Override
    public Iterator<MenuItem> createIterator() {
        return new DinerMenuIterator(menuItems);
    }
}
