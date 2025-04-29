package linked_list;

public sealed interface LinkedList<T extends Number> permits Nil, Cons {

    static <T extends Number> LinkedList<T> newLinkedList() {
        return new Nil<>();
    }

    default LinkedList<T> addFirst(T e) {
        return new Cons<>(e, this);
    }

    default LinkedList<T> remove(int index) {
        return switch (this) {
            case Nil<T> _ -> throw new IllegalStateException("No element to be removed");
            case Cons<T> c -> {
                if (index < 0 || index >= this.size()) {
                    throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + this.size());
                } else if (index == 0) {
                    yield c.removeFirst();
                } else {
                    LinkedList<T> newTail = c.next().remove(index - 1);
                    yield new Cons<T>(c.e(), newTail);
                }
            }
        };
    }

    default LinkedList<T> removeFirst() {
        return switch (this) {
            case Nil<T> _ -> throw new IllegalStateException("No element to be removed");
            case Cons<T> c -> c.next();
        };
    }

    default LinkedList<T> removeLast() {
        return switch (this) {
            case Nil<T> _ -> throw new IllegalStateException("No element to be removed");
            case Cons<T> c -> c.remove(this.size() - 1);
        };
    }

    default int size() {
        return switch (this) {
            case Nil<T> _ -> 0;
            case Cons<T> c -> c.next().size() + 1;
        };
    }

    default double sum() {
        return switch (this) {
            case Nil<T> _ -> 0;
            case Cons<T> c -> c.e().doubleValue() + c.next().sum();
        };
    }

    default double average() {
        return switch (this) {
            case Nil<T> _ -> 0;
            // case Cons c -> (double) sum(l) / size(l);
            case Cons<T> _ -> {
                double sum = 0;
                int size = 0;
                for (LinkedList<T> cur = this; cur instanceof Cons(T e, LinkedList<T> next); cur = next) {
                    sum += e.doubleValue();
                    size++;
                }

                yield size == 0 ? 0 : sum / size;
            }
        };
    }
}

record Nil<T extends Number>() implements LinkedList<T> {}

record Cons<T extends Number>(T e, LinkedList<T> next) implements LinkedList<T> {}

