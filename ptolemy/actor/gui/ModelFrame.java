/* Top-level window containing a Ptolemy II model.

 Copyright (c) 1998-2000 The Regents of the University of California.
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
@AcceptedRating Red (eal@eecs.berkeley.edu)
*/

// FIXME: To do:
//  - Fix printing.
//  - Handle file changes (warn when discarding modified models).

package ptolemy.actor.gui;

// Ptolemy imports
import ptolemy.actor.CompositeActor;
import ptolemy.actor.ExecutionListener;
import ptolemy.actor.Manager;
import ptolemy.actor.Director;
import ptolemy.gui.Top;
import ptolemy.kernel.CompositeEntity;

// Java imports
import javax.swing.JOptionPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.util.Vector;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

//////////////////////////////////////////////////////////////////////////
//// ModelFrame
/**

ModelFrame is a top-level window containing a Ptolemy II model.
If contains a ModelPane, but adds a menu bar and a status bar for
message reporting.
<p>
An application that uses this class should set up the handling of
window-closing events.  Presumably, the application will exit when
all windows have been closed. This is done with code something like:
<pre>
    modelFrameInstance.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
            // Handle the event
        }
    });
</pre>

@author Edward A. Lee
@version $Id$
*/
public class ModelFrame extends Top implements ExecutionListener {

    /** Construct a frame to control the specified Ptolemy II model.
     *  After constructing this, it is necessary
     *  to call setVisible(true) to make the frame appear.
     *  A controlling application must be specified; this class
     *  delegates opening of new models to that application.
     *  @param model The model to put in this frame, or null if none.
     *  @param application The controlling application.
     */
    public ModelFrame(CompositeActor model, PtolemyApplication application) {
        super();
        _model = model;
        _application = application;

        // Create first with no model to avoid duplicating work when
        // we next call setModel().
        _pane = new ModelPane(null);
        setModel(model);

        getContentPane().add(_pane, BorderLayout.CENTER);

        // Make the go button the default.
        _pane.setDefaultButton();

        // Debug menu
        JMenuItem[] debugMenuItems = {
            new JMenuItem("Listen to Manager", KeyEvent.VK_M),
            new JMenuItem("Listen to Director", KeyEvent.VK_D),
        };
        _debugMenu.setMnemonic(KeyEvent.VK_D);
        DebugMenuListener sml = new DebugMenuListener();
        // Set the action command and listener for each menu item.
        for(int i = 0; i < debugMenuItems.length; i++) {
            debugMenuItems[i].setActionCommand(
                    debugMenuItems[i].getText());
            debugMenuItems[i].addActionListener(sml);
            _debugMenu.add(debugMenuItems[i]);
        }
        _menubar.add(_debugMenu);

        // FIXME: Need to do something with the progress bar in the status bar.
    }

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** Report that an execution error has occurred.  This method
     *  is called by the specified manager.
     *  @param manager The manager calling this method.
     *  @param ex The exception being reported.
     */
    public void executionError(Manager manager, Exception ex) {
        report(ex);
    }

    /** Report that execution of the model has finished.
     *  @param manager The manager calling this method.
     */
    public synchronized void executionFinished(Manager manager) {
        report("execution finished.");
    }

    /** Get the associated model.
     *  @return The associated model.
     */
    public CompositeActor getModel() {
        return _model;
    }

    /** Report that a manager state has changed.
     *  This is method is called by the specified manager.
     *  @param manager The manager calling this method.
     */
    public void managerStateChanged(Manager manager) {
        Manager.State newstate = manager.getState();
        if (newstate != _previousState) {
            report(manager.getState().getDescription());
            _previousState = newstate;
        }
    }

    /** Return the container into which to place placeable objects.
     *  @return A container for graphical displays.
     */
    public ModelPane modelPane() {
        return _pane;
    }

    /** Set background color.  This overrides the base class to set the
     *  background of contained ModelPane.
     *  @param background The background color.
     */
    public void setBackground(Color background) {
        super.setBackground(background);
        // This seems to be called in a base class constructor, before
        // this variable has been set. Hence the test against null.
        if (_pane != null) _pane.setBackground(background);
    }

