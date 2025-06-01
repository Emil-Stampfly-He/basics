package observer.swing;

import javax.swing.*;

public class SwingObserverExample {
    JFrame frame;

    public static void main(String[] args) {
        SwingObserverExample example = new SwingObserverExample();
        example.go();
    }

    private void go() {
        this.frame = new JFrame();

        JButton button = new JButton("Should I do it?");

        // Observer
        button.addActionListener(angelEvent -> System.out.println("Don't do it, you might regret it!"));
        button.addActionListener(evilEvent -> System.out.println("Come on, do it!"));

        this.frame.add(button);
        this.frame.pack();
        this.frame.setVisible(true);
    }
}
