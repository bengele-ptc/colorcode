package lu.intrasoft.jspwiki.plugin;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;



/**
 *  ColorCodeHTML.java
 * 
 *  @author  roy  (mailto:phillips.roy@gmail.com)
 *  @version "%I%, %G%"
 *  @since   Sep 28, 2006
 *
 *  ColorCodeHTML transforms arbitary 'souce code' texts into syntax colored
 *  HTML code, suitable for inclusion in a webpage, Wiki plugin, etc.
 */

public class ColorCodeHTML
{
    private HashSet Keywords;
    private Properties syntax;
    private String syntax_name = "colorcode"; // default name used as div class name, if not set explicitly
    private String newline;
    private String space;
    private int tab_size;
    private int line_number = -1;
    Logger logger;
    
    /**
     * Given a text body to be formatted, and a set of parameters specifying
     * the syntax, return an HTML marked-up version of the text, suitable
     * for including in a web page
     * 
     * @param body the plain text to be formatted
     * @param params immediate syntax and formatting values, and/or
     *        the name of a resource from where values are to be read:
     *        <code>params.get("syntax")</code>
     *        
     * @return the HTML fragment
     */
    public String
    format ( String body, Map params)
    {
        if( body == null )
        {
            return "" ;
        }
        initialise ( params);
        String formatted = process_body ( body);
        
        return get_start() + formatted + get_finish();
    }



    /**
     * Load the properties that specify how to interpret the syntax of the text
     * to be processed, and how to render it in HTML
     * 
     * @param params -- name/value pairs for the syntax and formatting properties 
     * 
     */
    private void
    initialise (Map params)
    {
        Keywords = new HashSet();
        
        // first, load the hard-coded property defaults:
        syntax = default_properties();
        
        // get a logger if parameter "log='true'" is present:
        String do_logging = (String)params.get ( "log");
        if ( do_logging != null && do_logging.length() > 0)
        {
            if ( Boolean.valueOf(do_logging).booleanValue() == true)
            {
                logger = Logger.getLogger ( this.getClass());        
            }
        }
        
        // next, load properties defined by the syntax resource, if any:
        String syntax_name_property = (String)params.get ( "syntax");
        if ( syntax_name_property != null)
        {
            syntax_name = syntax_name_property;
            try
            {
                ResourceBundle props = ResourceBundle.getBundle ( syntax_name);
                Enumeration keys = props.getKeys();
                while ( keys.hasMoreElements())
                {
                    final String key = (String)keys.nextElement ();
                    final String value = props.getString ( key);
                    
                    syntax.put ( key, value);
                }
            }
            catch ( MissingResourceException mre)
            {
                log_error ( "Could not get resource: " + mre.getMessage());
            }
        }
        
        // save the keywords loaded from the resource for later...
        String kwords = syntax.getProperty ( keywords);
        // now set/reset properties from immediate parameters (may override resources)
        get_parameters ( params);
        // finally, initialise the keywords from resource properties
        set_keywords ( kwords);
        // ...and add any keywords set in the immediate parameters:
        set_keywords ( syntax.getProperty ( keywords));
        
        newline = syntax.getProperty ( newline_markup);
        space = syntax.getProperty ( space_markup);
        String tab_spaces_def = syntax.getProperty ( tabsize);
        tab_size = 4;
        if ( tab_spaces_def != null && tab_spaces_def.length() > 0)
        {
            tab_size =  Integer.valueOf(tab_spaces_def).intValue();
        }
        
        String line_number_def = syntax.getProperty ( number_lines);
        line_number = -1;
        if ( line_number_def != null && line_number_def.length() > 0)
        {
            line_number = Integer.valueOf(line_number_def).intValue();
        }
        
        load_specials ( syntax);
    }

    private HashMap specials;
    private boolean start_of_line = true;
    
