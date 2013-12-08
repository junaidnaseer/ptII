/* A visitor for parse trees of the expression language.

 Copyright (c) 2006-2013 The Regents of the University of California
 All rights reserved.
 Permission is hereby granted, without written agreement and without
 license or royalty fees, to use, copy, modify, and distribute this
 software and its documentation for any purpose, provided that the above
 copyright notice and the following two paragraphs appear in all copies
 of this software.

 IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA OR RESEARCH IN MOTION
 LIMITED BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL,
 INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OF THIS
 SOFTWARE AND ITS DOCUMENTATION, EVEN IF THE UNIVERSITY OF CALIFORNIA
 OR RESEARCH IN MOTION LIMITED HAVE BEEN ADVISED OF THE POSSIBILITY OF
 SUCH DAMAGE.

 THE UNIVERSITY OF CALIFORNIA AND RESEARCH IN MOTION LIMITED
 SPECIFICALLY DISCLAIM ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 PARTICULAR PURPOSE. THE SOFTWARE PROVIDED HEREUNDER IS ON AN "AS IS"
 BASIS, AND THE UNIVERSITY OF CALIFORNIA AND RESEARCH IN MOTION
 LIMITED HAVE NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 ENHANCEMENTS, OR MODIFICATIONS.


 */
package ptolemy.cg.kernel.generic.program.procedural.java;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import ptolemy.cg.kernel.generic.ParseTreeCodeGenerator;
import ptolemy.cg.kernel.generic.program.procedural.ProceduralParseTreeCodeGenerator;
import ptolemy.data.ArrayToken;
import ptolemy.data.BitwiseOperationToken;
import ptolemy.data.BooleanToken;
import ptolemy.data.ComplexToken;
import ptolemy.data.FunctionToken;
import ptolemy.data.LongToken;
import ptolemy.data.MatrixToken;
import ptolemy.data.OrderedRecordToken;
import ptolemy.data.RecordToken;
import ptolemy.data.ScalarToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.ASTPtArrayConstructNode;
import ptolemy.data.expr.ASTPtBitwiseNode;
import ptolemy.data.expr.ASTPtFunctionApplicationNode;
import ptolemy.data.expr.ASTPtFunctionDefinitionNode;
import ptolemy.data.expr.ASTPtFunctionalIfNode;
import ptolemy.data.expr.ASTPtLeafNode;
import ptolemy.data.expr.ASTPtLogicalNode;
import ptolemy.data.expr.ASTPtMatrixConstructNode;
import ptolemy.data.expr.ASTPtMethodCallNode;
import ptolemy.data.expr.ASTPtOrderedRecordConstructNode;
import ptolemy.data.expr.ASTPtPowerNode;
import ptolemy.data.expr.ASTPtProductNode;
import ptolemy.data.expr.ASTPtRecordConstructNode;
import ptolemy.data.expr.ASTPtRelationalNode;
import ptolemy.data.expr.ASTPtRootNode;
import ptolemy.data.expr.ASTPtShiftNode;
import ptolemy.data.expr.ASTPtSumNode;
import ptolemy.data.expr.ASTPtUnaryNode;
import ptolemy.data.expr.AbstractParseTreeVisitor;
import ptolemy.data.expr.Constants;
import ptolemy.data.expr.ExpressionFunction;
import ptolemy.data.expr.ParseTreeSpecializer;
import ptolemy.data.expr.ParseTreeTypeInference;
import ptolemy.data.expr.ParserScope;
import ptolemy.data.expr.PtParserConstants;
import ptolemy.data.expr.Token;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.FunctionType;
import ptolemy.data.type.MatrixType;
import ptolemy.data.type.Type;
import ptolemy.data.type.TypeLattice;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.InternalErrorException;
import ptolemy.kernel.util.KernelException;
import ptolemy.math.Complex;
import ptolemy.util.StringUtilities;

///////////////////////////////////////////////////////////////////
//// JavaParseTreeCodeGenerator

/**
 Evaluate a parse tree given a reference to its root node and generate Java code.
 It implements a visitor that visits the parse tree in depth-first order,
 evaluating each node and storing the result as a token in the node.
 Two exceptions are logic nodes and the ternary if node (the ? : construct),
 which do not necessarily evaluate all children nodes.

 <p>This class has the following limitations:
 <ul>
 <li> It is a copy of ParseTreeEvaluator from data/expr and thus
 has lots of code for evaluating expressions, which we don't need
 <li> It is not properly converting types: We need to add logic to
 convert types.
 <li> The .tcl test has known failures involving nulls.
 <li> It does not evaluate constants.
 </ul>

 @author Man-Kit Leung
 @version $Id$
 @since Ptolemy II 10.0
 @Pt.ProposedRating Red
 @Pt.AcceptedRating Red
 @see ptolemy.data.expr.ASTPtRootNode
 */
