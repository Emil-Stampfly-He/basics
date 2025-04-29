package linked_list;

public sealed interface NewLinkedList permits Nil, Cons {

    static NewLinkedList newLinkedList() {
        return new Nil();
    }

    default NewLinkedList addFirst(int e) {
        return new Cons(e, this);
    }

    default NewLinkedList removeFirst() {
        return switch (this) {
            case Nil _ -> throw new IllegalStateException("No element to be removed");
            case Cons c -> c.next();
        };
    }

    default int size() {
        return switch (this) {
            case Nil _ -> 0;
            case Cons c -> c.next().size() + 1;
        };
    }

    default int sum() {
        return switch (this) {
            case Nil _ -> 0;
            case Cons c -> c.e() + c.next().sum();
        };
    }

    default double average() {
        return switch (this) {
            case Nil _ -> 0;
            // case Cons c -> (double) sum(l) / size(l);
            case Cons _ -> {
                int sum = 0;
                int size = 0;
                for (NewLinkedList cur = this; cur instanceof Cons(int e, NewLinkedList next); cur = next) {
                    sum += e;
                    size++;
                }

                yield size == 0 ? 0 : (double) sum / size;
            }
        };
    }
}

record Nil() implements NewLinkedList {}

record Cons(int e, NewLinkedList next) implements NewLinkedList {}

