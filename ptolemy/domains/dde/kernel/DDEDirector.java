/* A DDEDirector governs the execution of actors operating according
to the DDE model of computation.

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
@AcceptedRating Red (cxh@eecs.berkeley.edu)

*/

package ptolemy.domains.dde.kernel;

import ptolemy.kernel.*;
import ptolemy.kernel.util.*;
import ptolemy.kernel.event.*;
import ptolemy.actor.*;
import ptolemy.actor.process.*;
import ptolemy.data.*;
import ptolemy.data.expr.Parameter;
import ptolemy.domains.pn.kernel.BasePNDirector; // For Javadoc

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Comparator;
import java.util.Collections;
import java.util.Iterator;

//////////////////////////////////////////////////////////////////////////
//// DDEDirector
/**
A DDEDirector governs the execution of actors operating according
to the DDE model of computation (MoC). The DDE MoC incorporates
a distributed notion of time into a dataflow style communication
semantic. Much of the functionality of the DDEDirector is consistent
with the Process Networks director PNDirector. In particular, the
mechanism for dealing with blocking due to empty or full queues is
functionally identical for the DDEDirector and PNDirector.
<P>
The DDE domain's use of time serves as the point of divergence in
the respective designs of the DDE and PN directors. In a network of
actors governed by a DDEDirector each actor has a local notion of
time. Several features of the DDEDirector are intended to facilitate
these local notions of time.
<P>
All DDE models have a completion time. The completion time is a preset
time after which all execution ceases. The completion time for a
DDEDirector is specified via the <I>stopTime</I> parameter. The value
of the stopTime parameter is passed to the receivers of all actors
that the DDEDirector governs via newReceiver() during initialize().
After initialize() has been called, the value of stopTime can not be
changed.
<P>
The default value of the stopTime parameter is
PrioritizedTimedQueue.ETERNITY. Given this value, a DDE model will
continue executing without regard for a completion time.
<P>
Deadlock due to feedback loops is dealt with via NullTokens. When an
actor in a DDE model receives a NullToken, it may advance its local
time value even though no computation results directly from
consumption of the NullToken. For models with feedback topologies,
the actor FeedBackDelay should be used.
<P>
The DDE model of computation assumes that valid time stamps have
non-negative values. Three special purpose negative time values
are reserved with the following meanings. The value of
PrioritizedTimedQueue.INACTIVE is reserved to indicate the termination
of a receiver. The value of PrioritizedTimedQueue.ETERNITY is reserved
to indicate that a receiver has not begun to participate in a model's
execution. The value of PrioritizedTimedQueue.IGNORE is reserved to
indicate that the current token at the head of a DDEReceiver should
be ignored in favor of the tokens contained in the other receivers
of the actor in question. More details of IGNORE can be found in
FeedBackDelay.
<P>
NOTE: The current implementation of this director does not
include an infrastructure for mutations. Hence, ChangeRequest
and other facilities for changing the topology of a model are
not included in this director.


@author John S. Davis II, Mudit Goel
@version $Id$
@see ptolemy.domains.pn.kernel.BasePNDirector
@see ptolemy.domains.dde.kernel.FeedBackDelay
@see ptolemy.domains.dde.kernel.NullToken
*/
public class DDEDirector extends CompositeProcessDirector {

    /** Construct a DDEDirector in the default workspace with
     *  an empty string as its name. The director is added to
     *  the list of objects in the workspace. Increment the
     *  version number of the workspace.
     */
    public DDEDirector() {
        super();

	try {
            double val = PrioritizedTimedQueue.ETERNITY;
	    stopTime = new
                Parameter(this, "stopTime", new DoubleToken(val) );
	} catch( IllegalActionException e ) {
	    throw new InternalErrorException( e.toString() );
        } catch (NameDuplicationException e) {
            throw new InvalidStateException( e.toString() );
	}
    }

    /** Construct a director in the  workspace with an empty
     *  string as a name. The director is added to the list of
     *  objects in the workspace. Increment the version number
     *  of the workspace.
     * @param workspace The workspace of this object.
     */
    public DDEDirector(Workspace workspace) {
        super(workspace);

	try {
            double val = PrioritizedTimedQueue.ETERNITY;
	    stopTime = new
                Parameter(this, "stopTime", new DoubleToken(val) );
	} catch( IllegalActionException e ) {
	    throw new InternalErrorException( e.toString() );
        } catch (NameDuplicationException e) {
            throw new InvalidStateException( e.toString() );
	}
    }