public class JavaParseTreeCodeGenerator extends ProceduralParseTreeCodeGenerator {

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** Construct an ArrayToken that contains the tokens from the
     *  children of the specified node.
     *  @param node The specified node.
     *  @exception IllegalActionException If an parse error occurs.
     */
    public void visitArrayConstructNode(ASTPtArrayConstructNode node)
            throws IllegalActionException {
        if (node.isConstant() && node.isEvaluated()) {
            _evaluatedChildToken = node.getToken();
            //_fireCode.append(_evaluatedChildToken.toString());
            _childCode = _evaluatedChildToken.toString();
            return;
        }

        int numChildren = node.jjtGetNumChildren();

        ptolemy.data.Token[] tokens = new ptolemy.data.Token[numChildren];

        //ptolemy.data.Token[] tokens = _evaluateAllChildren(node);
        //_fireCode.append("$new(Array(" + numChildren + ", " + numChildren);
        //String[] elements = new String[numChildren];
        StringBuffer result = new StringBuffer("$new(Array(" + numChildren
                + ", " + numChildren);

        // Convert up to LUB.
        // Assume UNKNOWN is the lower type.
        ptolemy.data.type.Type elementType = BaseType.UNKNOWN;

        for (int i = 0; i < numChildren; i++) {
            //_fireCode.append(", ");
            result.append(", ");

            //int nextIndex = _fireCode.length();

            if (_typeInference == null) {
                _typeInference = new ParseTreeTypeInference();
            }
            _typeInference.inferTypes(node, _scope);

            tokens[i] = _evaluateChild(node, i);

            Type valueType = ((ASTPtRootNode) node.jjtGetChild(i)).getType();

            if (_isPrimitive(valueType)) {
                //_fireCode.insert(nextIndex, "$new(" + _codeGenType(valueType) + "(");
                //_fireCode.append("))");
                result.append("$new(" + _codeGenType(valueType) + "("
                        + _childCode + "))");
            } else {
                result.append(_childCode);
            }

            if (!elementType.equals(valueType)) { // find max type
                elementType = TypeLattice.leastUpperBound(elementType,
                        valueType);
            }
        }

        //for (int i = 0; i < numChildren; i++) {
        //tokens[i] = elementType.convert(tokens[i]);
        //}

        // Insert the elementType of the array as the last argument.
        if (_targetType(elementType).equals("Token")) {
            //_fireCode.append(", -1");
            result.append(", TYPE_Token");
        } else {
            //_fireCode.append(", TYPE_" + _codeGenType(elementType));
            result.append(", TYPE_" + _codeGenType(elementType));
        }

        //_fireCode.append("))");
        _childCode = result.toString() + "))";

        // Tests JavaParseTreeCodeGenerator-16.2 and
        // JavaParseTreeCodeGenerator-17.2 require that
        // _evaluatedChildToken be set here, otherwise
        // _evaluatedChildToken will be set to the value
        // of the last token.
        _evaluatedChildToken = new ArrayToken(elementType, tokens);

        //if (node.isConstant()) {
        //    node.setToken(_evaluatedChildToken);
        //}

    }

    /** Define a function, where the children specify the argument types
     *  and the expression.  The expression is not evaluated. The resulting
     *  token in the node is an instance of FunctionToken.
     *  @param node The specified node.
     *  @exception IllegalActionException If an parse error occurs.
     */
    public void visitFunctionDefinitionNode(ASTPtFunctionDefinitionNode node)
            throws IllegalActionException {
        ASTPtRootNode cloneTree;

        ParseTreeSpecializer specializer = new ParseTreeSpecializer();
        cloneTree = specializer.specialize(node.getExpressionTree(),
                node.getArgumentNameList(), _scope);

        // Infer the return type.
        if (_typeInference == null) {
            _typeInference = new ParseTreeTypeInference();
        }

        _typeInference.inferTypes(node, _scope);

        FunctionType type = (FunctionType) node.getType();
        ExpressionFunction definedFunction = new ExpressionFunction(
                node.getArgumentNameList(), node.getArgumentTypes(), cloneTree);
        FunctionToken result = new FunctionToken(definedFunction, type);
        _evaluatedChildToken = result;

        StringBuffer functionCode = new StringBuffer(
                "\n/* FIXME: This code does work.*/\n");

        // Handle functions like iterate()
        // FIXME: Needs to be finished.  iterate() takes a Ptolemy Token
        // as a third argument.
        // $PTII/bin/ptcg -language java /Users/cxh/ptII/ptolemy/codegen/c/actor/lib/test/auto/knownFailedTests/ExpressionIterate.xml

        // FIXME: Generate function declaration in _preinitCode.
        //functionCode += type.getReturnType().toString();

        List argumentNames = node.getArgumentNameList();
        Type[] argumentTypes = node.getArgumentTypes();

        // FIXME: If we are going to use ptolemy classes, we need
        // to add $PTII to the classpath some how.
        functionCode.append(" new ptolemy.data.expr.ExpressionFunction(\n"
                + "java.util.Arrays.asList(new String[]\n{\n");
        for (int i = 0; i < argumentNames.size(); i++) {
            functionCode.append("\"" + argumentNames.get(i) + "\"");
            if (i < argumentNames.size() - 1) {
                functionCode.append(", ");
            }
        }
        functionCode.append("\n}\n),\n new ptolemy.data.type.Type[] \n{\n");
        for (int i = 0; i < argumentTypes.length; i++) {
            functionCode.append("ptolemy.data.type.BaseType."
                    + argumentTypes[i].toString().toUpperCase(
                            Locale.getDefault()));
            if (i < argumentTypes.length - 1) {
                functionCode.append(", ");
            }
        }
        functionCode.append("\n}\n,\n");

        // FIXME: The problem here is that we need a way to create
        // a ASTPtRootNode at run time that contains the functionality
        // required.  One idea would be to create an anonymous class
        // that extended ExpressionFunction and had an apply() method
        // that had a body that consisted of the functionality we want.
        functionCode.append(null + "\n/*" + node.getExpressionTree() + "*/\n)");

        //functionCode += ") {\n";
        //functionCode += "    return ";
        // See ExpressionFunction.apply() for how to create a temporary scope.
        //functionCode += evaluateParseTree(node.getExpressionTree(), _scope);
        //functionCode += ";\n}\n";

        _childCode = functionCode.toString();
        return;
    }