    /**
     * Look for any definitions in the properties like:
     * <code>colorcode.special.tag.pattern</code>, which introduces
     * 'special' tokens
     * 
     * @param syntax2
     */
    private void
    load_specials ( Properties properties)
    {
        specials = new HashMap();
        Properties default_special_properties = new Properties();
        Iterator iter = properties.keySet().iterator();
        while ( iter.hasNext())
        {
            String key = ((String)iter.next()).toLowerCase();
            if ( key.startsWith ( "colorcode.special.") && key.endsWith ( ".pattern")) 
            {
                String name = key.substring ( 18, key.length()-8);
                String pattern = properties.getProperty ( key);    
                specials.put ( name, pattern);
                if ( properties.containsKey ( "colorcode.special." + name + ".font.color") == false)
                {
                    default_special_properties.put ( "colorcode.special." + name + ".font.color", default_special_font_color);
                }
                if ( properties.containsKey ( "colorcode.special." + name + ".font.bold") == false)
                {
                    default_special_properties.put ( "colorcode.special." + name + ".font.bold", default_special_font_bold);
                }
                if ( properties.containsKey ( "colorcode.special." + name + ".font.italic") == false)
                {
                    default_special_properties.put ( "colorcode.special." + name + ".font.italic", default_special_font_italic);
                }
            }
        }
        iter = default_special_properties.keySet().iterator();
        while ( iter.hasNext())
        {
            String key = (String)iter.next();
            String value = default_special_properties.getProperty ( key);
            properties.put ( key, value);
        }
        
    }



    /**
     * Copies all non-blank String parameters from the supplied
     * parameter Map into the internal Properties object 
     * (<code>syntax</code>) used in subsequesnt processing
     * 
     * @param params
     */
    private void
    get_parameters ( Map params)
    {
        Iterator keys = params.keySet ().iterator();
        while ( keys.hasNext())
        {
            String key = (String)keys.next();
            Object value = params.get ( key);
            if ( value != null && value instanceof String)
            {
                syntax.setProperty ( key, (String)value);
            }
        }
    }


    /**
     * Parse the supplied text, identifying the syntactic elements and
     * render then according to the configuration
     * 
     * @param body plain text to render
     * 
     * @returns HTML equivalent of the input body
     */
    private String
    process_body ( String body)
    {
        StringBuffer result = new StringBuffer ( "");        
        StreamTokenizer tokeniser = new SyntaxTokenizer ( new StringReader ( body.trim()), syntax);
        if ( line_number > -1)
        {
            result.append ( render_token ( "colorcode.number", ""+(line_number++)+space));
        }
        do
        {
            try
            {
                int token = tokeniser.nextToken();
                if ( token == StreamTokenizer.TT_EOF)
                {
                    break;
                }
                result.append ( process_token ( tokeniser, token));
            }
            catch ( IOException ex)
            {
                break;
            }
        }
        while ( true);
        
        return result.toString();
    }