    /** Construct a director in the given container with the
     *  given name. If the container argument must not be null,
     *  or a NullPointerException will be thrown. The given
     *  name must be unique with respect to the container. If
     *  the name argument is null, then the name is set to the
     *  empty string. Increment the version number of the workspace.
     * @param container The container of this director.
     * @param name Name of this director.
     * @exception IllegalActionException If the director is not compatible
     * with the specified container.  May be thrown in a derived class.
     */
    public DDEDirector(CompositeActor container, String name)
            throws IllegalActionException {
        super(container, name);

	try {
            double val = PrioritizedTimedQueue.ETERNITY;
	    stopTime = new
                Parameter(this, "stopTime", new DoubleToken(val) );
	} catch( IllegalActionException e ) {
	    throw new InternalErrorException( e.toString() );
        } catch (NameDuplicationException e) {
            throw new InvalidStateException( e.toString() );
	}
    }

    ///////////////////////////////////////////////////////////////////
    ////                         public variables                  ////

    public Parameter stopTime;

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** Return the current time of the DDEThread that calls this
     *  method on behalf of an actor. If this method is called by
     *  other than a DDEThread, then return the current time as
     *  specified by the superclass of this method.
     * @return The current time of the DDEThread that calls this
     *  method.
     */
    public double getCurrentTime() {
	Thread thread = Thread.currentThread();
	if( thread instanceof DDEThread ) {
	    TimeKeeper tk = ((DDEThread)thread).getTimeKeeper();
	    return tk.getCurrentTime();
	} else {
	    return super.getCurrentTime();
	}
    }

    /** Wait until a deadlock is detected. Then handle the deadlock
     *  (by calling the protected method _handleDeadlock()) and return.
     *  This method is synchronized on the director.
     *  @exception IllegalActionException If a derived class throws it.
    public void fire() throws IllegalActionException {
	Workspace workspace = workspace();
        synchronized (this) {
            while( !_areActorsDeadlocked() ) {
            // while( !_areActorsStopped() && !_areActorsDeadlocked() ) {
		workspace.wait(this);
            }
            if( _areActorsDeadlocked() ) {
                if( _isInternallyReadDeadlocked() ) {

                    _notDone = _resolveInternalReadDeadlock();

                } else if( _isInternallyWriteDeadlocked() ) {

                    _notDone = _resolveInternalWriteDeadlock();

                } else if( _isExternallyReadDeadlocked() ) {

                    _notDone = _resolveExternalReadDeadlock();

                } else if( _isExternallyWriteDeadlocked() ) {

                    _notDone = _resolveExternalWriteDeadlock();

                } else {
                    throw new IllegalActionException("Actor is "
                            + "deadlocked but not of one of the "
                            + "four internal/external deadlock "
                            + "types.");
                }
            } else {
                // Processes Are Stopped; Continued Execution
                // Is Allowed 
                // FIXME: We shouldn't get here.
		_notDone = true;
	    }
        }
    }
     */

    /** Schedule an actor to be fired at the specified time.
     *  If the thread that calls this method is an instance
     *  of DDEThread, then the specified actor must be
     *  contained by this thread. If the thread that calls
     *  this method is not an instance of DDEThread, then
     *  store the actor and refire time in the initial time
     *  table of this director.
     *  <P>
     *  NOTE: The current implementation of this method is such
     *  that a more appropriate name might be <I>continueAt()</I>
     *  rather than <I>fireAt()</I>.
     * @param actor The actor scheduled to fire.
     * @param time The scheduled time to fire.
     * @exception IllegalActionException If the specified
     *  time is in the past or if the thread calling this
     *  method is a DDEThread but the specified actor is
     *  not contained by the DDEThread.
     */
    public void fireAt(Actor actor, double time)
            throws IllegalActionException {

        DDEThread ddeThread;
        Thread thread = Thread.currentThread();
        if( thread instanceof DDEThread ) {
            ddeThread = (DDEThread)thread;
        } else {
	    // Add the start time of actor to initialize table
	    if( _initialTimeTable == null ) {
		_initialTimeTable = new Hashtable();
	    }
	    _initialTimeTable.put( actor, new Double(time) );
            return;
        }

	if( _completionTime != -5.0 && time > _completionTime ) {
	    return;
	}

        Actor threadActor = ddeThread.getActor();
        if( threadActor != actor ) {
            throw new IllegalActionException("Actor argument of "
                    + "DDEDirector.fireAt() must be contained "
                    + "by the DDEThread that calls fireAt()");
        }

        TimeKeeper timeKeeper = ddeThread.getTimeKeeper();
        try {
            timeKeeper.setCurrentTime(time);
        } catch( IllegalArgumentException e ) {
	    throw new IllegalActionException(
		    ((NamedObj)actor).getName() + " - Attempt to "
		    + "set current time in the past.");
        }
    }

