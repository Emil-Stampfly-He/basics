package iterator;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MenuItem {
    String name;
    String description;
    boolean vegetarian;
    double price;
}