    /**
     * Process an individual token from the input stream, identifying it
     * and rendering it approriately
     * 
     * @param tokeniser -- the tokenizer parsing the text
     * @param token -- the type or value of the currently-read token
     * 
     * @returns a StringBuffer holding the current tokens HTML representation
     */
    private StringBuffer
    process_token ( StreamTokenizer tokeniser, int token)
    {
        boolean is_line_start = start_of_line;
        start_of_line = false;
        StringBuffer result = new StringBuffer();
        String value = "";
        switch ( token)
        {
            case StreamTokenizer.TT_NUMBER:
                double number = tokeniser.nval;
                log_info ( "seen number: [" + number + "]");
                Double d = new Double ( number);
                value = ""+d.longValue();
/*              value = Double.toString ( number);
                if ( value.endsWith ( ".0"))
                {
                    value = value.substring ( 0, value.length()-2);
                }*/
                result.append ( render_token ( "colorcode.number", value));
                break;
                
            case StreamTokenizer.TT_WORD:
                if ( Boolean.valueOf(syntax.getProperty ( keyword_ignorecase)).booleanValue() == true)
                {
                    value = tokeniser.sval.toLowerCase();
                }
                else
                {
                    value = tokeniser.sval;
                }
                log_info ( "seen word: [" + value + "]");
                if ( Keywords.contains ( value)) // token is a keyword
                {
                    result.append ( render_token ( "colorcode.keyword", tokeniser.sval));
                }
                else
                {
                    value = tokeniser.sval;
                    if ( render_special ( value, is_line_start, result) == false)
                    {
                        result.append ( render_token ( "colorcode.normal", value));
                    }
                }
                break;
                
            case StreamTokenizer.TT_EOL:
                result.append ( newline + "\n" );
                if ( line_number > -1)
                {
                    result.append ( render_token ( "colorcode.number", ""+(line_number++)+space));
                }
                start_of_line = true;
                break;
                
            case '\'':
            case '\"':
                String quote = Character.toString ( (char)token);
                result.append ( render_token ( "colorcode.string", quote+tokeniser.sval+quote));
                break;
            case ' ':
                int space_count = 1;
                while ( token == ' ')
                {
                    try
                    {
                        token = tokeniser.nextToken ();
                    }
                    catch ( IOException ex)
                    {
                        tokeniser.pushBack();
                        break;
                    }
                    if ( token != ' ')
                    {
                        tokeniser.pushBack();
                        break;
                    }
                    space_count++;
                }
                
                result.append ( "<font color=\"" + syntax.getProperty ( area_bgcolor) + "\">"
                            + make_indent ( space_count) + syntax.getProperty ( font_color_end));
                break;
                
            case '\t':
                result.append ( "<font color=\"" + syntax.getProperty ( area_bgcolor) + "\">"
                        + make_indent ( tab_size) + syntax.getProperty ( font_color_end));
                break;
                
            default:
                result.append ( render_token ( "colorcode.symbol", Character.toString ( (char)token)));
        }
        return result;
    }
    

    /**
     * Is the value supplied a 'special' token, as defined by the 
     * configuration?
     * 
     * @param value of the current token
     * @param is_line_start 
     * @param output 
     * @return true is value is 'special'
     */
    private boolean
    render_special ( String value, boolean is_line_start, StringBuffer output)
    {
        if ( specials == null || specials.isEmpty ())
        {
            return false;
        }
        log_info ( "Is '" + value + "' special?" );
        Iterator iter = specials.keySet().iterator();
        while ( iter.hasNext())
        {
            String name = (String)iter.next();
            String pattern = (String)specials.get ( name);
            if ( pattern == null || pattern.length() == 0)
            {
                continue;
            }
            
            if ( value.matches ( pattern))
            {
                boolean start_anchor = pattern.charAt(0) == '^';
                if ( start_anchor == true && is_line_start == false)
                {
                    log_info ( "Matches, but not start-of-line (^ anchor present in pattern)");
                    continue;
                }
                output.append ( render_token ( "colorcode.special." + name, value));
                log_info ( "Matches " + name);
                return true;
            }
        }
        log_info ( "No match");
        return false;
    }

    /**
     * Load the set of words that are to be treated as keywords
     * 
     * @param kwords -- a comma-seperated list of words
     */
    private void
    set_keywords ( String kwords)
    {
        if ( kwords == null || kwords.length() == 0)
        {
            return;
        }
        String[] words = kwords.split ( ",");
        boolean ignore_case = Boolean.valueOf(syntax.getProperty ( keyword_ignorecase)).booleanValue();
        for ( int i = 0; words != null && i < words.length; i++)
        {
            String word = words[i];
            if ( ignore_case)
            {
                word = word.toLowerCase();
            }
            Keywords.add ( word);
        }
    }
    
    private void
    log_info ( String message)
    {
        if ( logger != null)
        {
            logger.info ( message);
        }
    }

    private void
    log_error ( String message)
    {
        if ( logger != null)
        {
            logger.error ( message);
        }
    }    


