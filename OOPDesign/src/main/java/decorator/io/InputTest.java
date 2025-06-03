package decorator.io;

import java.io.*;

public class InputTest {

    private static final String TEST_FILE = "OOPDesign/src/main/java/decorator/io/test.txt";

    public static void main(String[] args) throws FileNotFoundException {
        int c;
        try {
            // 典型的装饰者模式，层层包裹
            InputStream in =
                    new LowerCaseInputStream(
                            new BufferedInputStream(
                                    new FileInputStream(TEST_FILE)));
            while ((c = in.read()) >= 0) {
                System.out.print((char) c);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
