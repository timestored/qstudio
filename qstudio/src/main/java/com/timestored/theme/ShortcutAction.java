package com.timestored.theme;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;


/** 
 * Allows compact construction of a control+(key) action with description etc. 
 */
public abstract class ShortcutAction extends AbstractAction {
	private static final long serialVersionUID = 1L;

	public ShortcutAction(String text, Icon icon, int mnemonicAccel) {
		this(text, icon, text, mnemonicAccel, mnemonicAccel);
	}

	public ShortcutAction(String text, Icon icon, String desc) {
		this(text, icon, desc, null, KeyEvent.VK_UNDEFINED);
	}
	
	public ShortcutAction(String text, Icon icon,
                      String desc, Integer mnemonic, 
                      int acceleratorKey) {
        super(text, icon!=null ? icon.get16() : null);
        putValue(SHORT_DESCRIPTION, desc);
        if(mnemonic != null) {
            putValue(MNEMONIC_KEY, mnemonic);	
        }
		KeyStroke k;
		if(KeyEvent.VK_UNDEFINED != acceleratorKey) {
			k = KeyStroke.getKeyStroke(acceleratorKey, 
					Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
	        putValue(ACCELERATOR_KEY, k);
		}
    }

	public ShortcutAction(String text, Icon icon,
                      String desc, Integer mnemonic, 
                      KeyStroke k) {
        super(text, icon!=null ? icon.get16() : null);
        putValue(SHORT_DESCRIPTION, desc);
        if(mnemonic != null) {
            putValue(MNEMONIC_KEY, mnemonic);	
        }
		if(k != null) {
	        putValue(ACCELERATOR_KEY, k);
		}
    }
    
	
    @Override public abstract void actionPerformed(ActionEvent e);
}
