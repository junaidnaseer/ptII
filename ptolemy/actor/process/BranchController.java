/* A BranchController manages the execution of a set of branch objects by 
monitoring whether the branches have blocked. 

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

@ProposedRating Red (davisj@eecs.berkeley.edu)
@AcceptedRating Red (davisj@eecs.berkeley.edu)

*/

package ptolemy.actor.process;

// Ptolemy imports.
import ptolemy.actor.*;
import ptolemy.actor.process.*;
import ptolemy.data.Token;
import ptolemy.kernel.*;
import ptolemy.kernel.util.*;

// Java imports
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

//////////////////////////////////////////////////////////////////////////
//// BranchController
/**
A BranchController manages the execution of a set of branch objects by 
monitoring whether the branches have blocked. A branch blocks when it is 
either unable to get data from its producer receiver or put data into its 
consumer receiver. When a branch blocks, it registers the block with its 
branch controller by passing the specific receiver that is blocked. If all 
of a branch controllers branches are blocked, then the branch controller 
informs the director associated with its containing composite actors. 
<P>
Branches are assigned to a branch controller by the director associated 
with the controller's composite actor via the addBranches() method. This
method takes an io port and determine's the port's receivers. Branches
are then instantiated and assigned to the receivers according to whether
the receivers are producer or consumer receivers. 


@author John S. Davis II
@version $Id$
*/

public class BranchController implements Runnable {

    /** Construct a branch controller in the specified composite actor
     *  container.
     *
     *  @param container The parent actor that contains this object.
    */
    public BranchController(CompositeActor container) {
        _parentActor = container;
        _parentName = ((Nameable)container).getName();
    }

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** Activate the branches that are managed by this branch
     *  controller. This method should be invoked once when
     *  a branch controller first starts the branches it controls.
     *  Invocation of this method will cause the branches to
     *  begin transferring tokens between their assigned producer
     *  and consumer receiver. 
     */
    public void activateBranches() {
        synchronized(this) {
            if( !hasBranches() ) {
                return;
            }
            setActive(true);
            Branch branch = null;
            for( int i=0; i < _branches.size(); i++ ) {
                branch = (Branch)_branches.get(i);
                branch.run();
            }
        }
    }
    
    /** Add branches corresponding to the channels of the port
     *  argument. The port must be contained by the same actor
     *  that contains this controller. If branches corresponding
     *  to the specified port have already been added to this 
     *  controller, then an IllegalActionException will be thrown. 
     *  If the input/output polarity of this port does not match that
     *  of ports for whom branches have been previously added
     *  to this controller, then throw an IllegalActionException.
     *  @param port The port for which branches will be added to this
     *   controller.
     *  @exception IllegalActionException If branches for the
     *   port have been previously added to this controller or
     *   if the port input/output polarity does not match that
     *   of ports for whom branches were previously add to this
     *   controller.
     */
    public void addBranches(IOPort port) throws 
            IllegalActionException {
	if( port.getContainer() != getParent() ) {
	    throw new IllegalActionException("Can not contain "
		    + "a port that is not contained by this "
		    + "BranchController's container.");
	}
        
        if( _ports.contains(port) ) {
            throw new IllegalActionException(port, "This port "
            	    + "is already controlled by this " 
                    + "BranchController");
        }
        // Careful; maintain order of following test in case
        // Java is like C
        if( _hasInputPorts() && !port.isInput() ) {
	    throw new IllegalActionException("BranchControllers "
            	    + "must contain only input ports or only output "
                    + "ports; not both");
        }
        if( _hasOutputPorts() && !port.isOutput() ) {
	    throw new IllegalActionException("BranchControllers "
            	    + "must contain only input ports or only output "
                    + "ports; not both");
        }
        _ports.add(port);

	Branch branch = null;
	ProcessReceiver prodRcvr = null;
	ProcessReceiver consRcvr = null;
	Receiver[][] prodRcvrs = null;
	Receiver[][] consRcvrs = null;

	for( int i=0; i < port.getWidth(); i++ ) {
	    if( port.isInput() ) {
		prodRcvrs = port.getReceivers();
		consRcvrs = port.deepGetReceivers();
	    } else if( port.isOutput() ) {
		prodRcvrs = port.getInsideReceivers();
		consRcvrs = port.getRemoteReceivers();
	    } else {
		throw new IllegalActionException("Bad news");
	    }
            
	    prodRcvr = (ProcessReceiver)prodRcvrs[i][0];
	    consRcvr = (ProcessReceiver)consRcvrs[i][0];

	    branch = new Branch( prodRcvr, consRcvr, this );
	    _branches.add(branch);
	}
    }

    /** Deactive the branches assigned to this branch controller.
     */
    public synchronized void deactivateBranches() {
        // System.out.println(_parentName+": branch controller calling deactivate()");
	setActive(false);
        Iterator branches = _branches.iterator();
        Branch branch = null;
        ProcessReceiver bRcvr = null;
        while (branches.hasNext()) {
            branch = (Branch)branches.next();
            branch.setActive(false);
            bRcvr = branch.getConsReceiver();
            synchronized(bRcvr) {
                bRcvr.notifyAll();
            }
            bRcvr = branch.getProdReceiver();
            synchronized(bRcvr) {
                bRcvr.notifyAll();
            }
        }
	notifyAll();
        // System.out.println(_parentName+": branch controller ending deactivate()");
    }

    /** Returned a linked list of the blocked receivers associated
     *  with this branch controller.
     *  @return A linked list of the blocked receivers associated 
     *   with this branch controller.
     */
    public LinkedList getBlockedReceivers() {
	return _blockedReceivers;
    }

