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

public class CustomFieldEditor extends JFrame
                      implements ListSelectionListener {

    private JPanel contentPane;
    private JList list;
    private DefaultListModel listModel;
    private static final String addString = "Add";
    private static final String removeLabel = "Remove";
    private JButton removeFieldButton;
    private JTextField customFieldName;

    public CustomFieldEditor() {
        super();
        listModel = new DefaultListModel();

        // Create the widgets
        list = new JList(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        list.addListSelectionListener(this);
        list.setVisibleRowCount(5);
        JScrollPane listScrollPane = new JScrollPane(list);

        JButton addFieldButton = new JButton(addString);
        AddFieldListener addListener = new AddFieldListener(addFieldButton);
        addFieldButton.setActionCommand(addString);
        addFieldButton.addActionListener(addListener);
        addFieldButton.setEnabled(false);

        removeFieldButton = new JButton(removeLabel);
        removeFieldButton.setActionCommand(removeLabel);
        removeFieldButton.addActionListener(new RemoveFieldListener());

        customFieldName = new JTextField(12);
        customFieldName.addActionListener(addListener);
        customFieldName.getDocument().addDocumentListener(addListener);

        // Stack up the buttons
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane,
                                           BoxLayout.LINE_AXIS));
        buttonPane.add(removeFieldButton);
        buttonPane.add(Box.createHorizontalStrut(5));
        buttonPane.add(new JSeparator(SwingConstants.VERTICAL));
        buttonPane.add(Box.createHorizontalStrut(5));
        buttonPane.add(customFieldName);
        buttonPane.add(addFieldButton);
        buttonPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        // Add list and buttons to the content pane
        contentPane = new JPanel(new BorderLayout());
        contentPane.add(listScrollPane, BorderLayout.CENTER);
        contentPane.add(buttonPane, BorderLayout.PAGE_END);
                contentPane.setOpaque(true); //content panes must be opaque
        setContentPane(contentPane);
        pack();
        setVisible(true);
    }

    class RemoveFieldListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            int index = list.getSelectedIndex();
            listModel.remove(index);
            int size = listModel.getSize();
            if (size == 0) {
                // grey out the remove button
                removeFieldButton.setEnabled(false);
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

    // listener for both the text field and the add button
    class AddFieldListener implements ActionListener, DocumentListener {
        private boolean alreadyEnabled = false;
        private JButton button;

        public AddFieldListener(JButton button) {
            this.button = button;
        }

        public void actionPerformed(ActionEvent e) {
            String name = customFieldName.getText();

            if (name.equals("") || listModel.contains(name)) {
                customFieldName.requestFocusInWindow();
                customFieldName.selectAll();
                return;
            }

            int newIndex = list.getSelectedIndex() + 1;
            // "no selection" == -1, so in that case it goes at the start
            listModel.insertElementAt(customFieldName.getText(), newIndex);
            customFieldName.requestFocusInWindow();
            customFieldName.setText("");
            list.setSelectedIndex(newIndex);
            list.ensureIndexIsVisible(newIndex);
        }

        public void insertUpdate(DocumentEvent e) {
            enableButton();
        }

        public void removeUpdate(DocumentEvent e) {
            handleEmptyTextField(e);
        }

        public void changedUpdate(DocumentEvent e) {
            if (!handleEmptyTextField(e)) {
                enableButton();
            }
        }

        private void enableButton() {
            if (!alreadyEnabled) {
                button.setEnabled(true);
            }
        }

        private boolean handleEmptyTextField(DocumentEvent e) {
            if (e.getDocument().getLength() <= 0) {
                button.setEnabled(false);
                alreadyEnabled = false;
                return true;
            }
            return false;
        }
    }

    public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting() == false) {
            removeFieldButton.setEnabled(list.getSelectedIndex() != -1);
        }
    }
}