    /**
     * Create a set of 'reasonable' default configuration values, to
     * be used when no value provided by the caller
     * 
     * @returns a Properties set with the default proeprty values
     */
    private Properties
    default_properties()
    {      
        Properties result = new Properties();
        
        result.put ( area_class, default_area_class);
        result.put ( area_border, "1");
        result.put ( area_bgcolor, default_area_bgcolor);
        result.put ( start, DEFAULT_START);
        result.put ( finish, DEFAULT_FINISH);
        
        result.put ( string_font_color, default_string_font);
        result.put ( string_font_bold, "false");
        result.put ( string_font_italic, "false");
        
        result.put ( keyword_font_color, default_keyword_font);
        result.put ( keyword_font_bold, "true");
        result.put ( keyword_font_italic, "false");
        result.put ( keyword_foldcase, "no");
        result.put ( keyword_ignorecase, default_keyword_ignorecase);
        
        result.put ( symbol_font_color, default_symbol_font);
        result.put ( symbol_font_bold, "true");
        result.put ( symbol_font_italic, "false");
        
        result.put ( normal_font_color, default_normal_font);
        result.put ( normal_font_bold, "false");
        result.put ( normal_font_italic, "false");
        result.put ( normal_foldcase, "no");
        
        result.put ( number_font_color, default_number_font);
        result.put ( number_font_bold, "false");
        result.put ( number_font_italic, "false");
        
        result.put ( comment_font_color, default_comment_font);
        result.put ( comment_font_bold, "false");
        result.put ( comment_font_italic, "true");
        
        result.put ( font_color_end, default_font_off);
        result.put ( bold_font_on, default_font_bold_on);
        result.put ( bold_font_off, default_font_bold_off);
        result.put ( italic_font_on, default_font_italic_on);
        result.put ( italic_font_off, default_font_italic_off);
        result.put ( newline_markup, default_newline);
        result.put ( space_markup, default_space);
        result.put ( tabsize, default_tabsize);
        result.put ( number_lines, default_number_lines);

        return result;
    }


    private String[] indents = new String[100];
    /**
     * Utility to return an indentation string for the number of
     * spaces specified
     * 
     * @param level number of spaces to indent
     * @return HTML string representing <code>level</code> spaces
     */
    private String 
    make_indent ( int level)
    {
        String result = indents[level];
        if ( result == null)
        {
            result = "";
            for ( int i = 0; i < level; i++)
            {
                result += space;
            }
            indents[level] = result;
        }
        return result;
    }
    
    /**
     * Render a token of type <code>key</code> with the supplied <code>value/code>;
     * this method appends omto <code>key</code> to reference the appropriate confuration
     * properties for that token type.  For example, "colorcode.keyword" is 
     * extended to "colorcode.keyword.font.bold" to find the boolean value 
     * determining if a keyword should be rendered in bold text
     * 
     * @param key -- base identity of the token's class
     * @param value -- the token's value
     * @return
     */
    private String
    render_token ( String key, String value)
    {
        StringBuffer result = new StringBuffer ( "<font color=\"" + syntax.getProperty ( key + ".font.color") + "\">");
        String bold = syntax.getProperty (  key + ".font.bold");
        boolean bold_on = false;
        if ( bold != null && bold.length() > 0)
        {
            bold_on = Boolean.valueOf(bold).booleanValue();
        }
        String italic = syntax.getProperty (  key + ".font.italic");
        boolean italic_on = false;
        if ( italic != null && italic.length() > 0)
        {
            italic_on = Boolean.valueOf(italic).booleanValue();
        }
        if ( bold_on == true)
        {
            result.append ( syntax.getProperty ( bold_font_on));
        }
        if ( italic_on == true)
        {
            result.append ( syntax.getProperty ( italic_font_on));
        }
        
        String foldcase = syntax.getProperty (  key + ".foldcase");
        String out_value = value;
        if ( foldcase != null && foldcase.length() > 0)
        {
            if ( foldcase.equalsIgnoreCase  ( "upper"))
            {
               out_value = value.toUpperCase(); 
            }
            else if ( foldcase.equalsIgnoreCase  ( "lower"))
            {
               out_value = value.toLowerCase(); 
            }
        }
        if ( out_value.indexOf ( "<") >= 0)
        {
            out_value = out_value.replaceAll ( "<", "&lt;");
        }
        if ( out_value.indexOf ( ">") >= 0)
        {
            out_value = out_value.replaceAll ( ">", "&gt;");
        }
        result.append ( out_value);
        
        if ( italic_on == true)
        {
            result.append ( syntax.getProperty ( italic_font_off));
        }
        if ( bold_on == true)
        {
            result.append ( syntax.getProperty ( bold_font_off));
        }
        
        return result.toString() + syntax.getProperty ( font_color_end);
    }
    
