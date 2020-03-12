package lu.intrasoft.jspwiki.plugin;

import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.Properties;

/**
 *  SyntaxTokenizer.java
 * 
 *  @author  roy  (mailto:phillips.roy@gmail.com)
 *  @version "%I%, %G%"
 *  @since   Sep 28, 2006
 *
 *  SyntaxTokenizer is a specialised StreamTokenizer that reads its 
 *  configuration settings from either a provided configuration, or
 *  some reasonable defaults
 */

public class SyntaxTokenizer
    extends StreamTokenizer
{
    /**
     * Constructor building a tokenizer using 
     * build-in default values
     * 
     * @param reader - the input to get token stream from 
     */
    public SyntaxTokenizer ( Reader reader)
    {
        super ( reader);
        initialise ( get_defaults());
    }
    
    /**
     * Constructor building a tokenizer using supplied configuration
     * values, overriding the built in defaults where appropriate
     * 
     * @param readerthe input to get token stream from 
     * @param configuration the syntax/formatting values
     */
    public SyntaxTokenizer ( Reader reader, Properties configuration)
    {
        this ( reader);
        initialise ( configuration);
    }
    
    /**
     * Configure the tokenizer behaviour from the supplied configuration
     * 
     * @param configuration the properties to user 
     */
    private void
    initialise ( Properties configuration)
    {
        String eolIsSignificant = configuration.getProperty ( tokenizer_eolIsSignificant);
        if ( eolIsSignificant != null && eolIsSignificant.length() > 0)
        {
            this.eolIsSignificant ( Boolean.valueOf(eolIsSignificant).booleanValue ());
        }
        String lowerCaseMode = configuration.getProperty ( tokenizer_lowerCaseMode);
        if ( lowerCaseMode != null && lowerCaseMode.length() > 0)
        {
            this.lowerCaseMode ( Boolean.valueOf(lowerCaseMode).booleanValue ());
        }
        String parseNumbers = configuration.getProperty ( tokenizer_parseNumbers);
        if ( parseNumbers != null && parseNumbers.length() > 0)
        {
            this.lowerCaseMode ( Boolean.valueOf(parseNumbers).booleanValue ());
        }
        String slashSlashComments = configuration.getProperty ( tokenizer_slashSlashComments);
        if ( slashSlashComments != null && slashSlashComments.length() > 0)
        {
            this.lowerCaseMode ( Boolean.valueOf(slashSlashComments).booleanValue ());
        }
        String slashStarComments = configuration.getProperty ( tokenizer_slashStarComments);
        if ( slashStarComments != null && slashStarComments.length() > 0)
        {
            this.lowerCaseMode ( Boolean.valueOf(slashStarComments).booleanValue ());
        }
        
        String ordinaryChar = configuration.getProperty ( tokenizer_ordinaryChar);
        if ( ordinaryChar != null && ordinaryChar.length() > 0)
        {
            StringBuffer ordins = new StringBuffer();
            String[] chars = ordinaryChar.split ( ",");
            for ( int i = 0; chars != null && i < chars.length; i++)
            {
                char chari = (char)parse_char_code ( chars[i]);
                this.ordinaryChar ( chari);
                ordins.append ( chari);
            }
        }
        String ordinaryChars = configuration.getProperty ( tokenizer_ordinaryChars);
        if ( ordinaryChars != null && ordinaryChars.length() > 0)
        {
            String[] chars = ordinaryChars.split ( "-");
            if ( chars != null && chars.length == 2)
            { 
                int lo = parse_char_code ( chars[0]);
                int hi = parse_char_code ( chars[1]);
                this.ordinaryChars ( lo, hi);
            }
        }
        String quoteChar = configuration.getProperty ( tokenizer_quoteChar);
        if ( quoteChar != null && quoteChar.length() > 0)
        {
            String[] chars = quoteChar.split ( ",");
            for ( int i = 0; chars != null && i < chars.length; i++)
            {
                this.quoteChar ( parse_char_code ( chars[i]));
            }
        }
        
        String whitespaceChars = configuration.getProperty ( tokenizer_whitespaceChars);
        if ( whitespaceChars != null && whitespaceChars.length() > 0)
        {
            String[] chars = whitespaceChars.split ( "-");
            if ( chars != null && chars.length == 2)
            {
                int lo = parse_char_code ( chars[0]);
                int hi = parse_char_code ( chars[1]);
                this.whitespaceChars ( lo, hi);
            }
        }
        
        String wordChar = configuration.getProperty ( tokenizer_wordChar);
        if ( wordChar != null && wordChar.length() > 0)
        {
            String[] chars = wordChar.split ( ",");
            for ( int i = 0; chars != null && i < chars.length; i++)
            {
                int chari = parse_char_code ( chars[i]);
                this.wordChars ( chari, chari);
            }
        }
        
        String wordChars = configuration.getProperty ( tokenizer_wordChars);
        if ( wordChars != null && wordChars.length() > 0)
        {
            String[] chars = wordChars.split ( "-");
            if ( chars != null && chars.length == 2)
            {
                int lo = parse_char_code ( chars[0]);
                int hi = parse_char_code ( chars[1]);
                this.wordChars ( lo, hi);
            }
        }
    }
    
    /**
     * Interpret a string as a character and return it's value; the string
     * may be a numeric value (20, 0x32, #40) or a literal character, such
     * as ' '
     * 
     * @param code string representing a single character
     * @return the appropate integer code for the character supplied
     */
    private int
    parse_char_code ( String code)
    {
        int result = 0;
        try
        {
            result = Integer.decode(code).intValue(); 
        }
        catch ( NumberFormatException nfe)
        {
            result = Character.getNumericValue ( code.charAt(0));
        }
        if ( result < 0)
        {
            result = (int)code.charAt(0);
        }
        return result;
    }
    
    /**
     * Create a  set of default properties to configure the tokenizer
     * 
     * @return the default property set
     */
    private Properties
    get_defaults()
    {
        Properties result = new Properties();
        
        result.put ( tokenizer_eolIsSignificant, "true");
        result.put ( tokenizer_lowerCaseMode, "false");
        result.put ( tokenizer_ordinaryChar, "_,0x20");
        result.put ( tokenizer_parseNumbers, "true");
        result.put ( tokenizer_quoteChar, "',\"");
        result.put ( tokenizer_slashSlashComments, "false");
        result.put ( tokenizer_slashStarComments, "false");
        result.put ( tokenizer_wordChars, "65-122");
        
        return result;
    }
    
    /*
     * Constants naming the configuration properties used
     */
    private static final String tokenizer_eolIsSignificant = "tokenizer.eolIsSignificant";
    private static final String tokenizer_lowerCaseMode = "tokenizer.lowerCaseMode";
    private static final String tokenizer_ordinaryChar = "tokenizer.ordinaryChar";
    private static final String tokenizer_ordinaryChars = "tokenizer.ordinaryChars";
    private static final String tokenizer_parseNumbers = "tokenizer.parseNumbers";
    private static final String tokenizer_quoteChar = "tokenizer.quoteChar";
    private static final String tokenizer_slashSlashComments = "tokenizer.slashSlashComments";
    private static final String tokenizer_slashStarComments = "tokenizer.slashStarComments";
    private static final String tokenizer_whitespaceChars = "tokenizer.whitespaceChars";
    private static final String tokenizer_wordChar = "tokenizer.wordChar";
    private static final String tokenizer_wordChars = "tokenizer.wordChars";
}