    /** Evaluate the first child, and depending on its (boolean) result,
     *  evaluate either the second or the third child. The result of
     *  that evaluation becomes the result of the specified node.
     *  @param node The specified node.
     *  @exception IllegalActionException If an parse error occurs.
     */
    public void visitFunctionalIfNode(ASTPtFunctionalIfNode node)
            throws IllegalActionException {
        if (node.isConstant() && node.isEvaluated()) {
            _evaluatedChildToken = node.getToken();
            //_fireCode.append(_evaluatedChildToken.toString());
            _childCode = _evaluatedChildToken.toString();

            return;
        }

        int numChildren = node.jjtGetNumChildren();

        if (numChildren != 3) {
            // A functional-if node MUST have three children in the parse
            // tree.
            throw new InternalErrorException(
                    "PtParser error: a functional-if node does not have "
                            + "three children in the parse tree.");
        }

        StringBuffer result = new StringBuffer();

        // evaluate the first sub-expression
        _evaluateChild(node, 0);
        result.append(_childCode);

        /*
         ptolemy.data.Token test = _evaluatedChildToken;

         if (!(test instanceof BooleanToken)) {
         throw new IllegalActionException(
         "Functional-if must branch on a boolean, but instead test "
         + (test == null ? "was null " : "was "
         + test.toString() + "an instance of "
         + test.getClass().getName()));
         }

         boolean value = ((BooleanToken) test).booleanValue();

         // Choose the correct sub-expression to evaluate,
         // and type check the other.
         if (_typeInference == null) {
         _typeInference = new ParseTreeTypeInference();
         }


         if (value) {
         */
        //_fireCode.append(" ? ");
        result.append(" ? ");

        ASTPtRootNode tokenChild1 = (ASTPtRootNode) node.jjtGetChild(1);
        ASTPtRootNode tokenChild2 = (ASTPtRootNode) node.jjtGetChild(2);

        tokenChild1.visit(this);
        result.append(_childCode);

        //_fireCode.append(" : ");
        result.append(" : ");

        tokenChild2.visit(this);
        result.append(_childCode);

        _childCode = "(" + result + ")";

        ptolemy.data.Token token1 = _evaluatedChildToken;
        ptolemy.data.Token token2 = _evaluatedChildToken;

        Type conversionType = (Type) TypeLattice.lattice().leastUpperBound(
                token1.getType(), token2.getType());

        _evaluatedChildToken = conversionType.convert(token1);

        //if (node.isConstant()) {
        //    node.setToken(_evaluatedChildToken);
        //}

    }

    /** Evaluate a numeric constant or an identifier. In the case of an
     *  identifier, its value is obtained from the scope or from the list
     *  of registered constants.
     *  @param node The specified node.
     *  @exception IllegalActionException If an parse error occurs.
     */
    public void visitLeafNode(ASTPtLeafNode node) throws IllegalActionException {
        if (node.isConstant() && node.isEvaluated()) {
            _evaluatedChildToken = node.getToken();
            if (_evaluatedChildToken instanceof ComplexToken) {
                Complex complex = ((ComplexToken) _evaluatedChildToken)
                        .complexValue();
                _childCode = "$Complex_new(" + complex.real + ", "
                        + complex.imag + ")";
            } else if (_evaluatedChildToken instanceof StringToken) {
                // In C, Strings should have \n tags substituted.
                // See Test 17.2

                //_fireCode.append(escapeForTargetLanguage(_evaluatedChildToken.toString()));

                _childCode = escapeForTargetLanguage(_evaluatedChildToken
                        .toString());

            } else if (_evaluatedChildToken instanceof LongToken) {
                //_fireCode.append(((LongToken) _evaluatedChildToken).longValue() + "LL");
                _childCode = ((LongToken) _evaluatedChildToken).longValue()
                        + "L";
            } else {
                //_fireCode.append(_evaluatedChildToken.toString());
                _childCode = _evaluatedChildToken.toString();
            }
            return;
        }

        String name = node.getName();

        // The node refers to a variable, or something else that is in
        // scope
        ptolemy.data.Token value = null;

        if (_scope != null) {
            value = _scope.get(name);
        }

        // Look up for constants.
        if (value == null) {
            // A named constant that is recognized by the parser.
            value = Constants.get(name);
        }

        // Set the value, if we found one.
        if (value != null) {
            _evaluatedChildToken = value;

            String label = value.toString();
            if (label.startsWith("object(")) {

                // If this is an ObjectToken, we only wants the label.
                //_fireCode.append(label.substring(7, label.length() - 1));

                _childCode = label.substring(7, label.length() - 1).trim();
                //if (_childCode.equals("object(null)")) {
                //    _childCode = "null";
                //}

            } else {
                // FIXME: handle the rest of the constants from data.expr.Constants
                if (label.equals("Infinity")) {
                    _childCode = "Double.POSITIVE_INFINITY";
                } else if (label.equals("NaN")) {
                    // $PTII/bin/ptcg -language java ./adapter/generic/program/procedural/java/adapters/ptolemy/actor/lib/test/auto/TestNaN.xml
                    _childCode = "Double.NaN";
                } else {
                    //_fireCode.append(label);
                    _childCode = label;
                }
            }

            return;
        }

        throw new IllegalActionException("The ID " + node.getName()
                + " is undefined.");
    }

