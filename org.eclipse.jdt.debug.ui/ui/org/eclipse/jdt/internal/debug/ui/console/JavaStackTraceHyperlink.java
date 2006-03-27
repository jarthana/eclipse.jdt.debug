/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.debug.ui.console;


import com.ibm.icu.text.MessageFormat;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.debug.core.JavaDebugUtils;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.internal.debug.ui.actions.OpenTypeAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.console.IHyperlink;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * A hyperlink from a stack trace line of the form "*(*.java:*)"
 */
public class JavaStackTraceHyperlink implements IHyperlink {
	
	private TextConsole fConsole;

	/**
	 * Constructor for JavaStackTraceHyperlink.
	 */
	public JavaStackTraceHyperlink(TextConsole console) {
		fConsole = console;
	}

	/**
	 * @see org.eclipse.debug.ui.console.IConsoleHyperlink#linkEntered()
	 */
	public void linkEntered() {
	}

	/**
	 * @see org.eclipse.debug.ui.console.IConsoleHyperlink#linkExited()
	 */
	public void linkExited() {
	}

	/**
	 * @see org.eclipse.debug.ui.console.IConsoleHyperlink#linkActivated()
	 */
	public void linkActivated() {
		try {
			String typeName;
            int lineNumber;
            try {
                String linkText = getLinkText();
                typeName = getTypeName(linkText);
                lineNumber = getLineNumber(linkText);
            } catch (CoreException e1) {
                ErrorDialog.openError(JDIDebugUIPlugin.getActiveWorkbenchShell(), ConsoleMessages.JavaStackTraceHyperlink_Error, ConsoleMessages.JavaStackTraceHyperlink_Error, e1.getStatus()); 
                return;
            }
			
			// documents start at 0
			if (lineNumber > 0) {
				lineNumber--;
			}
			Object sourceElement = getSourceElement(typeName);
			if (sourceElement != null) {
				IDebugModelPresentation presentation = JDIDebugUIPlugin.getDefault().getModelPresentation();
				IEditorInput editorInput = presentation.getEditorInput(sourceElement);
				if (editorInput != null) {
					String editorId = presentation.getEditorId(editorInput, sourceElement);
					if (editorId != null) {
						IEditorPart editorPart = JDIDebugUIPlugin.getActivePage().openEditor(editorInput, editorId);
						if (editorPart instanceof ITextEditor && lineNumber >= 0) {
							ITextEditor textEditor = (ITextEditor)editorPart;
							IDocumentProvider provider = textEditor.getDocumentProvider();
							provider.connect(editorInput);
							IDocument document = provider.getDocument(editorInput);
							try {
								IRegion line = document.getLineInformation(lineNumber);
								textEditor.selectAndReveal(line.getOffset(), line.getLength());
							} catch (BadLocationException e) {
                                MessageDialog.openInformation(JDIDebugUIPlugin.getActiveWorkbenchShell(), ConsoleMessages.JavaStackTraceHyperlink_0, MessageFormat.format("{0}{1}{2}", new String[] {(lineNumber+1)+"", ConsoleMessages.JavaStackTraceHyperlink_1, typeName}));  //$NON-NLS-2$ //$NON-NLS-1$
							}
							provider.disconnect(editorInput);
						}
						return;
					}
				}
			}
			// did not find source
			MessageDialog.openInformation(JDIDebugUIPlugin.getActiveWorkbenchShell(), ConsoleMessages.JavaStackTraceHyperlink_Information_1, MessageFormat.format(ConsoleMessages.JavaStackTraceHyperlink_Source_not_found_for__0__2, new String[] {typeName}));  
		} catch (CoreException e) {
			JDIDebugUIPlugin.errorDialog(ConsoleMessages.JavaStackTraceHyperlink_An_exception_occurred_while_following_link__3, e); 
			return;
		}
	}
	
	/**
	 * Returns the source element associated with the given type name,
	 * or <code>null</code> if none.
	 * 
	 * @param typeName type name to search for source element
	 * @return the source element associated with the given type name,
	 * or <code>null</code> if none
	 */
	protected Object getSourceElement(String typeName) throws CoreException {
		ILaunch launch = getLaunch();
		Object result = null;
		if (launch != null) {
			result = JavaDebugUtils.resolveSourceElement(typeName, getLaunch());
		}
		if (result == null) {
			// search for the type in the workspace
			result = OpenTypeAction.findTypeInWorkspace(typeName);
		}
		return result;
	}
	