    // HTML markup to precede the fomatted text
    private static final String DEFAULT_START = 
        "<!-- ColorCodeHTML, syntax=SYNTAX: START of automatically generated HTML code -->\n" +
        "<div align=\"left\" class=\"AREA_CLASS\">\n" +
        "  <table class=\"wikitable\" border=\"AREA_BORDER\" cellpadding=\"3\" cellspacing=\"0\" bgcolor=\"AREA_BGCOLOR\">\n" +
        "    <tr>\n" +
        "      <!-- start colorized source code -->\n" +
        "      <td nowrap=\"nowrap\" valign=\"top\" align=\"left\">\n" +
        "        <code style=\"white-space: pre; margin: 1.5em 2em 1.8em 2em; font-size: 1.2em; background: AREA_BGCOLOR;\">\n";

    // HTML markup to follow the fomatted text
    private static final String DEFAULT_FINISH = 
        "</code>\n" +
        "      </td>\n" +
        "      <!-- end colorized source code -->\n" +
        "    </tr>\n" +
        "  </table>\n" +
        "</div>\n" +
        "<!-- ColorCodeHTML: END of automatically generated HTML code -->\n";
    
    /**
     * Get the HTML text to be inserted before the formatted code, 
     * replacing the substitution values with ones from the current
     * configuration:
     * <ul>
     * <li>SYNTAX - the name of the syntax used for formatting</li>
     * <li>AREA_CLASS - the class-name to use in CSS processing</li>
     * <li>AREA_BGCOLOR - the background color of the fragment</li>
     * </ul>
     * @return HTML prefix string
     */
    private String
    get_start()
    {
        return header_footer_substitution ( syntax.getProperty ( start));
    }
    
    /**
     * Get the HTML text to follow the formatted code, 
     * replacing the substitution values with ones from the current
     * configuration
     * 
     * @return HTML prefix string
     */
    private String
    get_finish()
    {
        return header_footer_substitution ( syntax.getProperty  ( finish));
    }
    
    /**
     * Perform the text substitution of values in the prefix/postfix
     * markup blocks:
     * <ul>
     * <li>set SYNTAX from "syntax" property</li>
     * <li>set AREA_CLASS from "colorcode.area.class" property (or if not set, from "syntax")</li>
     * <li>set AREA_BGCOLOR from "colorcode.area.class" property</li>
     * </ul>

     * @param text
     * @return
     */
    private String
    header_footer_substitution ( String text)
    {
        if ( text == null || text.length() == 0)
        {
            return "";
        }
        String result = text;
        if ( result.indexOf ( "AREA_CLASS") > -1)
        {
            String aclass = syntax.getProperty ( area_class);
            if ( aclass == null || aclass.length() == 0)
            {
                aclass = syntax_name.toLowerCase();
            }
            if ( aclass != null && aclass.length() > 0)
            {
                result = result.replaceAll ( "AREA_CLASS", aclass);
            }
        }
        if ( result.indexOf ( "AREA_BGCOLOR") > -1)
        {
            String bgcolor = syntax.getProperty ( area_bgcolor);
            if ( bgcolor != null && bgcolor.length() > 0)
            {
                result = result.replaceAll ( "AREA_BGCOLOR", bgcolor);
            }
        }
        if ( result.indexOf ( "AREA_BORDER") > -1)
        {
            String border = syntax.getProperty ( area_border);
            if ( border != null && border.length() > 0)
            {
                result = result.replaceAll ( "AREA_BORDER", border);
            }
        }
        if ( result.indexOf ( "SYNTAX") > -1)
        {
            String bgcolor = syntax.getProperty ( area_bgcolor);
            if ( syntax_name != null && syntax_name.length() > 0)
            {
                result = result.replaceAll ( "SYNTAX", syntax_name);
            }
        }
        return result;
    }
    