    /** Construct a matrix containing the children nodes.
     *  The specified node ends up with a MatrixToken value.
     *  @param node The specified node.
     *  @exception IllegalActionException If an parse error occurs.
     */
    public void visitMatrixConstructNode(ASTPtMatrixConstructNode node)
            throws IllegalActionException {
        if (node.isConstant() && node.isEvaluated()) {
            _evaluatedChildToken = node.getToken();
            //_fireCode.append(_evaluatedChildToken.toString());
            _childCode = _evaluatedChildToken.toString();
            return;
        }

        //ptolemy.data.Token[] tokens = _evaluateAllChildren(node);
        int row = node.getRowCount();
        int column = node.getColumnCount();
        ptolemy.data.Token[] tokens = new ptolemy.data.Token[row * column];

        //_fireCode.append("((Token*) $new(Matrix(" + row + ", " + column);
        StringBuffer result = new StringBuffer(row + ", " + column + ", " + row
                * column);

        ptolemy.data.Token childToken = null;
        ptolemy.data.type.Type elementType = BaseType.UNKNOWN;

        if (node.getForm() == 1) {
            for (int i = 0; i < row; i++) {
                for (int j = 0; j < column; j++) {
                    //_fireCode.append(", ");
                    result.append(", ");

                    int index = i * column + j;

                    //int nextIndex = _fireCode.length();
                    tokens[index] = _evaluateChild(node, index);

                    Type valueType = tokens[index].getType();

                    if (_isPrimitive(valueType)) {
                        //_fireCode.insert(nextIndex, "$new(" + _codeGenType(valueType) + "(");
                        //_fireCode.append("))");
                        result.append("$new(" + _codeGenType(valueType) + "("
                                + _childCode + "))");
                    }

                    if (!elementType.equals(valueType)) { // find max type
                        elementType = TypeLattice.leastUpperBound(elementType,
                                valueType);
                    }
                }
            }

            String codegenType = _codeGenType(elementType);

            // Insert the elementType of the array as the last argument.
            if (_targetType(elementType).equals("Token")) {
                //_fireCode.append(", -1");
                result.append(", -1");
            } else {
                //_fireCode.append(", TYPE_" + _codeGenType(elementType));
                result.append(", TYPE_" + codegenType);
            }
            //_fireCode.append(")))");
            result = new StringBuffer("($new(" + /*codegenType + */"Matrix("
                    + result.toString());
            _childCode = result.toString() + ")))";

            childToken = MatrixToken.arrayToMatrix(tokens, node.getRowCount(),
                    node.getColumnCount());
        } else if (node.getForm() == 2) {
            try {
                // FIXME: Do codegen for the matlab form.
                int columnCount = MatrixToken.determineSequenceLength(
                        (ScalarToken) tokens[0], (ScalarToken) tokens[1],
                        (ScalarToken) tokens[2]);

                // Make sure that all following rows have the same number
                // of columns.
                for (int i = 1; i < node.getRowCount(); ++i) {
                    if (columnCount != MatrixToken.determineSequenceLength(
                            (ScalarToken) tokens[3 * i],
                            (ScalarToken) tokens[3 * i + 1],
                            (ScalarToken) tokens[3 * i + 2])) {
                        throw new IllegalActionException("Matrix "
                                + "should have the same number of columns "
                                + "for all rows.");
                    }
                }

                ptolemy.data.Token[] matrixTokens = new ptolemy.data.Token[node
                        .getRowCount() * columnCount];

                for (int i = 0; i < node.getRowCount(); i++) {
                    ptolemy.data.Token[] newTokens = MatrixToken
                            .createSequence(tokens[3 * i], tokens[3 * i + 1],
                                    columnCount);
                    System.arraycopy(newTokens, 0, matrixTokens, columnCount
                            * i, columnCount);
                }

                childToken = MatrixToken.arrayToMatrix(matrixTokens,
                        node.getRowCount(), columnCount);
            } catch (IllegalActionException ex) {
                // FIXME: better detail message that includes the thing
                // we were parsing.
                throw new IllegalActionException(null, null, ex,
                        "Matrix Token construction failed.");
            }
        }

        _evaluatedChildToken = childToken;

        //if (node.isConstant()) {
        //    node.setToken(_evaluatedChildToken);
        //}
    }

    /** Multiply the children of the specified node.
     *  @param node The specified node.
     *  @exception IllegalActionException If an parse error occurs.
     */
    public void visitProductNode(ASTPtProductNode node)
            throws IllegalActionException {
        if (node.isConstant() && node.isEvaluated()) {
            _evaluatedChildToken = node.getToken();
            //_fireCode.append(_evaluatedChildToken.toString());
            _childCode = _evaluatedChildToken.toString();
            return;
        }

        //ptolemy.data.Token[] tokens = _evaluateAllChildren(node);
        List lexicalTokenList = node.getLexicalTokenList();
        int numChildren = node.jjtGetNumChildren();
        _assert(numChildren > 0, node,
                "The number of child nodes must be greater than zero");
        _assert(numChildren == lexicalTokenList.size() + 1, node,
                "The number of child nodes is "
                        + "not equal to number of operators plus one");

        //_fireCode.append("(");
        StringBuffer result = new StringBuffer();

        //ptolemy.data.Token result = tokens[0];
        ptolemy.data.Token childToken = _evaluateChild(node, 0);
        result.append(_childCode);
        Type resultType = ((ASTPtRootNode) node.jjtGetChild(0)).getType();

        for (int i = 1; i < numChildren; i++) {
            Token operator = (Token) lexicalTokenList.get(i - 1);

            _evaluateChild(node, i);

            Type type = ((ASTPtRootNode) node.jjtGetChild(i)).getType();

            if (operator.kind == PtParserConstants.MULTIPLY) {
                if (type != null) {
                    result = new StringBuffer("$multiply_"
                            + _codeGenType(resultType) + "_"
                            + _codeGenType(type) + "(" + result.toString()
                            + ", " + _childCode + ")");

                    resultType = resultType.multiply(type);

                } else {
                    result.append("*" + _childCode);
                }
            } else if (operator.kind == PtParserConstants.DIVIDE) {
                if (type != null) {
                    result = new StringBuffer("$divide_"
                            + _codeGenType(resultType) + "_"
                            + _codeGenType(type) + "(" + result.toString()
                            + ", " + _childCode + ")");

                    resultType = resultType.divide(type);

                } else {
                    result.append("/" + _childCode);
                }
            } else if (operator.kind == PtParserConstants.MODULO) {
                if (type != null) {
                    result = new StringBuffer("$modulo_"
                            + _codeGenType(resultType) + "_"
                            + _codeGenType(type) + "(" + result.toString()
                            + ", " + _childCode + ")");

                    resultType = resultType.divide(type);

                } else {
                    result.append("%" + _childCode);
                }
            }

            if (operator.kind == PtParserConstants.MULTIPLY) {
                //childToken = childToken.multiply(nextToken);
            } else if (operator.kind == PtParserConstants.DIVIDE) {
                //childToken = childToken.divide(nextToken);
            } else if (operator.kind == PtParserConstants.MODULO) {
                //childToken = childToken.modulo(nextToken);
            } else {
                _assert(false, node, "Invalid operation");
            }
        }

        //_fireCode.append(")");
        _childCode = "(" + result + ")";

        _evaluatedChildToken = childToken;

        //if (node.isConstant()) {
        //    node.setToken(_evaluatedChildToken);
        //}
    }

