/*

 Copyright (c) 1997-2007 The Regents of the University of California.
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

 */

package ptolemy.actor.gt;

import java.util.Collection;

import ptolemy.data.expr.Parameter;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.InternalErrorException;
import ptolemy.kernel.util.KernelException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.Settable;
import ptolemy.kernel.util.ValueListener;
import ptolemy.kernel.util.Workspace;

/**

 @author Thomas Huining Feng
 @version $Id$
 @since Ptolemy II 6.1
 @Pt.ProposedRating Red (tfeng)
 @Pt.AcceptedRating Red (tfeng)
 */
public abstract class ParameterAttribute extends TransformationAttribute
implements Settable {

    public ParameterAttribute(NamedObj container, String name)
    throws NameDuplicationException, IllegalActionException {
        super(container, name);

        _initParameter();
    }

    public ParameterAttribute(Workspace workspace) {
        super(workspace);

        try {
            _initParameter();
        } catch (KernelException e) {
            throw new InternalErrorException(this, e,
                    "Unable to initialize parameters.");
        }
    }

    public void addValueListener(ValueListener listener) {
        parameter.addValueListener(listener);
    }

    public String getDefaultExpression() {
        return parameter.getDefaultExpression();
    }

    public String getExpression() {
        return parameter.getExpression();
    }

    public String getValueAsString() {
        return parameter.getValueAsString();
    }

    public Visibility getVisibility() {
        return parameter.getVisibility();
    }

    public void removeValueListener(ValueListener listener) {
        parameter.removeValueListener(listener);
    }

    public void setExpression(String expression) throws IllegalActionException {
        parameter.setExpression(expression);
    }

    public void setVisibility(Visibility visibility) {
        parameter.setVisibility(visibility);
    }

    public Collection<?> validate() throws IllegalActionException {
        return parameter.validate();
    }

    public Parameter parameter;

    protected abstract void _initParameter() throws IllegalActionException,
    NameDuplicationException;

}
