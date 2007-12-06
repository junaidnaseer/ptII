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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import ptolemy.data.expr.ScopeExtendingAttribute;
import ptolemy.data.expr.Variable;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.InternalErrorException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;

/**

 @author Thomas Huining Feng
 @version $Id$
 @since Ptolemy II 6.1
 @Pt.ProposedRating Red (tfeng)
 @Pt.AcceptedRating Red (tfeng)
 */
public class ActorScopeExtender extends ScopeExtendingAttribute {

    /**
     * @param container
     * @param name
     * @exception NameDuplicationException
     * @exception IllegalActionException
     * @exception IllegalActionException
     * @exception NameDuplicationException
     */
    public ActorScopeExtender(NamedObj container, String name)
            throws IllegalActionException, NameDuplicationException {
        super(container, name);
    }

    public List<?> attributeList() {
        long version = workspace().getVersion();
        if (_attributeList == null || version > _version) {
            _attributeList =
                new LinkedList<Object>((List<?>) super.attributeList());
            NamedObj scope = getContainer();
            Collection<?> children =
                GTTools.getChildren(scope, true, true, true, true);
            for (Object childObject : children) {
                NamedObj child = (NamedObj) childObject;
                if (child instanceof ActorScopeExtender) {
                     continue;
                }

                try {
                    Variable variable =
                        NamedObjVariable.getNamedObjVariable(child, true);
                    if (variable != null) {
                        _attributeList.add(variable);
                    }
                } catch (IllegalActionException e) {
                    throw new InternalErrorException(e);
                }
            }
        }
        return Collections.unmodifiableList(_attributeList);
    }

    public Attribute getAttribute(String name) {
        NamedObj scope = getContainer();
        NamedObj child = GTTools.getChild(scope, name, true, true, true, true);
        if (child instanceof ActorScopeExtender) {
            child = GTTools.getChild(scope, name, false, true, true, true);
        }
        if (child == null) {
            return super.getAttribute(name);
        }

        try {
            Variable actorVariable =
                NamedObjVariable.getNamedObjVariable(child, true);
            return actorVariable;
        } catch (IllegalActionException e) {
            throw new InternalErrorException(e);
        }
    }

    private List<Object> _attributeList;

    private long _version = -1;

}