    /** Construct a record by assigning the fields values given by
     *  the children nodes.
     *  @param node The record constructor node.
     *  @exception IllegalActionException If an parse error occurs.
     */
    public void visitRecordConstructNode(ASTPtRecordConstructNode node)
            throws IllegalActionException {
        if (node.isConstant() && node.isEvaluated()) {
            _evaluatedChildToken = node.getToken();
            return;
        }
        int numChildren = node.jjtGetNumChildren();

        StringBuffer result = new StringBuffer();

        //ptolemy.data.Token[] tokens = _evaluateAllChildren(node);
        ptolemy.data.Token[] tokens = new ptolemy.data.Token[numChildren];
        for (int i = 0; i < numChildren; i++) {
            tokens[i] = _evaluateChild(node, i);
            result.append(_childCode);
        }

        _assert(node.getFieldNames().size() == numChildren, node,
                "The number of labels and values does not "
                        + "match in parsing a record expression.");

        String[] labels = (String[]) node.getFieldNames().toArray(
                new String[numChildren]);

        if (node instanceof ASTPtOrderedRecordConstructNode) {
            _evaluatedChildToken = new OrderedRecordToken(labels, tokens);
        } else {
            _evaluatedChildToken = new RecordToken(labels, tokens);
        }

        //if (node.isConstant()) {
        //    node.setToken(_evaluatedChildToken);
        //}

        _childCode = result.toString();
    }

    public void visitRelationalNode(ASTPtRelationalNode node)
            throws IllegalActionException {
        if (node.isConstant() && node.isEvaluated()) {
            _evaluatedChildToken = node.getToken();
            //_fireCode.append(_evaluatedChildToken.toString());
            _childCode = _evaluatedChildToken.toString();
            return;
        }

        //ptolemy.data.Token[] tokens = _evaluateAllChildren(node);
        //_fireCode.append("(");
        StringBuffer result = new StringBuffer();

        int numChildren = node.jjtGetNumChildren();
        _assert(numChildren == 2, node, "The number of child nodes must be two");

        Token operator = node.getOperator();
        ptolemy.data.Token leftToken = _evaluateChild(node, 0);
        result.append(_childCode);

        if (operator.kind == PtParserConstants.EQUALS) {
            //_fireCode.append(" == ");
            result.append(" == ");
        } else if (operator.kind == PtParserConstants.NOTEQUALS) {
            //_fireCode.append(" != ");
            result.append(" != ");
        } else if (operator.kind == PtParserConstants.GTE) {
            //_fireCode.append(" >= ");
            result.append(" >= ");
        } else if (operator.kind == PtParserConstants.GT) {
            //_fireCode.append(" > ");
            result.append(" > ");
        } else if (operator.kind == PtParserConstants.LTE) {
            //_fireCode.append(" <= ");
            result.append(" <= ");
        } else if (operator.kind == PtParserConstants.LT) {
            //_fireCode.append(" < ");
            result.append(" < ");
        }

        ptolemy.data.Token rightToken = _evaluateChild(node, 1);
        //ptolemy.data.Token resultToken = null;

        //_fireCode.append(")");
        _childCode = "(" + result + _childCode + ")";

        if (operator.kind == PtParserConstants.EQUALS) {
            //resultToken = leftToken.isEqualTo(rightToken);
        } else if (operator.kind == PtParserConstants.NOTEQUALS) {
            //resultToken = leftToken.isEqualTo(rightToken).not();
        } else { /*
                              if (!((leftToken instanceof ScalarToken) && (rightToken instanceof ScalarToken))) {
                              throw new IllegalActionException("The " + operator.image
                              + " operator can only be applied between scalars.");
                              }

                              ScalarToken leftScalar = (ScalarToken) leftToken;
                              ScalarToken rightScalar = (ScalarToken) rightToken;
                 */

            if (operator.kind == PtParserConstants.GTE) {
                //result = leftScalar.isLessThan(rightScalar).not();
            } else if (operator.kind == PtParserConstants.GT) {
                //result = rightScalar.isLessThan(leftScalar);
            } else if (operator.kind == PtParserConstants.LTE) {
                //result = rightScalar.isLessThan(leftScalar).not();
            } else if (operator.kind == PtParserConstants.LT) {
                //result = leftScalar.isLessThan(rightScalar);
            } else {
                throw new IllegalActionException("Invalid operation "
                        + operator.image + " between "
                        + leftToken.getClass().getName() + " and "
                        + rightToken.getClass().getName());
            }
        }

        // FindBugs reports "Load of known null value"
        //_evaluatedChildToken = (resultToken);
        _evaluatedChildToken = null;

        //if (node.isConstant()) {
        //    node.setToken(_evaluatedChildToken);
        //}
    }

