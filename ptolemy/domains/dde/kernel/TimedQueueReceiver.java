/* A FIFO queue-based receiver for storing tokens with time stamps.

 Copyright (c) 1997-1999 The Regents of the University of California.
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

@ProposedRating Yellow (davisj@eecs.berkeley.edu)
@AcceptedRating Yellow (yuhong@eecs.berkeley.edu)

*/

package ptolemy.domains.dde.kernel;

import ptolemy.kernel.util.*;
import ptolemy.data.*;
import ptolemy.actor.*;
import ptolemy.actor.util.*;

//////////////////////////////////////////////////////////////////////////
//// TimedQueueReceiver
/**
A FIFO queue-based receiver for storing tokens with time stamps. A "time
stamp" is a time value that is associated with a token and is used to order
the consumption of a token with respect to other time stamped tokens. To
help organize the tokens contained by this queue, two flags are maintained:
<I>last time</I> and <I>receiver time</I>. The last time flag is defined to 
be equivalent to the time stamp of the token that was most recently placed 
in the queue. The receiver time flag is defined as the time stamp of the 
oldest token in the queue or the last token to be removed from the queue if 
the queue is empty. Both of these flags must have monotonically, 
non-decreasing values. At the conclusion of a simulation run the receiver 
time is set to INACTIVE.
<P>
A TimeKeeper object is assigned to each actor that operates according to
an DDE model of computation. The TimeKeeper manages each of the receivers
that are contained by an actor by keeping track of the receiver times of
each receiver. As information flows through a TimedQueueReceiver, the 
TimeKeeper must be kept up to date with respect to the receiver times. 


@author John S. Davis II
@version $Id$
@see ptolemy.domains.dde.kernel.DDEReceiver
@see ptolemy.domains.dde.kernel.TimeKeeper
*/

public class TimedQueueReceiver {

    /** Construct an empty queue with no container.
     */
    public TimedQueueReceiver() {
        super();
    }

    /** Construct an empty queue with the specified IOPort container.
     * @param container The IOPort that contains this receiver.
     */
    public TimedQueueReceiver(IOPort container) {
        super();
	_container = container;
    }

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** Take the the oldest token off of the queue and return it.
     *  If the queue is empty, throw a NoTokenException. If there are
     *  other tokens left on the queue after this removal, then set
     *  the receiver time of this receiver to equal that of the next 
     *  oldest token. Update the TimeKeeper that manages this 
     *  TimedQueueReceiver.
     * @exception NoTokenException If the queue is empty.
     */
    public Token get() {
	Thread thread = Thread.currentThread();
	Token token = null;
	synchronized( this ) {
            Event event = (Event)_queue.take();
	    if (event == null) {
                throw new NoTokenException(getContainer(),
	                "Attempt to get token from an empty "
                        + "TimedQueueReceiver.");
            }
	    token = event.getToken();
	    if( thread instanceof DDEThread ) {
	        TimeKeeper timeKeeper = ((DDEThread)thread).getTimeKeeper();
	        timeKeeper.setCurrentTime( event.getTime() );
	    }

	    if( _queue.size() > 0 ) {
	        Event nextEvent = (Event)_queue.get(0);
	        _rcvrTime = nextEvent.getTime();
            }

	    // Call updateRcvrList() even if _queue.size()==0, so that
	    // the triple is no longer in front.
	    if( thread instanceof DDEThread ) {
		TimeKeeper timeKeeper = ((DDEThread)thread).getTimeKeeper();
	        timeKeeper.updateRcvrList( this, _rcvrTime, _priority );
	    }
	}
        return token;
    }

    /** Get the queue capacity of this receiver.
     * @return The capacity of this receiver's queue.
     */
    public int getCapacity() {
        return _queue.getCapacity();
    }

    /** Return the IOPort container of this receiver.
     * @return IOPort The containing IOPort.
     */
    public IOPort getContainer() {
        return _container;
    }

    /** Return the last time of this receiver. The last time is
     *  the time associated with the token that was most recently
     *  placed in the queue. If no tokens have been placed on the
     *  queue, then the last time equals 0.0.
     * @return double The last time of this TimedQueueReceiver.
     */
    public double getLastTime() {
        return _lastTime;
    }

    /** Get the priority of this receiver.
     * @return Return the priority of this receiver.
     */
    public int getPriority() {
        return _priority;
    }

    /** Return the receiver time of this receiver. The receiver
     *  time is the time stamp associated with the oldest token that 
     *  is currently on the queue. If the queue is empty, then the 
     *  receiver time value is equal to the "last time."
     * @return double The receiver time of this TimedQueueReceiver.
     */
    public double getRcvrTime() {
        return _rcvrTime;
    }

