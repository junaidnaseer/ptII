/* The rate limiter in the CT domain.

 Copyright (c) 1998-2001 The Regents of the University of California.
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

@ProposedRating Red (liuj@eecs.berkeley.edu)
@AcceptedRating Red (cxh@eecs.berkeley.edu)
*/

package ptolemy.domains.ct.lib;
import ptolemy.domains.ct.kernel.*;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.*;
import ptolemy.actor.lib.Transformer;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.expr.Parameter;
import ptolemy.data.DoubleToken;
import ptolemy.data.Token;
import ptolemy.data.type.BaseType;


//////////////////////////////////////////////////////////////////////////
//// CTRateLimiter
/**
This actor limits the first derivative of the input signal (u).
The output (y) changes no faster than the specified limit. The derivative
is calculated using this equation:
<pre>
        u(k) - y(k-1)
rate = --------------
        t(k) - t(k-1)
</pre>
where u(k) and t(k) are the current input and time, and y(k-1) and t(k-1)
are the output and time at the previous step. The output is determined 
by comparing rate to the <i>risingSlewRate</i> and 
<i>fallingSlewRate</i> parameters.
<ul>
<li>If rate is greater than <i>risingSlewRate</i>, the output is 
<pre>
y(k) = (t(k)-t(k-1))*risingSlewRate + y(k-1) 
</pre> 
<li>If rate is less than <i>fallingSlewRate</i>, the output is 
<pre>
y(k) = (t(k)-t(k-1))*fallingSlewRate + y(k-1) 
<li>Otherwise, just output the input. 
<P>
This actor works as a (continuous) nonlinear function. It does not
control integration step sizes. Notice that this actor does not try
to find the time instant that the input rate reaches the rising slew rate
or falling slew rate. As a consequence, this may cause some ODE solvers
not converging under some circumstances. 

FIXME: This actor should implement the CTStateful interface to support
rollback.

@author Jie Liu
@version $Id$

*/
public class CTRateLimiter extends Transformer {

    /** Construct an actor with the given container and name.
     *  @param container The container.
     *  @param name The name of this actor.
     *  @exception IllegalActionException If the actor cannot be contained
     *   by the proposed container.
     *  @exception NameDuplicationException If the container already has an
     *   actor with this name.
     */
    public CTRateLimiter(CompositeEntity container, String name)
            throws NameDuplicationException, IllegalActionException {
        super(container, name);
        input.setTypeEquals(BaseType.DOUBLE);
        output.setTypeEquals(BaseType.DOUBLE);

        risingSlewRate = new Parameter(this, "risingSlewRate", 
                new DoubleToken(1.0));
        fallingSlewRate = new Parameter(this, "fallingSlewRate", 
                new DoubleToken(-1.0));
        
    }
    
    ///////////////////////////////////////////////////////////////////
    ////                     ports and parameters                  ////
    /** The rising slew rate.
     *  The parameter has type doubele and default value 1.0.
     */
    public Parameter risingSlewRate;

    /** The rising slew rate.
     *  The parameter has type doubele and default value -1.0.
     */
    public Parameter fallingSlewRate;

    ///////////////////////////////////////////////////////////////////
    ////                        public methods                     ////
    
    /** Compute the change rate of the input and compare it to
     *  the <i>risingSlewRate</i> and the <i>fallingSlewRate</i>.
     *  If the rate is outside the range defined by the parameters,
     *  it will output the bounded signal.
     *  If there is no input, then produce no output.
     *  @exception IllegalActionException If there is no director.
     */
    public void fire() throws IllegalActionException {
        if (input.hasToken(0)) {
            _newToken = input.get(0);
            double currentTime = getDirector().getCurrentTime();
            if(currentTime == _lastTime) {
                output.send(0, _newToken);
            } else {
                double valueDifference = ((DoubleToken)_newToken.subtract(
                        _lastToken)).doubleValue();
                double timeDifference = currentTime - _lastTime;
                double rate = valueDifference / timeDifference;
                double risingRate = ((DoubleToken)risingSlewRate.getToken())
                    .doubleValue();
                double fallingRate = ((DoubleToken)fallingSlewRate.getToken())
                    .doubleValue();
                if (rate > risingRate) {
                    _newToken = (new DoubleToken(timeDifference*
                            risingRate)).add(_lastToken);
                } else if(rate < fallingRate) {
                    _newToken = (new DoubleToken(timeDifference*
                             fallingRate)).add(_lastToken);
                }
                output.send(0, _newToken);
            }
        }
    }

    /** Update the time and value for this iteration. Return the same
     *  value as super.postfire().
     *  @exception IllegalActionException If the director throws it when
     *   scheduling the next firing.
     */
    public boolean postfire() throws IllegalActionException {
        _lastTime = getDirector().getCurrentTime();
        _lastToken = _newToken;
        return super.postfire();
    }
    
    ///////////////////////////////////////////////////////////////////
    ////                     private variables                     ////
    // Last time instant.
    private double _lastTime;

    // Last output value.
    private Token _lastToken;
     
    // New value.
    private Token _newToken;
}