    /** Apply a shift operator to the children of the specified node.
     *  @param node The specified node.
     *  @exception IllegalActionException If an parse error occurs.
     */
    public void visitShiftNode(ASTPtShiftNode node)
            throws IllegalActionException {
        if (node.isConstant() && node.isEvaluated()) {
            _evaluatedChildToken = node.getToken();
            //_fireCode.append(_evaluatedChildToken.toString());
            _childCode = _evaluatedChildToken.toString();
            return;
        }

        //ptolemy.data.Token[] tokens = _evaluateAllChildren(node);
        int numChildren = node.jjtGetNumChildren();
        _assert(numChildren == 2, node, "The number of child nodes must be two");

        Token operator = node.getOperator();

        //ptolemy.data.Token token = tokens[0];
        //ptolemy.data.Token bitsToken = tokens[1];

        //_fireCode.append("(");

        /*ptolemy.data.Token token =*/_evaluateChild(node, 0);
        StringBuffer result = new StringBuffer(_childCode);

        if (operator.kind == PtParserConstants.SHL) {
            //_fireCode.append(" << ");
            result.append(" << ");
        } else if (operator.kind == PtParserConstants.SHR) {
            //_fireCode.append(" >> ");
            result.append(" >> ");
        } else if (operator.kind == PtParserConstants.LSHR) {
            //_fireCode.append(" >>> ");
            result.append(" >>> ");
        }

        /*ptolemy.data.Token bitsToken =*/_evaluateChild(node, 1);
        //ptolemy.data.Token resultToken = null;

        //_fireCode.append(")");
        _childCode = "(" + result + _childCode + ")";

        /*
        if (!(token instanceof ScalarToken)) {
            throw new IllegalActionException("The " + operator
                    + " operator requires "
                    + "the left operand to be a scalar.");
        }

        if (!(bitsToken instanceof ScalarToken)) {
            throw new IllegalActionException("The " + operator
                    + " operator requires "
                    + "the right operand to be a scalar.");
        }

        // intValue() is used rather than testing for IntToken
        // because any token with an intValue() is OK.  However,
        // we need a try...catch to generate a proper error message.
        try {
            if (operator.kind == PtParserConstants.SHL) {
                resultToken = ((ScalarToken) token)
                        .leftShift(((ScalarToken) bitsToken).intValue());
            } else if (operator.kind == PtParserConstants.SHR) {
                resultToken = ((ScalarToken) token)
                        .rightShift(((ScalarToken) bitsToken).intValue());
            } else if (operator.kind == PtParserConstants.LSHR) {
                resultToken = ((ScalarToken) token)
                        .logicalRightShift(((ScalarToken) bitsToken).intValue());
            } else {
                _assert(false, node, "Invalid operation");
            }
        } catch (IllegalActionException ex) {
            throw new IllegalActionException("The " + operator
                    + " operator requires "
                    + "the right operand to have an integer value.");
        }
         */

        // FindBugs reports "Load of known null value"
        //_evaluatedChildToken = (resultToken);
        _evaluatedChildToken = null;

        //if (node.isConstant()) {
        //    node.setToken(_evaluatedChildToken);
        //}
    }

    /** Apply a sum operator to the children of the specified node.
     *  @param node The specified node.
     *  @exception IllegalActionException If an parse error occurs.
     */
    public void visitSumNode(ASTPtSumNode node) throws IllegalActionException {
        if (node.isConstant() && node.isEvaluated()) {
            _evaluatedChildToken = node.getToken();
            //_fireCode.append(_evaluatedChildToken.toString());
            _childCode = _evaluatedChildToken.toString();
            return;
        }

        //ptolemy.data.Token[] tokens = _evaluateAllChildren(node);
        List lexicalTokenList = node.getLexicalTokenList();
        int numChildren = node.jjtGetNumChildren();
        _assert(numChildren > 0, node,
                "The number of child nodes must be greater than zero");
        _assert(numChildren == lexicalTokenList.size() + 1, node,
                "The number of child nodes is "
                        + "not equal to number of operators plus one");

        //ptolemy.data.Token result = tokens[0];
        StringBuffer result = new StringBuffer();

        ptolemy.data.Token childToken = _evaluateChild(node, 0);

        String childType = _codeGenType(((ASTPtRootNode) node.jjtGetChild(0))
                .getType());

        String nodeType = _codeGenType(node.getType());

        result.append("$convert_" + childType + "_" + nodeType + "("
                + _childCode + ")");

        for (int i = 1; i < numChildren; i++) {
            childType = _codeGenType(((ASTPtRootNode) node.jjtGetChild(i))
                    .getType());

            Token operator = (Token) lexicalTokenList.get(i - 1);

            //ptolemy.data.Token nextToken = tokens[i];
            if (operator.kind == PtParserConstants.PLUS) {
                result = new StringBuffer("$add_" + nodeType + "_" + childType
                        + "(" + result.toString() + ", ");

                //childToken = childToken.add(_evaluateChild(node, i));
            } else if (operator.kind == PtParserConstants.MINUS) {
                result = new StringBuffer("$subtract_" + nodeType + "_"
                        + childType + "(" + result.toString() + ", ");

                //childToken = childToken.subtract(_evaluateChild(node, i));
            } else {
                _assert(false, node, "Invalid operation");
            }

            _evaluateChild(node, i);
            result.append(_childCode + ")");
        }

        _childCode = "(" + result + ")";

        _evaluatedChildToken = childToken;

        //if (node.isConstant()) {
        //    node.setToken(_evaluatedChildToken);
        //}
    }

