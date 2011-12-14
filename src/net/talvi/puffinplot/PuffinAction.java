package net.talvi.puffinplot;

import java.awt.Toolkit;
import java.awt.event.InputEvent;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

/**
 * PuffinAction is the superclass for most Actions used in PuffinPlot.
 * It provides convenient constructors for setting up an Action with
 * a tooltip and operating-system-appropriate keyboard shortcuts.
 *
 * @author pont
 */
public abstract class PuffinAction extends AbstractAction {
    private boolean specialMacMenuItem;
    
    /**
     * The standard modifier key for the platform; generally, this will
     * correspond to Ctrl on most systems, and ⌘ (‘Apple key’) on
     * Mac OS X systems.
     */
    public static final int modifierKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

    /**
     * Creates a new PuffinAction with the specified parameters.
     * 
     * @param name the name of the Action (used for the menu item)
     * @param description a short description of the Action (used for the tooltip)
     * @param accelerator the accelerator key (keyboard shortcut) for the Action.
     *        If {@code null}, no accelerator will be set
     * @param shift {@code true} if the accelerator should require shift to be held down
     * @param mnemonic mnemonic key (used to select the Action from a menu when the
     *        menu is open); if {@code null}, no mnemonic key will be set
     * @param specialMacMenuItem if {@code true}, no menu item should be
     *        created on Mac OS X, because this Action (e.g. ‘quit’) is
     *        conventionally reached via a different interface (e.g. the 
     *        application menu) under Mac OS X
     * @param modifier the modifier key (usually {@code 0} for no modifier
     *        key, or {@link #modifierKey} for an automatically chosen
     *        platform-appropriate modifier key (ctrl or ‘apple’).
     */
    public PuffinAction(String name, String description, Character accelerator,
            boolean shift, Integer mnemonic, boolean specialMacMenuItem, int modifier) {
        super(name);
        this.specialMacMenuItem = specialMacMenuItem;
        putValue(SHORT_DESCRIPTION, description);
        if (accelerator != null) {
            putValue(ACCELERATOR_KEY, 
                    KeyStroke.getKeyStroke(accelerator,
                    modifier | (shift ? InputEvent.SHIFT_DOWN_MASK : 0),
                    false));
        }
        if (mnemonic != null) {
            putValue(MNEMONIC_KEY, mnemonic);
        }
    }

    /**
     * Creates a new PuffinAction with the specified parameters.
     * 
     * @param name the name of the Action (used for the menu item)
     * @param description a short description of the Action (used for the tooltip)
     * @param accelerator the accelerator key (keyboard shortcut) for the Action.
     *        If {@code null}, no accelerator will be set
     * @param shift {@code true} if the accelerator should require shift to be held down
     * @param mnemonic mnemonic key (used to select the Action from a menu when the
     *        menu is open); if {@code null}, no mnemonic key will be set
     */
    public PuffinAction(String name, String description, Character accelerator,
            boolean shift, Integer mnemonic) {
        this(name, description, accelerator, shift, mnemonic, false, modifierKey);
    }

    /**
     * Creates a new PuffinAction with the specified parameters.
     * 
     * @param name the name of the Action (used for the menu item)
     * @param description a short description of the Action (used for the tooltip)
     */
    public PuffinAction(String name, String description) {
        this(name, description, null, false, null);
    }

    /**
     * Determines whether the Action should be excluded from the normal
     * application menus (on the grounds that it already exists in a 
     * special Mac OS X menu and the application is being run on Mac OS X).
     * 
     * @return {@code true} if this Action should not be shown on menus
     */
    public boolean isExcludedFromMenu() {
        return PuffinApp.getInstance().isOnOsX() && specialMacMenuItem;
    }
    
}
