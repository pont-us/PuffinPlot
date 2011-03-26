package net.talvi.puffinplot.window;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import net.talvi.puffinplot.data.CustomFields;
import net.talvi.puffinplot.data.Suite;

public class CustomFieldEditor extends JFrame
                      implements ListSelectionListener {

    private JPanel contentPane;
    private JList list;
    private DefaultListModel listModel;
    private static final String addString = "Add...";
    private static final String removeString = "Remove";
    private JButton removeButton;
    private JButton addButton;
    private JButton renameButton;
    private JButton upButton;
    private JButton downButton;
    private final CustomFields<String> fields;

    public CustomFieldEditor(CustomFields<String> fields, String title) {
        super(title);
        this.fields = fields;
        listModel = new DefaultListModel();
        setFromFields();

        // Create the widgets
        list = new JList(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        list.addListSelectionListener(this);
        list.setVisibleRowCount(5);
        JScrollPane listScrollPane = new JScrollPane(list);

        addButton = new JButton(addString);
        AddFieldListener addListener = new AddFieldListener(addButton);
        //addButton.setActionCommand(addString);
        addButton.addActionListener(addListener);

        removeButton = new JButton(removeString);
        //removeButton.setActionCommand(removeString);
        removeButton.addActionListener(new RemoveFieldListener());

        upButton = new JButton("Move up");
        upButton.addActionListener(new MoveUpListener());

        // Stack up the buttons
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.PAGE_AXIS));
        buttonPane.add(upButton);
        addVstrut(buttonPane);
        buttonPane.add(new JButton("Move down"));
        addVstrut(buttonPane);
        buttonPane.add(addButton);
        addVstrut(buttonPane);
        buttonPane.add(new JButton("Rename"));
        addVstrut(buttonPane);
        buttonPane.add(removeButton);
        //buttonPane.add(Box.createVerticalStrut(5));
        // buttonPane.add(customFieldName);

        buttonPane.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));

        // Add list and buttons to the content pane
        contentPane = new JPanel(new BorderLayout());
        contentPane.add(listScrollPane, BorderLayout.CENTER);
        contentPane.add(buttonPane, BorderLayout.LINE_END);
        contentPane.setOpaque(true); // (compulsory)
        setContentPane(contentPane);
        pack();
        setVisible(true);
    }

    private void setFromFields() {
        listModel.removeAllElements();
        for (int i = 0; i < fields.size(); i++) {
            listModel.addElement(fields.get(i));
        }
    }

    private void addVstrut(JPanel p) {
        p.add(Box.createVerticalStrut(4));
    }

    class MoveUpListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            int index = list.getSelectedIndex() - 1;
            if (index<0) return;
            fields.swapAdjacent(index);
            setFromFields();
            list.setSelectedIndex(index);
            list.ensureIndexIsVisible(index);
        }
    }

    class RemoveFieldListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            int index = list.getSelectedIndex();
            listModel.remove(index);
            fields.remove(index);
            int size = listModel.getSize();
            if (size == 0) {
                // grey out the remove button
                removeButton.setEnabled(false);
            } else {
                if (index == size) {
                    // last item removed, adjust current index
                    index--;
                }
                list.setSelectedIndex(index);
                list.ensureIndexIsVisible(index);
            }
        }
    }

    class AddFieldListener implements ActionListener {
        public AddFieldListener(JButton button) {
        }

        public void actionPerformed(ActionEvent e) {
            //String name = customFieldName.getText();
            String name = (String) JOptionPane.showInputDialog(
                    CustomFieldEditor.this,
                    "Name for new field",
                    "Add custom field",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    "");
            if (name==null || name.equals("") || listModel.contains(name)) {
                return;
            }

            int newIndex = list.getSelectedIndex() + 1;
            // "no selection" == -1, so in that case it goes at the start
            listModel.insertElementAt(name, newIndex);
            fields.add(newIndex, name);
            list.setSelectedIndex(newIndex);
            list.ensureIndexIsVisible(newIndex);
        }

    }

    public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting() == false) {
            removeButton.setEnabled(list.getSelectedIndex() != -1);
        }
    }
}