    /** Apply a unary operator to the single child of the specified node.
     *  @param node The specified node.
     *  @exception IllegalActionException If an parse error occurs.
     */
    public void visitUnaryNode(ASTPtUnaryNode node)
            throws IllegalActionException {
        if (node.isConstant() && node.isEvaluated()) {
            _evaluatedChildToken = node.getToken();
            //_fireCode.append(_evaluatedChildToken.toString());
            _childCode = _evaluatedChildToken.toString();
            return;
        }

        //ptolemy.data.Token[] tokens = _evaluateAllChildren(node);
        _assert(node.jjtGetNumChildren() == 1, node,
                "Unary node must have exactly one child!");

        String result = "";

        if (node.isMinus()) {
            //_fireCode.append("-");
            result = "-";
        } else if (node.isNot()) {
            //_fireCode.append("!");
            result = "!";
        } else if (node.isBitwiseNot()) {
            //_fireCode.append("~");
            result = "~";
        }

        //ptolemy.data.Token result = tokens[0];
        ptolemy.data.Token childToken = _evaluateChild(node, 0);
        _childCode = result + _childCode;

        /*
         if (node.isMinus()) {
         result = result.zero().subtract(result);
         } else if (node.isNot()) {
         if (result instanceof BooleanToken) {
         //result = ((BooleanToken) result).not();
         } else {
         throw new IllegalActionException(
         "Not operator not support for non-boolean token: "
         + result.toString());
         }
         } else if (node.isBitwiseNot()) {
         if (!(result instanceof BitwiseOperationToken)) {
         throw new IllegalActionException("Bitwise negation"
         + " not defined on " + result
         + " which does not support bitwise operations.");
         }

         //result = (ptolemy.data.Token) ((BitwiseOperationToken) result)
         //        .bitwiseNot();
         } else {
         _assert(false, node, "Unrecognized unary node");
         }
         */
        _evaluatedChildToken = childToken;

        //if (node.isConstant()) {
        //    node.setToken(_evaluatedChildToken);
        //}
    }

    ///////////////////////////////////////////////////////////////////
    ////                         protected methods                 ////

    /** Assert that the given boolean value, which describes the given
     *  parse tree node, is true.  If it is false, then throw a new
     *  InternalErrorException that describes the node and includes
     *  the given message.
     *  @param flag The flag that is asserted to be true.
     *  @param node The node on which the assertion is asserted.
     *  @param message The message to include in the exception.
     *  @exception InternalErrorException If the assertion is violated.
     *   Note that this is a runtime exception, so it need not be declared
     *   explicitly.
     */
    protected void _assert(boolean flag, ASTPtRootNode node, String message) {
        if (!flag) {
            throw new InternalErrorException(message + ": " + node.toString());
        }
    }

    /** Loop through all of the children of this node,
     *  visiting each one of them; this will cause their token
     *  value to be determined.
     *  @param node The node whose children are evaluated.
     *  @return The array of resulting tokens.
     *  @exception IllegalActionException If an parse error occurs.
     */
    protected ptolemy.data.Token[] _evaluateAllChildren(ASTPtRootNode node)
            throws IllegalActionException {
        int numChildren = node.jjtGetNumChildren();
        ptolemy.data.Token[] tokens = new ptolemy.data.Token[numChildren];

        for (int i = 0; i < numChildren; i++) {
            /* ASTPtRootNode child = (ASTPtRootNode) */node.jjtGetChild(i);
            tokens[i] = _evaluateChild(node, i);
        }

        return tokens;
    }

    /** Evaluate the array index operation represented by the given node.
     *  @param node The node that caused this method to be called.
     *  @param value The token that is being indexed into, which must
     *   be an ArrayToken.
     *  @param type The element type.
     *  @exception IllegalActionException If an parse error occurs.
     */
    protected void _evaluateArrayIndex(ASTPtRootNode node,
            ptolemy.data.Token value, Type type) throws IllegalActionException {

        //_fireCode.append("Array_get(");
        StringBuffer result = new StringBuffer("Array_get(");

        String name = value.toString();
        if (name.startsWith("object(")) {
            //_fireCode.append(name.substring(7, name.length() - 1) + ", ");
            result.append(name.substring(7, name.length() - 1) + ", ");
        }

        // get the array index
        _evaluateChild(node, 1);

        //_fireCode.append(")");
        result.append(_childCode + ")");

        Type elementType = ((ArrayType) type).getElementType();

        //_fireCode.append(".payload." + _codeGenType(elementType));
        _childCode = result.toString() + ".payload/*jptcg*/."
                + _codeGenType(elementType);
    }

    /** Evaluate the child with the given index of the given node.
     *  This is usually called while visiting the given node.
     *  @param node The given node.
     *  @param i The given index.
     *  @return The resulting token.
     *  @exception IllegalActionException If an parse error occurs.
     */
    protected ptolemy.data.Token _evaluateChild(ASTPtRootNode node, int i)
            throws IllegalActionException {
        ASTPtRootNode child = (ASTPtRootNode) node.jjtGetChild(i);
        _traceEnter(child);
        child.visit(this);
        _traceLeave(child);
        return _evaluatedChildToken;
    }

    //     /** Evaluate the Matrix index operation represented by the given node.
    //      *  @param node The node that caused this method to be called.
    //      *  @param value The token that is being indexed into, which must
    //      *   be a MatrixToken.
    //      *  @param rowIndex The row index, which must be an integer token.
    //      *  @param columnIndex The column index, which must be an integer token.
    //      *  @return The element of the given token at the given index.
    //      *  @exception IllegalActionException If an parse error occurs.
    //      */
    //     protected ptolemy.data.Token _evaluateMatrixIndex(ASTPtRootNode node,
    //             ptolemy.data.Token value, ptolemy.data.Token rowIndex,
    //             ptolemy.data.Token columnIndex) throws IllegalActionException {
    //         if (!(value instanceof MatrixToken)) {
    //             throw new IllegalActionException(
    //                     "Matrix indexing cannot be applied to '" + value.toString()
    //                             + "' because its value is not a matrix.");
    //         }

    //         if (!(rowIndex instanceof IntToken)) {
    //             throw new IllegalActionException(
    //                     "Matrix row index must be an integer. Got: " + rowIndex);
    //         }

    //         if (!(columnIndex instanceof IntToken)) {
    //             throw new IllegalActionException(
    //                     "Matrix column index must be an integer. Got: "
    //                             + columnIndex);
    //         }

    //         int integerRowIndex = ((IntToken) rowIndex).intValue();
    //         int integerColumnIndex = ((IntToken) columnIndex).intValue();

