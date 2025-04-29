package linked_list;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestLinkedList {

    @Test
    public void test() {
        var list = LinkedList
                .newLinkedList()
                .addFirst(0)
                .addFirst(1)
                .addFirst(2)
                .addFirst(3)
                .addFirst(4);

        Assertions.assertEquals(5, list.size());
        Assertions.assertEquals(10, list.sum());
        Assertions.assertEquals(2.0, list.average());

        list = list.removeFirst().removeFirst();
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals(3, list.sum());
        Assertions.assertEquals(1.0, list.average());
    }

    @Test
    public void testRemove() {

    }
}
