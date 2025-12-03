/*
 * qStudio - Free SQL Analysis Tool
 * Copyright C 2013-2023 TimeStored
 *
 * Licensed under the Apache License, Version 2.0 the "License";
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.timestored.qstudio;

import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.swing.JEditorPane;
import javax.swing.ToolTipManager;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import com.google.common.base.MoreObjects;
import com.timestored.docs.Document;

/**
 * Displays an open document and keeps the model and view synchronized. 
 * Also allows setting a tooltip provider to set a custom tooltip. 
 */
class DocEditorPane extends JEditorPane {

	private static final long serialVersionUID = 1L;
	private static final String NL = System.getProperty("line.separator");

	private final Document document;
	private TooltipProvider tooltipProvider = null;

	private boolean modifyingSelf;

	/**
	 * Show the selected document, allow editing it and performing other actions on it. 
	 */
	public DocEditorPane(final Document document) {

		this.document = document;

		// handle both content changes and selection changes here.
		addCaretListener(new CaretListener() {
			
			@Override public void caretUpdate(CaretEvent e) {
				//TODO this creates a different large string on every single keystroke!
				if(!modifyingSelf) {
					modifyingSelf = true;
					String t = getText().replaceAll(NL, "\n");
					if(!t.equals(document.getContent())) {
						document.setContent(t);
					}
					
					document.setSelection(getSelectionStart(), getSelectionEnd(), e.getDot());
					modifyingSelf = false;
				}
			}
		});

        
        // lastly add myself as listener
        document.addListener(new Document.Listener() {

			@Override public void docContentModified() {
		        refreshContent();
			}

			@Override public void docCaratModified() {
				int docPos = document.getCaratPosition();
				if(docPos != getCaretPosition()) {
					setCaretPosition(docPos);
				}
			} 
			@Override public void docSaved() { } // irrelevant
        	
        });
        refreshContent();
        setCaretPosition(document.getCaratPosition());
        requestFocus();
		ToolTipManager.sharedInstance().registerComponent(this);
	}


	/** @return Get screen location near carat */
	Point getPopupPoint() {
		Point screen = getLocationOnScreen();
		Point carat = getCaret().getMagicCaretPosition();
		if(carat != null) {
			return new Point(screen.x + carat.x, screen.y + carat.y + 20);
		}
		return new Point(screen.x, screen.y + 20);
	}
	
	
	public Document getDoc() {
		return document;
	}

	public void setTooltipProvider(TooltipProvider tooltipProvider) {
		this.tooltipProvider = tooltipProvider;
	}

	private void refreshContent() {
		String docText = document.getContent();
		String editorText = getText().replaceAll(System.getProperty("line.separator"), "\n");
		
		boolean textDiff = (docText!=null && editorText!= null && docText.length()>0 && !docText.equals(editorText))
				|| !document.getContent().equals(editorText);
		
		if(textDiff) {
			modifyingSelf = true;
			setText(docText);
			setSelectionStart(document.getSelectionStart());
			setSelectionEnd(document.getSelectionEnd());
			setCaretPosition(document.getCaratPosition());
			modifyingSelf = false;
			requestFocus();
		}
	}

	@Override public String getToolTipText(MouseEvent e) {
		// override this to allow custom tooltip providers based on carat position of mouse.
		// Most tooltips are kdb specific. Only show for q/k/unknown file types
		if(tooltipProvider!=null && MyPreferences.INSTANCE.isShowTooltips()) {
		    Point pt = new Point(e.getX(), e.getY());
			return tooltipProvider.getToolTipText(e, viewToModel(pt));
		}
		return null;
	}

	@Override public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("document", document).toString();
	}

	/**
	 * Provides tooltips for {@link DocEditorPane} based on caret location under the mouse..
	 */
	public static interface TooltipProvider {
		/**
		 * @param pos the offset >= 0 from the start of the document to the caret under the mouse 
		 * 	if known, else -1
		 * @return The thext that should be shown in the tooltip, or null if no tooltip to show.
		 */
		abstract String getToolTipText(MouseEvent event, int caret);
	}
}
