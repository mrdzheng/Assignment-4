/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wci.frontend.pascal.parsers;

import java.util.EnumSet;
import wci.frontend.Token;
import static wci.frontend.pascal.PascalErrorCode.MISSING_LEFT_BRACKET;
import static wci.frontend.pascal.PascalErrorCode.MISSING_OF;
import static wci.frontend.pascal.PascalErrorCode.MISSING_RIGHT_BRACKET;
import wci.frontend.pascal.PascalParserTD;
import wci.frontend.pascal.PascalTokenType;
import static wci.frontend.pascal.PascalTokenType.LEFT_BRACKET;
import static wci.frontend.pascal.PascalTokenType.OF;
import static wci.frontend.pascal.PascalTokenType.RIGHT_BRACKET;
import static wci.frontend.pascal.PascalTokenType.SEMICOLON;
import wci.intermediate.TypeFactory;
import wci.intermediate.TypeSpec;
import static wci.intermediate.typeimpl.TypeFormImpl.*;
import static wci.intermediate.typeimpl.TypeKeyImpl.ARRAY_ELEMENT_TYPE;
import static wci.intermediate.typeimpl.TypeKeyImpl.SET_ELEMENT_TYPE;

/**
 *
 * @author rawiyah
 */
class SetTypeParser extends TypeSpecificationParser{
    /**
     * Constructor.
     * @param parent the parent parser.
     */
    protected SetTypeParser(PascalParserTD parent)
    {
        super(parent);
    }
    // Synchronization set for the [ token.
    private static final EnumSet<PascalTokenType> LEFT_BRACKET_SET =
        SimpleTypeParser.SIMPLE_TYPE_START_SET.clone();
    static {
        LEFT_BRACKET_SET.add(LEFT_BRACKET);
        LEFT_BRACKET_SET.add(RIGHT_BRACKET);
    }

    // Synchronization set for the ] token.
    private static final EnumSet<PascalTokenType> RIGHT_BRACKET_SET =
        EnumSet.of(RIGHT_BRACKET, OF, SEMICOLON);

    // Synchronization set for OF.
    private static final EnumSet<PascalTokenType> OF_SET =
        TypeSpecificationParser.TYPE_START_SET.clone();
    static {
        OF_SET.add(OF);
        OF_SET.add(SEMICOLON);
    }
     /**
     * Parse a Pascal Set type specification.
     * @param token the current token.
     * @return the set type specification.
     * @throws Exception if an error occurred.
     */
    public TypeSpec parse(Token token)
        throws Exception
    {
        TypeSpec setType = TypeFactory.createType(SET);
        token = nextToken();  // consume SET word

        // Synchronize at OF.
        token = synchronize(OF_SET);
        if (token.getType() == OF) {
            token = nextToken();  // consume OF
        }
        else {
            errorHandler.flag(token, MISSING_OF, this);
        }
        TypeSpec elementType=setType;

        // Parse the element type.
        elementType.setAttribute(SET_ELEMENT_TYPE, parseElementType(token));

        return setType;
    }
      /**
     * Parse the element type specification.
     * @param token the current token.
     * @return the element type specification.
     * @throws Exception if an error occurred.
     */
    private TypeSpec parseElementType(Token token)
        throws Exception
    {
        TypeSpecificationParser typeSpecificationParser =
            new TypeSpecificationParser(this);
        return typeSpecificationParser.parse(token);
    }
}