    /** Return true if one of the actors governed by this
     *  director has a pending mutation; return false
     *  otherwise.
     * @return True if a pending mutation exists; return
     *  false otherwise.
     */
    public boolean hasMutation() {
        return false;
    }

    /** Initialize this director and the actors it contains
     *  and set variables to their initial values. Create a
     *  DDEThread for each actor that this director controls
     *  but do not start the thread.
     * @exception IllegalActionException If there is an
     *  error during the creation of the threads or
     *  initialization of the actors.
     */
    public void initialize() throws IllegalActionException {
        super.initialize();
	_completionTime = -5.0;
        // _internalReadBlocks = 0;
        // _externalReadBlocks = 0;
        // _writeBlocks = 0;
        _writeBlockedQs = new LinkedList();
        _pendingMutations = false;
    }

    /** Return a new receiver of a type compatible with this
     *  director. If the completion time of this director has
     *  been explicitly set to a particular value then set the
     *  completion time of the receiver to this same value;
     *  otherwise set the completion time to
     *  PrioritizedTimedQueue.ETERNITY which indicates that the
     *  receivers should ignore the completion time.
     *  @return A new DDEReceiver.
     */
    public Receiver newReceiver() {
        DDEReceiver rcvr = new DDEReceiver();
	double time = _completionTime;
	try {
	    time = ((DoubleToken)stopTime.getToken()).doubleValue();
	} catch( IllegalActionException e ) {
	    throw new InternalErrorException( e.toString() );
	}
	rcvr._setCompletionTime( time );
        return rcvr;
    }

    /** Return true if the actors governed by this director can
     *  continue execution; return false otherwise. Continuation
     *  of execution is dependent upon whether the system is
     *  deadlocked in a manner that can not be resolved even if
     *  external communication occurs.
     * @return True if execution can continue; false otherwise.
     * @exception IllegalActionException Not thrown in this base class.
     *  May be thrown in derived classes.
     */
    public boolean postfire() throws IllegalActionException {
	Thread thread = Thread.currentThread();
	if( thread instanceof DDEThread ) {
	    TimeKeeper timeKeeper =
                ((DDEThread)thread).getTimeKeeper();
	    timeKeeper.removeAllIgnoreTokens();
	}
	return _notDone;
    }

    /** Return true if it
     *  transfer data from an input port of the container to the
     *  ports it is connected to on the inside. The port argument must
     *  be an opaque input port. If any channel of the input port
     *  has no data, then that channel is ignored.
     *  <P>
     *  NOTE: This method is preliminary and will likely change.
     *
     *  @exception IllegalActionException If the port is not an opaque
     *   input port.
     *  @param port The port to transfer tokens from.
     *  @return True if data are transferred.
    public boolean transferInputs(IOPort port) throws IllegalActionException {
        if (!port.isInput() || !port.isOpaque()) {
            throw new IllegalActionException(this, port,
                    "transferInputs: port argument is not an opaque" +
                    "input port.");
        }
        boolean trans = false;
        Token token = null;
        Receiver[][] insiderecs = port.deepGetReceivers();
        for (int i = 0; i < port.getWidth(); i++) {
            if (insiderecs != null && insiderecs[i] != null) {
            	for (int j = 0; j < insiderecs[i].length; j++ ) {
                    if( insiderecs[i][j].hasRoom() ) {
                        if( token == null ) {
                            if( port.hasToken(i) ) {
                                token = port.get(i);
                                insiderecs[i][j].put(token);
                            }
                        } else {
                            insiderecs[i][j].put(token);
                        }
                        trans = true;
                    }
                }
            }
        }
        return trans;
    }
     */

    /** Return true if it
     *  transfers data from an output port of the container to the
     *  ports it is connected to on the outside.  The port argument must
     *  be an opaque output port. If any channel of the output port
     *  has no data, then that channel is ignored.
     *  <P>
     *  NOTE: This method is preliminary and will likely change.
     *
     *  @exception IllegalActionException If the port is not an opaque
     *   output port.
     *  @param port The port to transfer tokens from.
     *  @return True if data are transferred.
    public boolean transferOutputs(IOPort port) throws IllegalActionException {
        if (!port.isOutput() || !port.isOpaque()) {
            throw new IllegalActionException(this, port,
                    "transferOutputs: port argument is not " +
                    "an opaque output port.");
        }
        boolean trans = false;
        Receiver[][] insiderecs = port.getInsideReceivers();
        if (insiderecs != null) {
            for (int i = 0; i < insiderecs.length; i++) {
                if (insiderecs[i] != null) {
                    for (int j = 0; j < insiderecs[i].length; j++) {
                        if (insiderecs[i][j].hasToken()) {
                            try {
                                if( port.hasRoom(i) ) {
                                    Token t = insiderecs[i][j].get();
                                    port.send(i, t);
                                }
                                trans = true;
                            } catch (NoTokenException ex) {
                                throw new InternalErrorException(
                                        "Director.transferOutputs: " +
                                        "Internal error: " +
                                        ex.getMessage());
                            }
                        }
                    }
                }
            }
        }
        return trans;
    }
     */

