package class_loader;

public class PrintClassLoaderTree {

    public static void main(String[] args) {
        ClassLoader classLoader = PrintClassLoaderTree.class.getClassLoader();
        StringBuilder sb = new StringBuilder("|--");

        while (true) {
            System.out.println(sb.toString() + classLoader);
            if (classLoader == null) {
                break;
            } else {
                classLoader = classLoader.getParent();
                sb.insert(0, "\t");
            }
        }
    }
}
