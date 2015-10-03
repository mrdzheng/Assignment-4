package wci.backend.interpreter.executors;

import java.util.ArrayList;
import java.util.EnumSet;

import wci.intermediate.*;
import wci.intermediate.icodeimpl.*;
import wci.backend.interpreter.*;
import java.util.TreeSet;

import static wci.intermediate.symtabimpl.SymTabKeyImpl.*;
import static wci.intermediate.icodeimpl.ICodeNodeTypeImpl.*;
import static wci.intermediate.icodeimpl.ICodeKeyImpl.*;
import static wci.backend.interpreter.RuntimeErrorCode.*;

/**
 * <h1>ExpressionExecutor</h1>
 *
 * <p>Execute an expression.</p>
 *
 * <p>Copyright (c) 2009 by Ronald Mak</p>
 * <p>For instructional purposes only.  No warranties.</p>
 */
public class ExpressionExecutor extends StatementExecutor
{
    /**
     * Constructor.
     * @param the parent executor.
     */
    public ExpressionExecutor(Executor parent)
    {
        super(parent);
    }

    /**
     * Execute an expression.
     * @param node the root intermediate code node of the compound statement.
     * @return the computed value of the expression.
     */
    public Object execute(ICodeNode node)
    {
        ICodeNodeTypeImpl nodeType = (ICodeNodeTypeImpl) node.getType();

        switch (nodeType) {

            case VARIABLE: {

                // Get the variable's symbol table entry and return its value.
                SymTabEntry entry = (SymTabEntry) node.getAttribute(ID);
                return entry.getAttribute(DATA_VALUE);
            }

            case INTEGER_CONSTANT: {

                // Return the integer value.
                return (Integer) node.getAttribute(VALUE);
            }

            case REAL_CONSTANT: {

                // Return the float value.
                return (Float) node.getAttribute(VALUE);
            }

            case STRING_CONSTANT: {

                // Return the string value.
                return (String) node.getAttribute(VALUE);
            }

            case NEGATE: {

                // Get the NEGATE node's expression node child.
                ArrayList<ICodeNode> children = node.getChildren();
                ICodeNode expressionNode = children.get(0);

                // Execute the expression and return the negative of its value.
                Object value = execute(expressionNode);
                if (value instanceof Integer) {
                    return -((Integer) value);
                }
                else {
                    return -((Float) value);
                }
            }

            case NOT: {

                // Get the NOT node's expression node child.
                ArrayList<ICodeNode> children = node.getChildren();
                ICodeNode expressionNode = children.get(0);

                // Execute the expression and return the "not" of its value.
                boolean value = (Boolean) execute(expressionNode);
                return !value;
            }
            case SETS:
            {
                // Get the NOT node's expression node child.
                ArrayList<ICodeNode> children = node.getChildren();
                TreeSet<Integer> values = new TreeSet<>();
                int prev = 0;
                for (ICodeNode x : children)
                {
                    if (x.getType() == SUBRANGE)
                    {
                        setUpSubRangeSet(prev,(int)x.getAttribute(VALUE), values);
                        prev = (int)  x.getAttribute(VALUE) ;
                    } else
                    {
                        if(x.getAttribute(VALUE)!=null)
                        {
                            values.add((Integer) x.getAttribute(VALUE));
                            prev = (int)  x.getAttribute(VALUE) ;
                        }
                        else //it must be an identifier.
                        {
                            int val = (int) execute(x);
                            values.add(val);
                            prev = val ;
                        }
                    }
                }
                return values;
 
            }

            // Must be a binary operator.
            default: return executeBinaryOperator(node, nodeType);
        }
    }

    // Set of arithmetic operator node types.
    private static final EnumSet<ICodeNodeTypeImpl> ARITH_OPS =
        EnumSet.of(ADD, SUBTRACT, MULTIPLY, FLOAT_DIVIDE, INTEGER_DIVIDE, MOD);