    ///////////////////////////////////////////////////////////////////
    ////                        protected methods                  ////

    /**
     */
    protected synchronized boolean _resolveInternalDeadlock() 
	 throws IllegalActionException {
	if( _writeBlockedQs.size() > 0 ) {
	    _incrementLowestCapacityPort();
	    return true;
	} 
	return false;
    }

    /** Implementations of this method must be synchronized.
     * @param internal True if internal read block.
     */
    protected synchronized void _actorUnBlocked(LinkedList rcvrs) {
        Iterator rcvrIterator = rcvrs.iterator();
        while( rcvrIterator.hasNext() ) {
            DDEReceiver rcvr = (DDEReceiver)rcvrIterator.next();
            _actorUnBlocked(rcvr);
        }
        notifyAll();
    }

    /** Increment the count of actors blocked on an internal read.
     */
    protected synchronized void _actorBlocked(DDEReceiver rcvr) {
	/*
        if( rcvr.isReadBlocked() ) {
            if( rcvr.isConnectedToBoundary() ) {
                _externalReadBlocks++;
            }
            _internalReadBlocks++;
        }
	*/
        if( rcvr.isWriteBlocked() ) {
	    if( _writeBlockedQs == null ) {
	        _writeBlockedQs = new LinkedList();
	    }
	    _writeBlockedQs.addFirst(rcvr);
            // _writeBlocks++;
        }
	super._actorBlocked(rcvr);
	notifyAll();

	/*
	if( _areActorsDeadlocked() ) {
	    notifyAll();
	}
	*/
    }

    /** Implementations of this method must be synchronized.
     * @param internal True if internal read block.
     */
    protected synchronized void _actorBlocked(LinkedList rcvrs) {
	Iterator rcvrIterator = rcvrs.iterator();
	while( rcvrIterator.hasNext() ) {
	   DDEReceiver rcvr = (DDEReceiver)rcvrIterator.next();
	   _actorBlocked(rcvr);
	}
	notifyAll();
    }

    /** Increment the count of actors blocked on an internal read.
     */
    protected synchronized void _actorUnBlocked(DDEReceiver rcvr) {
	/*
        if( rcvr.isReadBlocked() ) {
            if( rcvr.isConnectedToBoundary() ) {
                _externalReadBlocks--;
            }
            _internalReadBlocks--;
        }
	*/
        if( rcvr.isWriteBlocked() ) {
	    if( _writeBlockedQs == null ) {
                // FIXME: throw exception???
	    }
            _writeBlockedQs.remove(rcvr);
	    /*
            if( _writeBlocks > 0 ) {
                _writeBlocks--;
            }
	    */
        }
	super._actorUnBlocked(rcvr);
	notifyAll();
    }


    ///////////////////////////////////////////////////////////////////
    ////                  package friendly methods                 ////

    /** Return the initial time table of this director.
     * @return The initial time table of this actor.
     */
    Hashtable _getInitialTimeTable() {
	if( _initialTimeTable == null ) {
	    _initialTimeTable = new Hashtable();
	}
	return _initialTimeTable;
    }

    ///////////////////////////////////////////////////////////////////
    ////                         protected methods                 ////

    /** Return a new ProcessThread of a type compatible with this
     *  director.
     * @param actor The actor that the new ProcessThread will
     *  control.
     * @param director The director that manages the new
     *  ProcessThread.
     * @exception IllegalActionException If an error occurs while
     *  instantiating the new ProcessThread.
     */
    protected ProcessThread _getProcessThread(Actor actor,
	    ProcessDirector director) throws IllegalActionException {
	return new DDEThread(actor, director);
    }