    /** Set the associated model.
     *  @param model The associated model.
     */
    public void setModel(CompositeActor model) {
        _model = model;
        if (model != null) {
            _pane.setModel(model);
            setTitle(model.getName());
            Manager manager = model.getManager();
            if (manager != null) {
                manager.addExecutionListener(this);
            }
        }
    }

    ///////////////////////////////////////////////////////////////////
    ////                         protected methods                 ////

    /** Open a dialog with basic information about this window.
     */
    protected void _about() {
        JOptionPane.showMessageDialog(this,
                "This is a control panel for a Ptolemy II model.\n" +
                "By: Claudius Ptolemeus, ptolemy@eecs.berkeley.edu\n" +
                "Version 1.0, Build: $Id$\n\n"+
                "For more information, see\n" +
                "http://ptolemy.eecs.berkeley.edu/ptolemyII\n\n" +
                "Copyright (c) 1997-2000, " +
                "The Regents of the University of California.",
                "About Ptolemy II", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Clear the current contents.  First, check to see whether
     *  the contents have been modified, and if so, then prompt the user
     *  to save them.  A return value of false
     *  indicates that the user has canceled the action.
     *  @return False if the user cancels the clear.
     */
    protected boolean _clear() {
        if (super._clear()) {
            setModel(new CompositeActor());
            return true;
        } else {
            return false;
        }
    }

    /** Display more detailed information than given by _about().
     */
    protected void _help() {
        String message = "Ptolemy II model.";
        if (_model != null) {
            String tip = Documentation.consolidate(_model);
            if (tip != null) {
                message = "Ptolemy II model:\n" + tip;
            }
        }
        JOptionPane.showMessageDialog(this, message,
                "About " + getTitle(), JOptionPane.INFORMATION_MESSAGE);
    }

    /** Read the specified URL.
     *  @param url The URL to read.
     *  @exception IOException If the URL cannot be read.
     */
    protected void _read(URL url) throws IOException {
        // NOTE: Used to use for the first argument the following, but
        // it seems to not work for relative file references:
        // new URL("file", null, _directory.getAbsolutePath()
        _application._read(url, url.openStream());
    }

    /** Print the contents.
     */
    protected void _print() {
        // FIXME: What should we print?  Plots?  How?
        super._print();
    }

    /** Write the model to the specified file.
     *  @param file The file to write to.
     *  @exception IOException If the write fails.
     */
    protected void _writeFile(File file) throws IOException {
        java.io.FileWriter fout = new java.io.FileWriter(file);
        _model.exportMoML(fout);
        fout.close();
    }

    ///////////////////////////////////////////////////////////////////
    ////                         protected variables               ////

    /** @serial Debug menu for this frame. */
    protected JMenu _debugMenu = new JMenu("Debug");

    ///////////////////////////////////////////////////////////////////
    ////                         private variables                 ////

    // The controlling application. This class delegates opening new models
    // to that application.
    private PtolemyApplication _application;

    // The model that this window controls, if any.
    private CompositeActor _model;

    // The pane in which the model data is displayed.
    private ModelPane _pane;

    // The previous state of the manager, to avoid reporting it if it hasn't
    // changed.
    private Manager.State _previousState;

    ///////////////////////////////////////////////////////////////////
    ////                         inner classes                     ////

    /** Listener for debug menu commands. */
    class DebugMenuListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            JMenuItem target = (JMenuItem)e.getSource();
            String actionCommand = target.getActionCommand();
            final TopDebugListener listener = new TopDebugListener();
            if (actionCommand.equals("Listen to Manager")) {
                final Manager manager = _model.getManager();
                if (manager != null) {
                    manager.addDebugListener(listener);
                    // Listen for window closing events to unregister.
                    listener.addWindowListener(new WindowAdapter() {
                        public void windowClosing(WindowEvent e) {
                            manager.removeDebugListener(listener);
                        }
                    });
                }
            } else if (actionCommand.equals("Listen to Director")) {
                final Director director = _model.getDirector();
                if (director != null) {
                    director.addDebugListener(listener);
                    // Listen for window closing events to unregister.
                    listener.addWindowListener(new WindowAdapter() {
                        public void windowClosing(WindowEvent e) {
                            director.removeDebugListener(listener);
                        }
                    });
                }
            }
        }
    }
}
