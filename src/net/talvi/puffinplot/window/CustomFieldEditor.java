package net.talvi.puffinplot.window;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.data.CustomFields;

public class CustomFieldEditor extends JFrame {

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
        list.setVisibleRowCount(5);
        list.setPreferredSize(new Dimension(150, list.getPreferredSize().height));
        JScrollPane listScrollPane = new JScrollPane(list);

        addButton = new JButton(addString);
        AddFieldListener addListener = new AddFieldListener();
        addButton.addActionListener(addListener);
        removeButton = new JButton(removeString);
        removeButton.addActionListener(new RemoveFieldListener());
        renameButton = new JButton("Rename...");
        renameButton.addActionListener(new RenameListener());
        upButton = new JButton("Move up");
        upButton.addActionListener(new MoveUpListener());
        downButton = new JButton("Move down");
        downButton.addActionListener(new MoveDownListener());

        // Stack up the buttons
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.PAGE_AXIS));
        buttonPane.add(upButton);
        addVstrut(buttonPane);
        buttonPane.add(downButton);
        addVstrut(buttonPane);
        buttonPane.add(addButton);
        addVstrut(buttonPane);
        buttonPane.add(renameButton);
        addVstrut(buttonPane);
        buttonPane.add(removeButton);
        buttonPane.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));

        // Add list and buttons to the content pane
        contentPane = new JPanel(new BorderLayout());
        contentPane.add(listScrollPane, BorderLayout.CENTER);
        contentPane.add(buttonPane, BorderLayout.LINE_END);
        contentPane.setOpaque(true); // (compulsory)
        setContentPane(contentPane);
        pack();
        setLocationRelativeTo(PuffinApp.getInstance().getMainWindow());
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
            PuffinApp.getInstance().updateDisplay();
        }
    }

    class MoveDownListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            int index = list.getSelectedIndex();
            if (index>=fields.size()-1 || index==-1) return;
            fields.swapAdjacent(index);
            setFromFields();
            list.setSelectedIndex(index+1);
            list.ensureIndexIsVisible(index+1);
            PuffinApp.getInstance().updateDisplay();
        }
    }

    class RemoveFieldListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            int index = list.getSelectedIndex();
            if (index==-1) return;
            listModel.remove(index);
            fields.remove(index);
            int size = listModel.getSize();
            if (size > 0) {
                if (index == size) {
                    // last item removed, adjust current index
                    index--;
                }
                list.setSelectedIndex(index);
                list.ensureIndexIsVisible(index);
            }
            PuffinApp.getInstance().updateDisplay();
        }
    }

    class AddFieldListener implements ActionListener {
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
            PuffinApp.getInstance().updateDisplay();
        }
    }

    class RenameListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            int index = list.getSelectedIndex();
            if (index==-1) return;
            String name = fields.get(index);
            String newName = (String) JOptionPane.showInputDialog(
                    CustomFieldEditor.this,
                    "Please enter a new name for ‘"+name+"’.",
                    "Rename field",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    "");
            if (newName==null || newName.equals("") || listModel.contains(newName)) {
                return;
            }
            fields.set(index, newName);
            listModel.set(index, newName);
            PuffinApp.getInstance().updateDisplay();
        }

    }
}