	/**
	 * Returns the launch associated with this hyperlink, or
	 *  <code>null</code> if none
	 * 
	 * @return the launch associated with this hyperlink, or
	 *  <code>null</code> if none
	 */
	private ILaunch getLaunch() {
		IProcess process = (IProcess) getConsole().getAttribute(IDebugUIConstants.ATTR_CONSOLE_PROCESS);
		if (process != null) {
		    return process.getLaunch();
		}
		return null;
	}

	/**
	 * Returns the fully qualified name of the type to open
	 *  
	 * @return fully qualified type name
	 * @exception CoreException if unable to parse the type name
	 */
	protected String getTypeName(String linkText) throws CoreException {
        int start = linkText.indexOf('(');
        int end = linkText.indexOf(':');
        if (start >= 0 && end > start) {
            //linkText could be something like packageA.TypeB(TypeA.java:45)
            //need to look in packageA.TypeA for line 45 since TypeB is defined
            //in TypeA.java 
            //Inner classes can be ignored because we're using file and line number
            
            // get File name (w/o .java)
            String typeName = linkText.substring(start + 1, end);
            typeName = JavaCore.removeJavaLikeExtension(typeName);

            String qualifier = linkText.substring(0, start);
            // remove the method name
            start = qualifier.lastIndexOf('.');

            if (start >= 0) {
	            // remove the class name
	            start = new String((String) qualifier.subSequence(0, start)).lastIndexOf('.');
	            if (start == -1) {
	                start = 0; // default package
	            }
            }

            if (start >= 0) {
                qualifier = qualifier.substring(0, start);
            }
            
            if (qualifier.length() > 0) {
                typeName = qualifier + "." + typeName; //$NON-NLS-1$
            }
            return typeName;
        }
        IStatus status = new Status(IStatus.ERROR, JDIDebugUIPlugin.getUniqueIdentifier(), 0, ConsoleMessages.JavaStackTraceHyperlink_Unable_to_parse_type_name_from_hyperlink__5, null); 
        throw new CoreException(status);
    }	
	
	/**
	 * Returns the line number associated with the stack trace or -1 if none.
	 * 
	 * @exception CoreException if unable to parse the number
	 */
	protected int getLineNumber(String linkText) throws CoreException {
		int index = linkText.lastIndexOf(':');
		if (index >= 0) {
			String numText = linkText.substring(index + 1);
			index = numText.indexOf(')');
			if (index >= 0) {
				numText = numText.substring(0, index);
			}
			try {
				return Integer.parseInt(numText);
			} catch (NumberFormatException e) {
				IStatus status = new Status(IStatus.ERROR, JDIDebugUIPlugin.getUniqueIdentifier(), 0, ConsoleMessages.JavaStackTraceHyperlink_Unable_to_parse_line_number_from_hyperlink__6, e); 
				throw new CoreException(status);
			}		
		}
		IStatus status = new Status(IStatus.ERROR, JDIDebugUIPlugin.getUniqueIdentifier(), 0, ConsoleMessages.JavaStackTraceHyperlink_Unable_to_parse_line_number_from_hyperlink__7, null); 
		throw new CoreException(status);			
	}
	
	/**
	 * Returns the console this link is contained in.
	 *  
	 * @return console
	 */
	protected TextConsole getConsole() {
		return fConsole;
	}
	
	/**
	 * Returns this link's text
	 * 
	 * @exception CoreException if unable to retrieve the text
	 */
	protected String getLinkText() throws CoreException {
	    try {
            IDocument document = getConsole().getDocument();
	        IRegion region = getConsole().getRegion(this);
            int regionOffset = region.getOffset();
            
	        int lineNumber = document.getLineOfOffset(regionOffset);
	        IRegion lineInformation = document.getLineInformation(lineNumber);
            int lineOffset = lineInformation.getOffset();
	        String line = document.get(lineOffset, lineInformation.getLength());
            
            int regionOffsetInLine = regionOffset - lineOffset;

            int linkEnd = line.indexOf(')', regionOffsetInLine);
            int linkStart = line.lastIndexOf(' ', regionOffsetInLine);
            
            return line.substring(linkStart==-1?0:linkStart+1,linkEnd+1);
		} catch (BadLocationException e) {
			IStatus status = new Status(IStatus.ERROR, JDIDebugUIPlugin.getUniqueIdentifier(), 0, ConsoleMessages.JavaStackTraceHyperlink_Unable_to_retrieve_hyperlink_text__8, e); 
			throw new CoreException(status);
		}		
	}

}