    /** Return true if the number of tokens stored in the queue is
     *  less than the capacity of the queue. Return false otherwise.
     * @return boolean Return true if the queue is not full; return
     *  false otherwise.
     */
    public boolean hasRoom() {
        return !_queue.isFull();
    }

    /** Return true if there are tokens stored on the queue. Return
     *  false if the queue is empty.
     * @return boolean Return true if the queue is not empty; return
     *  false otherwise.
     */
    public boolean hasToken() {
        return _queue.size() > 0;
    }

    /** Put a token on the queue with the specified time stamp and set
     *  the last time value to be equal to this time stamp. If the 
     *  queue is empty immediately prior to putting the token on
     *  the queue, then set the receiver time value to be equal to the
     *  last time value. If the queue is full, throw a NoRoomException.
     * @param token The token to put on the queue.
     * @param time The time stamp of the token.
     * @exception NoRoomException If the queue is full.
     */
    public void put(Token token, double time) throws NoRoomException {
	if( time < _lastTime && time != INACTIVE ) {
	    IOPort port = (IOPort)getContainer(); 
	    NamedObj actor = (NamedObj)port.getContainer(); 
	    throw new IllegalArgumentException(actor.getName() + 
		    " - Attempt to set current time in the past.");
	}
        Event event;
        synchronized(this) {
            _lastTime = time;
            event = new Event(token, _lastTime);

            if( _queue.size() == 0 ) {
                _rcvrTime = _lastTime;
            }

            if (!_queue.put(event)) {
                throw new NoRoomException (getContainer(),
                        "Queue is at capacity. Cannot insert token.");
            }
        }
    }

    /** Set the queue capacity of this receiver.
     * @param capacity The capacity of this receiver's queue.
     */
    public void setCapacity(int capacity) throws IllegalActionException {
        _queue.setCapacity(capacity);
    }

    /** Set the IOPort container.
     * @param port The IOPort which contains this receiver.
     */
    public void setContainer(IOPort port) {
        _container = port;
    }

    ///////////////////////////////////////////////////////////////////
    ////                   package friendly methods                ////

    /** Return the completion time of this receiver.
     * @return double The completion time.
     */
    synchronized double getCompletionTime() {
        return _completionTime;
    }

    /** Set the completion time of this receiver.
     * @param time The completion time of this receiver.
     */
    void setCompletionTime(double time) {
        _completionTime = time;
    }

    /** Set the priority of this receiver.
     * @param int The priority of this receiver.
     */
    synchronized void setPriority(int priority) {
        _priority = priority;
    }

    ///////////////////////////////////////////////////////////////////
    ////                         private variables                 ////

    // This time value indicates that the receiver is no longer active.
    protected static double INACTIVE = -1.0;

    // This time value indicates that the receiver is no longer active.
    protected static double NOTSTARTED = -5.0;

    // The time stamp of the newest token to be placed in the queue.
    private double _lastTime = 0.0;

    // The time stamp of the earliest token that is still in the queue.
    private double _rcvrTime = 0.0;

    // The time after which execution of this receiver will cease.
    private double _completionTime = NOTSTARTED;

    // The priority of this receiver.
    private int _priority = 0;

    // The queue in which this receiver stores tokens.
    private FIFOQueue _queue = new FIFOQueue();

    // The IOPort which contains this receiver.
    private IOPort _container;

    // The thread controlling the actor that contains this receiver.
    private DDEThread _controllingThread;

    // The time keeper for the actor that receives through this receiver.
    private TimeKeeper _rcvingTimeKeeper;

    // The time keeper for the actor that sends through this receiver.
    private TimeKeeper _sendingTimeKeeper;

    ///////////////////////////////////////////////////////////////////
    ////                         inner class                       ////

    // An Event is an aggregation consisting of a Token, a
    // time stamp and destination Receiver. Both the token
    // and destination receiver are allowed to have null
    // values. This is particularly useful in situations
    // where the specification of the destination receiver
    // may be considered redundant.

    private class Event {

	// Construct an Event with a token and time stamp.
	public Event(Token token, double time) {
	    _token = token;
	    _timeStamp = time;
	}

	// Construct an Event with a token, a time stamp and a
	// destination receiver.
	public Event(Token token, double time, Receiver receiver) {
	    this(token, time);
	    _receiver = receiver;
	}


        ///////////////////////////////////////////////////////////
	////                     public methods                ////

	// Return the destination receiver of this event.
	public Receiver getReceiver() {
	    return _receiver;
	}

	// Return the time stamp of this event.
	public double getTime() {
	    return _timeStamp;
	}

	// Return the token of this event.
	public Token getToken() {
	    return _token;
	}

        ///////////////////////////////////////////////////////////
        ////                     private variables             ////

	double _timeStamp = 0.0;
	Token _token = null;
	Receiver _receiver = null;
    }

}