    /** Return the list of branches controlled by this controller.
     *  @return The list of branches controlled by this controller.
     */
    public LinkedList getBranchList() {
        return _branches;
    }
    
    /** Return the composite actor that contains this branch 
     *  controller.
     *  @return The composite actor that contains this controller.
     */
    public CompositeActor getParent() {
        return _parentActor;
    }

    /** Return true if this branch controller controls one or more 
     *  branches; return false otherwise.
     *  @return True if this controller controls one or more branches;
     *   return false otherwise.
     */
    public boolean hasBranches() {
        return _branches.size() > 0;
    }
    
    /** Return true if this controller is active; return false 
     *  otherwise.
     * @return True if this controller is active; false otherwise.
     */
    public boolean isActive() {
	return _isActive;
    }

    /** Return true if all of the branches assigned to this branch 
     *  controller are blocked or if this branch controller has no 
     *  branches; return false otherwise. 
     *  @return True if all branches controlled by this branch
     *   controller are blocked or if this branch controller has
     *   no branches; return false otherwise.
     */
    public synchronized boolean isBlocked() {
        if( !hasBranches() ) {
            return true;
        }
        if( _branchesBlocked >= _branches.size() ) {
            if( _branchesBlocked > 0 ) {
                return true;
            }
        }
        return false;
    }
    
    /** Begin executing the branches associated with this branch
     *  controller so that they will begin transfering data in
     *  their assigned channels. If all of the branches become
     *  blocked then the director associated with this branch
     *  branch controller is notified.
     */
    public void run() {
    	synchronized(this) {
            try {
                activateBranches();
                while( isActive() ) {
                    while( !isBlocked() && isActive() ) {
                        wait();
                    }
                    while( isBlocked() && isActive() ) {
			_getDirector()._controllerBlocked(this);
                        wait();
                    }
		    _getDirector()._controllerUnBlocked(this);
                }
	    } catch( InterruptedException e ) {
		// Do something
            }
        }
    }
    
    /** Set this branch controller active if the active parameter is
     *  true; set this branch controller to inactive otherwise.
     *  @param active The indicator of whether this branch controller
     *   will be set active or inactive.  
     */
    public void setActive(boolean active) {
	_isActive = active;
    }

    ///////////////////////////////////////////////////////////////////
    ////                         protected methods                 ////

    /** Increase the count of branches that are blocked trying to do
     *  a read or write and register the receiver instigating the 
     *  blocked branch. If all the assigned branches are blocked, 
     *  then this branch controller will be registered as blocked with
     *  the controlling director. 
     *  @param rcvr The process receiver that is being registered
     *   as blocked.
     */
    protected void _branchBlocked(ProcessReceiver rcvr) {
        synchronized(this) {
            _branchesBlocked++;
	    _blockedReceivers.addFirst(rcvr);
	    notifyAll();
        }
    }

    /** Decrease the count of branches that are read or write blocked
     *  and note the receiver that is no longer blocked. If the branch
     *  controller was previously registered as being blocked, register 
     *  this branch controller with the director as no longer being 
     *  blocked.
     */
    protected void _branchUnBlocked(ProcessReceiver rcvr) {
        synchronized(this) {
            if( _branchesBlocked > 0 ) {
                _branchesBlocked--;
            }
	    _blockedReceivers.remove(rcvr);
	    notifyAll();
        }
    }

    ///////////////////////////////////////////////////////////////////
    ////                         private methods                   ////

    /** Return the director that controls the execution of this 
     *  branch controller's containing composite actor. 
     *  @return The composite process director that is associated
     *   with this branch controller's container.
     */
    private CompositeProcessDirector _getDirector() {
        try {
	    return  (CompositeProcessDirector)_parentActor.getDirector();
        } catch (NullPointerException ex) {
            // If a thread has a reference to a receiver with no director it
            // is an error so terminate the process.
	    String name = ((Nameable)getParent()).getName();
	    throw new TerminateProcessException("Error: " +
		    name + " contains a branch controller that has a " +
		    "receiver that does not have a director");
	}
    }

    /** Return true if this branch controller has input ports associated
     *  with it; return false otherwise.
     *  @return True if this branch controller has input ports associated
     *  with it. False otherwise.
     */
    private boolean _hasInputPorts() {
    	if( _ports.size() == 0 ) {
            return false;
        }
        Iterator ports = _ports.iterator();
        while( ports.hasNext() ) {
            IOPort port = (IOPort)ports.next();
            return port.isInput();
        }
        return false;
    }
    
    /** Return true if this branch controller has output ports associated
     *  with it; return false otherwise.
     *  @return True if this branch controller has output ports associated
     *  with it. False otherwise.
     */
    private boolean _hasOutputPorts() {
    	if( _ports.size() == 0 ) {
            return false;
        }
        Iterator ports = _ports.iterator();
        while( ports.hasNext() ) {
            IOPort port = (IOPort)ports.next();
            return port.isOutput();
        }
        return false;
    }
    
    ///////////////////////////////////////////////////////////////////
    ////                         private variables                 ////

    // The number of branches that are blocked
    private int _branchesBlocked = 0;
    
    // The CompositeActor that owns this controller object.
    private CompositeActor _parentActor;

    private LinkedList _branches = new LinkedList(); 
    private LinkedList _ports = new LinkedList();
    private LinkedList _blockedReceivers = new LinkedList();
    
    private boolean _isActive = false;
    
    
    private String _parentName = null;

}