    /** Increment the port capacity's according to Tom Parks'
     *  algorithm. Select the port with the smallest capacity
     *  and double the capacity.
     * @exception IllegalActionException If there is an error
     *  while attempting to set the capacity of a DDE receiver.
     */
    protected void _incrementLowestCapacityPort()
            throws IllegalActionException {
	if( _writeBlockedQs == null ) {
            return;
	}
        Collections.sort( _writeBlockedQs,
                new RcvrCapacityComparator() );
        DDEReceiver smallestQueue;
        smallestQueue = (DDEReceiver)_writeBlockedQs.getFirst();

        if( smallestQueue.getCapacity() <= 0 ) {
            smallestQueue.setCapacity(1);
        } else {
            int cap = smallestQueue.getCapacity();
            smallestQueue.setCapacity(cap * 2);
        }
        _actorUnBlocked( smallestQueue );
        synchronized( smallestQueue ) {
            smallestQueue.notifyAll();
        }
    }

    /** Mutate the model that this director controls.
     */
    protected void _performMutations() {
        ;
    }

    ///////////////////////////////////////////////////////////////////
    ////                         private methods                   ////

    ///////////////////////////////////////////////////////////////////
    ////                         private variables                 ////

    private double _completionTime = PrioritizedTimedQueue.ETERNITY;
    // private int _internalReadBlocks = 0;
    // private int _externalReadBlocks = 0;
    // private int _writeBlocks = 0;
    // private LinkedList _extReadBlockedQs;
    private boolean _pendingMutations = false;
    private LinkedList _writeBlockedQs;
    private Hashtable _initialTimeTable;

    // private int _readBlocks = 0;
    // private int _writeBlocks = 0;


    ///////////////////////////////////////////////////////////////////
    ////                         inner class			   ////

    private class RcvrCapacityComparator implements Comparator {

        /**
         * @exception ClassCastException If fst and scd are
         *  not instances of DDEReceiver.
         */
        public int compare(Object fst, Object scd) {
            DDEReceiver first = null;
            DDEReceiver second = null;

            if( fst instanceof DDEReceiver ) {
                first = (DDEReceiver)fst;
            }
            if( scd instanceof DDEReceiver ) {
                second = (DDEReceiver)scd;
            }

            if( first.getCapacity() < second.getCapacity() ) {
                return 1;
            } else if( first.getCapacity() > second.getCapacity() ) {
                return -1;
            } else {
                return 0;
            }
        }
    }
}

    /** Increment the count of actors blocked on an internal read.
    protected synchronized void _actorReadBlocked(DDEReceiver rcvr) {
        boolean internal = true;
        if( rcvr.isConnectedToBoundary() ) {
            internal = false;
        }
        if( internal ) {
            _internalReadBlocks++;
        } else {
            _externalReadBlocks++;
        }
	if( _areActorsDeadlocked() ) {
	    notifyAll();
	}
    }
     */

    /** Decrement the count of actors externally blocked on a read.
    protected synchronized void _actorReadUnBlocked(DDEReceiver rcvr) {
        boolean internal = true;
        if( rcvr.isConnectedToBoundary() ) {
            internal = false;
        }
        if( internal ) {
            if( _internalReadBlocks > 0 ) {
                _internalReadBlocks--;
            }
        } else {
            if( _externalReadBlocks > 0 ) {
            	_externalReadBlocks--;
            }
        }
    }
     */

    /** Increment the count of actors blocked on a write.
     * @param rcvr The DDEReceiver that has a write block.
    protected synchronized void _actorWriteBlocked(DDEReceiver rcvr) {
	if( _writeBlockedQs == null ) {
	    _writeBlockedQs = new LinkedList();
	}
	_writeBlockedQs.addFirst(rcvr);
        _writeBlocks++;
        if( _areActorsDeadlocked() ) {
            notifyAll();
        }
    }
     */

    /** Increment the count of actors blocked on a write.
    protected synchronized void _actorWriteBlocked(DDEReceiver rcvr) {
        _writeBlocks++;
        if( _areActorsDeadlocked() ) {
            notifyAll();
        }
    }
     */

    /** Decrement the count of actors blocked on a write.
     *  @param rcvr The DDEReceiver that is no longer
     *   write blocked.
    protected synchronized void _actorWriteUnBlocked(DDEReceiver rcvr) {
	if( _writeBlockedQs == null ) {
	    _writeBlockedQs = new LinkedList();
	}
	_writeBlockedQs.remove(rcvr);
        if( _writeBlocks > 0 ) {
            _writeBlocks--;
        }
    }
     */

    /** Decrement the count of actors blocked on a write.
     *  @param rcvr The DDEReceiver that is no longer
     *   write blocked.
    protected synchronized void _actorWriteUnBlocked(DDEReceiver) {
        if( _writeBlocks > 0 ) {
            _writeBlocks--;
        }
    }
     */
