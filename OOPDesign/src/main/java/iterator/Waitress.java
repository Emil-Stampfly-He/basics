package iterator;

import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.Iterator;

@AllArgsConstructor
public class Waitress {
    ArrayList<Menu> menus;

    public void printMenu() {
        for (Menu menu : menus) {
            printMenu(menu.createIterator());
        }
    }

    private void printMenu(Iterator<MenuItem> iterator) {
        while (iterator.hasNext()) {
            MenuItem menuItem = iterator.next();
            System.out.println(menuItem.getName() + ", ");
            System.out.println(menuItem.getPrice() + ", ");
            System.out.println(menuItem.getDescription());
        }
    }
}
