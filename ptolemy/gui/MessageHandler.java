/* Singleton class for displaying exceptions, warnings, and messages.

 Copyright (c) 1999 The Regents of the University of California.
 All rights reserved.
 Permission is hereby granted, without written agreement and without
 license or royalty fees, to use, copy, modify, and distribute this
 software and its documentation for any purpose, provided that the above
 copyright notice and the following two paragraphs appear in all copies
 of this software.

 IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
 FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
 THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
 SUCH DAMAGE.

 THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
 CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 ENHANCEMENTS, OR MODIFICATIONS.
 
                                        PT_COPYRIGHT_VERSION_2
                                        COPYRIGHTENDKEY
@ProposedRating Yellow (eal@eecs.berkeley.edu)
@AcceptedRating Red (reviewmoderator@eecs.berkeley.edu)
*/

package ptolemy.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;


//////////////////////////////////////////////////////////////////////////
//// MessageHandler
/**
This is a static class that is used to report errors.
When an applet or application starts up, it should call setContext()
to specify a component with respect to which the display window
should be created.  This ensures that if the application is iconfied
or deiconified, that the display window goes with it. If the context
is not specified, then the display window is centered on the screen,
but iconifying and deiconifying may not work as desired.
<p>
This class is based on (and contains code from) the diva GUIUtilities
class.

@author  Edward A. Lee, Steve Neuendorffer, and John Reekie
@version $Id$
*/
public class MessageHandler {
    
    // This constructor is private because the class is a singleton.
    private MessageHandler() {
    }

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** Return a string that contains the original string, limited to the
     *  given number of characters.  If the string is truncated, elipses
     *  will be appended to the end of the string.
     *  @param string The string to truncate.
     *  @param length The length to which to truncate the string.
     */
    public static String ellipsis(String string, int length) {
	if(string.length() > length) {
	    return string.substring(0, length-3) + "...";
	}
	return string;
    }

    /** Show the specified message and exception information.
     *  If the exception is an instance of CancelException, then it
     *  is not shown.
     *  @param info The message.
     *  @param exception The exception.
     *  @see CancelException
     */
    public static void error(String info, Exception exception) {
        if (exception instanceof CancelException) return;
        Object[] message = new Object[1];
	String string;
	if(info != null) {
	    string = info + "\n" + exception.getMessage();
	} else {
	    string = exception.getMessage();
	}
	message[0] = ellipsis(string, 400);
	
        Object[] options = {"Dismiss", "Display Stack Trace"};

        // Show the MODAL dialog
        int selected = JOptionPane.showOptionDialog(
                _context,
                message,
                "Exception",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.ERROR_MESSAGE, 
                null,
                options,
                options[0]);
        
        if(selected == 1) {
            showStackTrace(exception, info);
        }
    }

    /** Set the component with respect to which the display window
     *  should be created.  This ensures that if the application is
     *  iconfied or deiconified, that the display window goes with it.
     *  @param context The component context.
     */
    public static void setContext(Component context) {
        _context = context;
    }

    /** Display a stack trace dialog. Eventually, the dialog should
     *  be able to email us a bug report. The "info" argument is a
     *  string printed at the top of the dialog instead of the Exception
     *  message.
     *  @param exception The exception.
     *  @param info A message.
     */
    public static void showStackTrace(Exception exception, String info) {
        // Show the stack trace in a scrollable text area.
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        JTextArea text = new JTextArea(sw.toString(), 60, 80);
        JScrollPane stext = new JScrollPane(text);
        stext.setPreferredSize(new Dimension(600, 300));
        text.setCaretPosition(0);
        text.setEditable(false);
        
        // We want to stack the text area with another message
        Object[] message = new Object[2];
        String string;
        if(info != null) {
            string = info + "\n" + exception.getMessage();
        } else {
            string = exception.getMessage();
        }
        message[0] = ellipsis(string, 400);
        message[1] = stext;
        
        // Show the MODAL dialog
        JOptionPane.showMessageDialog(
                _context,
                message,
                "Stack trace",
                JOptionPane.ERROR_MESSAGE);
    }

    /** Show the specified message in a modal dialog.  If the user
     *  clicks on the "Cancel" button, then throw an exception.
     *  This gives the user the option of not continuing the
     *  execution, something that is particularly useful if continuing
     *  execution will result in repeated warnings.
     *  @param info The message.
     *  @exception Exception If the user clicks on the "Cancel" button.
     */
    public static void warning(String info) throws CancelException {
        Object[] message = new Object[1];
        message[0] = info;
        Object[] options = {"OK", "Cancel"};

        // Show the MODAL dialog
        int selected = JOptionPane.showOptionDialog(
                _context,
                message,
                "Warning",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE, 
                null,
                options,
                options[0]);
        
        if(selected == 1) {
            throw new CancelException();
        }
    }

    ///////////////////////////////////////////////////////////////////
    ////                         private variables                 ////

    // The context.
    private static Component _context = null;
}
