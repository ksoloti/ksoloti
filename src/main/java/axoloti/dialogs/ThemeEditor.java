package axoloti.dialogs;

import components.ScrollPaneComponent;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

import axoloti.ui.Theme;

public class ThemeEditor extends JFrame {

    private static final Logger LOGGER = Logger.getLogger(ThemeEditor.class.getName());

    private Theme theme;
    private JPanel p;

    public ThemeEditor() {
        this.setPreferredSize(new Dimension(600, 600));
        theme = Theme.getCurrentTheme();
        p = new JPanel();
        ScrollPaneComponent s = new ScrollPaneComponent(p);
        p.setLayout(
                new GridLayout(theme.getClass().getFields().length + 8, 2)
        );
        final JButton load = new JButton("Load");
        load.addActionListener(e -> {
            theme.load(ThemeEditor.this);
            theme = Theme.getCurrentTheme();
            update();
        });

        final JButton save = new JButton("Save");
        save.addActionListener(e -> {
            theme.save(ThemeEditor.this);
        });

        final JButton revertToDefault = new JButton("Revert to Default");
        revertToDefault.addActionListener(e -> {
            Theme.loadDefaultTheme();
            theme = Theme.getCurrentTheme();
            update();
        });

        p.add(load);
        p.add(save);
        p.add(revertToDefault);
        p.add(new JLabel("   Note: reload patch to see changes."));
        p.add(new JPanel());
        p.add(new JPanel());

        for (final Field f : theme.getClass().getFields()) {
            p.add(new JLabel(f.getName().replace("_", " ")));
            try {
                if (f.getName() == "Theme_Name") {
                    final JTextArea textArea = new JTextArea((String) f.get(theme));
                    p.add(textArea);
                    textArea.getDocument().addDocumentListener(
                            new DocumentListener() {
                        @Override
                        public void removeUpdate(DocumentEvent e) {
                            updateThemeName(e);
                        }

                        @Override
                        public void insertUpdate(DocumentEvent e) {
                            updateThemeName(e);
                        }

                        @Override
                        public void changedUpdate(DocumentEvent e) {
                            updateThemeName(e);
                        }
                    });
                    p.add(new JPanel());
                    p.add(new JPanel());
                }
                else {
                    final JButton t = new JButton();
                    t.setBorder(BorderFactory.createLineBorder(getBackground(), 2));
                    final Color currentColor = (Color) f.get(theme);
                    t.setBackground(currentColor);
                    t.setContentAreaFilled(false);
                    t.setOpaque(true);
                    t.addMouseListener(new MouseListener() {
                        public void mouseClicked(MouseEvent e) {
                            try {
                                Color newColor = pickColor(e.getComponent().getBackground());
                                if (newColor != null) {
                                    f.set(theme, newColor);
                                    e.getComponent().setBackground(newColor);
                                    e.getComponent().repaint();
                                }

                            }
                            catch (IllegalAccessException ex) {
                                LOGGER.log(Level.SEVERE, "{0}", new Object[]{ex});
                            }
                        }

                        public void mousePressed(MouseEvent e) {

                        }

                        public void mouseEntered(MouseEvent e) {

                        }

                        public void mouseExited(MouseEvent e) {

                        }

                        public void mouseReleased(MouseEvent e) {

                        }
                    });
                    p.add(t);
                }
            }
            catch (IllegalAccessException ex) {
                LOGGER.log(Level.SEVERE, "{0}", new Object[]{ex});
            }
        }
        this.setContentPane(s);
        this.pack();
    }

    private void update() {
        int i = 9;
        for (final Field f : theme.getClass().getFields()) {
            Component target = p.getComponent(i);
            try {
                Color color = (Color) f.get(theme);
                target.setBackground(color);
            }
            catch (IllegalAccessException ex) {
                LOGGER.log(Level.SEVERE, "Failed to access theme field: {0}", ex);
            }
            catch (ClassCastException e) {
                LOGGER.log(Level.SEVERE, "Type mismatch when updating theme field: {0}", e);
            }
            target.repaint();
            i += 2;
        }
    }

    private void updateThemeName(DocumentEvent e) {
        String str = "";
        try {
            str = e.getDocument().getText(0, e.getDocument().getLength());
            theme.Theme_Name = str;
        }
        catch (BadLocationException ex) {
            LOGGER.log(Level.SEVERE, "Failed to update theme name to '" + str + "': {0}", ex);
        }
    }

    private Color pickColor(Color initial) {
        return JColorChooser.showDialog(
                this,
                "Choose Color",
                initial);
    }
}