    /*
     * Constants naming configuration properties used and their defaults
     */
    private static final String keywords = "colorcode.keywords";
    private static final String start = "colorcode.start";
    private static final String finish = "colorcode.finish";
    private static final String area_class = "colorcode.area.class";
    private static final String area_bgcolor = "colorcode.area.bgcolor";
    private static final String area_border = "colorcode.area.border";
    
    private static final String normal_font_color = "colorcode.normal.font.color";
    private static final String normal_font_bold = "colorcode.normal.font.bold";
    private static final String normal_font_italic = "colorcode.normal.font.italic";
    private static final String normal_foldcase = "colorcode.normal.foldcase";
    
    private static final String keyword_font_color = "colorcode.keyword.font.color";
    private static final String keyword_font_bold = "colorcode.keyword.font.bold";
    private static final String keyword_font_italic = "colorcode.keyword.font.italic";
    private static final String keyword_foldcase = "colorcode.keyword.foldcase";
    private static final String keyword_ignorecase = "colorcode.keyword.ignorecase";
    
    private static final String symbol_font_color = "colorcode.symbol.font.color";
    private static final String symbol_font_bold = "colorcode.symbol.font.bold";
    private static final String symbol_font_italic = "colorcode.symbol.font.italic";
    
    private static final String string_font_color = "colorcode.string.font.color";
    private static final String string_font_bold = "colorcode.string.font.bold";
    private static final String string_font_italic = "colorcode.string.font.italic";
    
    private static final String number_font_color = "colorcode.number.font.color";
    private static final String number_font_bold = "colorcode.number.font.bold";
    private static final String number_font_italic = "colorcode.number.font.italic";
    
    private static final String comment_font_color = "colorcode.comment.font.color";
    private static final String comment_font_bold = "colorcode.comment.font.bold";
    private static final String comment_font_italic = "colorcode.comment.font.italic";
        
    private static final String font_color_end = "colorcode.font.color.end";
    private static final String bold_font_on = "colorcode.font.bold.on";
    private static final String bold_font_off = "colorcode.font.bold.off";
    private static final String italic_font_on = "colorcode.font.italic.on";
    private static final String italic_font_off = "colorcode.font.italic.off";
    private static final String newline_markup = "colorcode.newline";
    private static final String space_markup = "colorcode.space";
    private static final String tabsize = "colorcode.tabsize";
    private static final String number_lines = "colorcode.line.number";
    
    private static final String default_area_bgcolor = "#f0f7f0";
    private static final String default_area_class = "body";
    private static final String default_string_font = "#2a00ff";
    private static final String default_keyword_font = "#7f0055";
    private static final String default_keyword_ignorecase = "true";
    private static final String default_symbol_font = "#000000";
    private static final String default_normal_font = "#000000";
    private static final String default_number_font = "#800000";
    private static final String default_comment_font = "#3f7f5f";
    private static final String default_special_font_color = "#8000ff";
    private static final String default_special_font_bold = "false";
    private static final String default_special_font_italic = "false";
    
    private static final String default_font_off = "</font>";
    private static final String default_font_bold_on = "<b>";
    private static final String default_font_bold_off = "</b>";
    private static final String default_font_italic_on = "<i>";
    private static final String default_font_italic_off = "</i>";
    private static final String default_newline = "<br/>";
    private static final String default_space = "&nbsp;";
    private static final String default_tabsize = "4";
    private static final String default_number_lines = "-1";
}
