package lu.intrasoft.jspwiki.plugin;

import java.util.Map;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.plugin.PluginException;
import com.ecyrd.jspwiki.plugin.WikiPlugin;

/**
*  ColorCodePlugin.java
* 
*  @author  roy  (mailto:phillips.roy@gmail.com)
*  @version "%I%, %G%"
*  @since   Sep 28, 2006
*
*  ColorCodePlugin implements the <code>WikiPlugin</code> interface,
*  providing an <code>execute</code> implemenation that renders the
*  body text as colorized source code, according to either a named
*  <code>syntax</code> parameter, and/or parameter values provided at 
*  the plugin's point of invocation.
*  
*  Example:
*  <pre>
*  [{ColorCodePlugin syntax='sql'
*  
*  select count(*) from dual
*  }]
*  </pre>
*  This example will attempt to load a resource named 'sql' from the webapps
*  classpath, for example, a file WEB-INF/classes/sql.properties, and use the
*  properties in it to format the text
*/

public class ColorCodePlugin
    implements WikiPlugin
{
	/**
     * Implementation of WikiPlugin execute method for ColorCode rendering
     * 
	 * @see com.ecyrd.jspwiki.plugin.WikiPlugin#execute(com.ecyrd.jspwiki.WikiContext, java.util.Map)
	 */
	public String
    execute ( WikiContext wiki_context, Map params)
		throws PluginException
	{
        String body = (String)params.get( "_body" );
        if( body == null )
        {
            return "";
        }
        
        ColorCodeHTML formatter = new ColorCodeHTML();
        return formatter.format ( body, params);
	}
}