    /**
     * Execute a binary operator.
     * @param node the root node of the expression.
     * @param nodeType the node type.
     * @return the computed value of the expression.
     */
    private Object executeBinaryOperator(ICodeNode node,
                                         ICodeNodeTypeImpl nodeType)
    {
        // Get the two operand children of the operator node.
        ArrayList<ICodeNode> children = node.getChildren();
        ICodeNode operandNode1 = children.get(0);
        ICodeNode operandNode2 = children.get(1);

        // Operands.
        Object operand1 = execute(operandNode1);
        Object operand2 = execute(operandNode2);

        boolean setMode = (operand1 instanceof TreeSet)
                && (operand2 instanceof TreeSet);

        boolean integerMode = (operand1 instanceof Integer)
                && (operand2 instanceof Integer);

        boolean specialInMode = (operand1 instanceof Integer)
                && (operand2 instanceof TreeSet);

        // ====================
        // Arithmetic operators
        // ====================

        if (ARITH_OPS.contains(nodeType)) {
            if (integerMode) {
                int value1 = (Integer) operand1;
                int value2 = (Integer) operand2;

                // Integer operations.
                switch (nodeType) {
                    case ADD:      return value1 + value2;
                    case SUBTRACT: return value1 - value2;
                    case MULTIPLY: return value1 * value2;

                    case FLOAT_DIVIDE: {

                        // Check for division by zero.
                        if (value2 != 0) {
                            return ((float) value1)/((float) value2);
                        }
                        else {
                            errorHandler.flag(node, DIVISION_BY_ZERO, this);
                            return 0;
                        }
                    }

                    case INTEGER_DIVIDE: {

                        // Check for division by zero.
                        if (value2 != 0) {
                            return value1/value2;
                        }
                        else {
                            errorHandler.flag(node, DIVISION_BY_ZERO, this);
                            return 0;
                        }
                    }

                    case MOD:  {

                        // Check for division by zero.
                        if (value2 != 0) {
                            return value1%value2;
                        }
                        else {
                            errorHandler.flag(node, DIVISION_BY_ZERO, this);
                            return 0;
                        }
                    }
                }
            } else if (setMode)
            {

                TreeSet<Integer> value1 = (TreeSet) operand1;
                TreeSet<Integer> value2 = (TreeSet) operand2;

                switch (nodeType)
                {
                    case ADD:    // union of the two sets  
                    {
                        TreeSet<Integer> finalSet = new TreeSet<Integer>();
                        for (Integer x : value1)
                        {
                            finalSet.add(x);
                        }
                        for (Integer y : value2)
                        {
                            finalSet.add(y);
                        }
                        return finalSet;

                    }
                    case SUBTRACT: //difference of the two sets.
                    {
                        TreeSet<Integer> finalSet = new TreeSet<Integer>();
                        for (Integer x : value1)
                        {
                            if (!value2.contains(x))
                            {
                                finalSet.add(x);
                            }
                        }
                        return finalSet;

                    }
                    case MULTIPLY: //intesection of the two sets.
                    {
                        TreeSet<Integer> finalSet = new TreeSet<>();
                        for (Integer x : value1)
                        {

                            if (value2.contains(x))
                            {
                                finalSet.add(x);
                            }
                        }
                        return finalSet;
                    }
                }
            } else
            {
                float value1 = operand1 instanceof Integer
                                   ? (Integer) operand1 : (Float) operand1;
                float value2 = operand2 instanceof Integer
                                   ? (Integer) operand2 : (Float) operand2;

                // Float operations.
                switch (nodeType) {
                    case ADD:      return value1 + value2;
                    case SUBTRACT: return value1 - value2;
                    case MULTIPLY: return value1 * value2;

                    case FLOAT_DIVIDE: {

                        // Check for division by zero.
                        if (value2 != 0.0f) {
                            return value1/value2;
                        }
                        else {
                            errorHandler.flag(node, DIVISION_BY_ZERO, this);
                            return 0.0f;
                        }
                    }
                }
            }
        }

        // ==========
        // AND and OR
        // ==========

        else if ((nodeType == AND) || (nodeType == OR)) {
            boolean value1 = (Boolean) operand1;
            boolean value2 = (Boolean) operand2;

            switch (nodeType) {
                case AND: return value1 && value2;
                case OR:  return value1 || value2;
            }
        }

        // ====================
        // Relational operators
        // ====================

        else if (integerMode) {
            int value1 = (Integer) operand1;
            int value2 = (Integer) operand2;

            // Integer operands.
            switch (nodeType) {
                case EQ: return value1 == value2;
                case NE: return value1 != value2;
                case LT: return value1 <  value2;
                case LE: return value1 <= value2;
                case GT: return value1 >  value2;
                case GE: return value1 >= value2;
            }
         } else if (specialInMode)
        {
            Integer value1 = (Integer) operand1;
            TreeSet<Integer> value2 = (TreeSet) operand2;
            if (nodeType == IN_NODE)
            {
                if (value2.contains(value1))
                {
                    return true;
                } else
                {
                    return false;
                }
            }
        } else if (setMode)
        {

            TreeSet<Integer> value1 = (TreeSet) operand1;
            TreeSet<Integer> value2 = (TreeSet) operand2;

            // Set operands.
            switch (nodeType)
            {
                case LE:
                {
                    //value2 is a superset if it contains every entry in value 1.
                    for(Integer x: value1)
                    {
                        if(!value2.contains(x))
                            return false;
                    }
                    return true;
                }
                case GE:
                {
                     //value2 is a superset if it contains every entry in value 1.
                    for(Integer x: value2)
                    {
                        if(!value1.contains(x))
                            return false;
                    }
                    return true;
                }
                case NE:
                {
                    for(Integer x: value1)
                    {
                        if(!value2.contains(x))
                            return true;
                    }
                    for(Integer x: value2)
                    {
                        if(!value1.contains(x))
                            return true;
                    }
                    return false;
                    
                }
                
                case EQ:
                {

                    for(Integer x: value1)
                    {
                        if(!value2.contains(x))
                            return false;
                    }
                    for(Integer x: value2)
                    {
                        if(!value1.contains(x))
                            return false;
                    }
                    return true;

                }
            }
        } else
        {
            float value1 = operand1 instanceof Integer
                               ? (Integer) operand1 : (Float) operand1;
            float value2 = operand2 instanceof Integer
                               ? (Integer) operand2 : (Float) operand2;

            // Float operands.
            switch (nodeType) {
                case EQ: return value1 == value2;
                case NE: return value1 != value2;
                case LT: return value1 <  value2;
                case LE: return value1 <= value2;
                case GT: return value1 >  value2;
                case GE: return value1 >= value2;
            }
        }

        return 0;  // should never get here
    }
    private void setUpSubRangeSet(int min,int max,
            TreeSet<Integer> values)
    {

        for (int i = min; i <= max; i++)
        {
            values.add(i);
        }
    }
}
