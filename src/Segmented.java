import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/* A row of text options where exactly one is chosen (underlined in red). Used on the menu. */
class Segmented extends JPanel {

    private final List<FlatButton> buttons = new ArrayList<>();

    Segmented(String[] labels, int initial, IntConsumer onChange) {
        super(new FlowLayout(FlowLayout.LEFT, 0, 0));
        setOpaque(false);
        for (int i = 0; i < labels.length; i++) {
            int index = i;
            if (i > 0) {
                add(Box.createHorizontalStrut(20));
            }
            FlatButton b = FlatButton.text(labels[i]);
            b.addActionListener(e -> {
                select(index);
                onChange.accept(index);
            });
            buttons.add(b);
            add(b);
        }
        select(initial);
    }

    void select(int index) {
        for (int i = 0; i < buttons.size(); i++) {
            buttons.get(i).setChosen(i == index);
        }
    }
}