    //         try {
    //             return ((MatrixToken) value).getElementAsToken(integerRowIndex,
    //                     integerColumnIndex);
    //         } catch (ArrayIndexOutOfBoundsException ex) {
    //             throw new IllegalActionException("The index (" + rowIndex + ","
    //                     + columnIndex + ") is out of bounds on the matrix '"
    //                     + value + "'.");
    //         }
    //     }

    ///////////////////////////////////////////////////////////////////
    ////                         protected variables               ////

    static {
        _functionMap.put("matrixToArray", "$matrixToArray");
        _functionMap.put("roundToInt", "(int)");
        _functionMap.put("repeat", "$arrayRepeat");
        _functionMap.put("sum", "$arraySum");

        // Java Specific functions
        _functionMap.put("NaN", "Double.NaN");
        _functionMap.put("abs", "Math.abs");
        _functionMap.put("acos", "Math.acos");
        _functionMap.put("asin", "Math.asin");
        _functionMap.put("atan", "Math.atan");
        _functionMap.put("cbrt", "Math.cbrt");
        _functionMap.put("ceil", "Math.ceil");
        _functionMap.put("cos", "Math.cos");
        _functionMap.put("cosh", "Math.cosh");
        _functionMap.put("exp", "Math.exp");
        _functionMap.put("expm1", "Math.expm1");
        _functionMap.put("floor", "Math.floor");
        _functionMap.put("iterate",
                "ptolemy.data.expr.UtilityFunctions.iterate");
        _functionMap.put("log", "Math.log");
        _functionMap.put("log10", "Math.log10");
        _functionMap.put("log1p", "Math.log1p");
        _functionMap.put("max", "Math.max");
        _functionMap.put("min", "Math.min");
        _functionMap.put("pow", "Math.pow");
        _functionMap.put("rint", "Math.rint");
        _functionMap.put("round", "Math.round");
        _functionMap.put("signum", "Math.signum");
        _functionMap.put("sin", "Math.sin");
        _functionMap.put("sinh", "Math.sinh");
        _functionMap.put("sqrt", "Math.sqrt");
        _functionMap.put("tan", "Math.tan");
        _functionMap.put("tanh", "Math.tanh");
        _functionMap.put("toDegrees", "Math.toDegrees");
        _functionMap.put("toRadians", "Math.toRadians");
        _functionMap.put("ulp", "Math.ulp");
    }

    ///////////////////////////////////////////////////////////////////
    ////                         private methods                   ////

    /**
     * Get the corresponding type in code generation from the given Ptolemy
     * type.
     * @param ptType The given Ptolemy type.
     * @return The code generation type.
     * @exception IllegalActionException Thrown if the given ptolemy cannot
     *  be resolved.
     */
    private/*static*/String _codeGenType(Type ptType) {
        // FIXME: this is duplicated code from CodeGeneratorHelper.codeGenType

        // FIXME: We may need to add more types.
        // FIXME: We have to create separate type for different matrix types.
        String result = ptType == BaseType.INT ? "Int"
                : ptType == BaseType.LONG ? "Long"
                        : ptType == BaseType.STRING ? "String"
                                : ptType == BaseType.DOUBLE ? "Double"
                                        : ptType == BaseType.BOOLEAN ? "Boolean"
                                                : ptType == BaseType.UNSIGNED_BYTE ? "UnsignedByte"
                                                        //: ptType == PointerToken.POINTER ? "Pointer"
                                                        : ptType == BaseType.COMPLEX ? "Complex"
                                                                // FIXME: Why do we have to use equals with BaseType.OBJECT
                                                                : ptType.equals(BaseType.OBJECT) ? "Object"
                                                                        //: ptType == BaseType.OBJECT ? "Object"
                                                                        : null;

        if (result == null) {
            if (ptType instanceof ArrayType) {
                //result = codeGenType(((ArrayType) ptType).getElementType()) + "Array";
                result = "Array";
            } else if (ptType instanceof MatrixType) {
                //result = ptType.getClass().getSimpleName().replace("Type", "");
                result = "Matrix";
            }
        }

        //if (result.length() == 0) {
        //    throw new IllegalActionException(
        //            "Cannot resolve codegen type from Ptolemy type: " + ptType);
        //}

        // Java specific changes
        if (result != null) {
            return result.replace("Int", "Integer").replace("Array", "Token");
        }
        return result;
    }

    /**
     * Determine if the given type is primitive.
     * @param ptType The given ptolemy type.
     * @return true if the given type is primitive, otherwise false.
     */
    private boolean _isPrimitive(Type ptType) {
        // FIXME: this is duplicated code from CodeGeneratorHelper.isPrimitive()
        return _primitiveTypes.contains(_codeGenType(ptType));
    }

    /**
     * Get the corresponding type in Java from the given Ptolemy type.
     * @param ptType The given Ptolemy type.
     * @return The Java data type.
     */
    private String _targetType(Type ptType) {
        // FIXME: this is duplicated code from CodeGeneratorHelper.targetType()
        // FIXME: we may need to add more primitive types.
        return ptType == BaseType.INT ? "int"
                : ptType == BaseType.STRING ? "String"
                        : ptType == BaseType.DOUBLE ? "double"
                                : ptType == BaseType.BOOLEAN ? "boolean"
                                        : ptType == BaseType.LONG ? "long"
                                                : ptType == BaseType.UNSIGNED_BYTE ? "byte"
                                                        //: ptType == PointerToken.POINTER ? "void*"
                                                        : "Token";
    }

    ///////////////////////////////////////////////////////////////////
    ////                         private variables                 ////

    /** A static list of the primitive types supported by the code generator. */
    private static final List _primitiveTypes = Arrays.asList(new String[] {
            "Integer", "Double", "String", "Long", "Boolean", "UnsignedByte",
            "Pointer", "Complex", "Object" });
}
