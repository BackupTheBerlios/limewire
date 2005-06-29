package com.limegroup.gnutella.i18n;


import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author Admin
 */
class LanguageLoader {
    
    /** @see LanguageInfo#getLink() */
    static final String BUNDLE_NAME = "MessagesBundle";
    /** @see LanguageInfo#getLink() */
    static final String PROPS_EXT = ".properties";
    /** @see LanguageInfo#getLink() */
    static final String UTF8_EXT = ".UTF-8.txt";    
    
    private final Map/*<String code, LanguageInfo li>*/ langs;
    private final File lib;
    
    /**
     * @param directory
     */
    LanguageLoader(File directory) {
        langs = new TreeMap();
        lib = directory;
    }
    
    /**
     * List and load all available bundles and map them into the languages map.
     * Note that resources are not expanded here per base language, and not cleaned
     * here from extra keys (needed to support the resources "check" option).
     * @return the languages map (from complete locale codes to LocaleInfo)
     */
    Map loadLanguages() {
        if (!lib.isDirectory())
            throw new IllegalArgumentException("invalid lib: " + lib);
        
        String[] files = lib.list();
        for (int i = 0; i < files.length; i++) {
            if (!files[i].startsWith(BUNDLE_NAME + "_") ||
                !files[i].endsWith(PROPS_EXT) ||
                 files[i].startsWith(BUNDLE_NAME + "_en"))
                continue;
            
            /* See if a .UTF-8.txt file exists; if so, use that as the link. */
            String linkFileName = files[i];
            int idxProperties = linkFileName.indexOf(PROPS_EXT);
            File utf8 = new File(lib, linkFileName.substring(0, idxProperties) + UTF8_EXT);
            boolean skipUTF8LeadingBOM;
            if (utf8.exists()) {
                /* properties files are normally read as streams of ISO-8859-1 bytes
                but we want to check the UTF-8 source file. The non ASCII characters
                in key values will be read as sequences of Extended Latin 1 characters
                instead of the actual Unicode character coded as Unicode escapes
                in the .properties file. So they won't have the actual run-time value;
                however it allows easier checking and validation here for messages
                printed on the Console, that will output ISO-8859-1; the result output
                still be interpretable as Unicode UTF-8. */
                linkFileName = utf8.getName();
                skipUTF8LeadingBOM = true;
            } else
                skipUTF8LeadingBOM = false;
            
            try {
                InputStream in =
                    new FileInputStream(new File(lib, linkFileName/*files[i]*/));
                if (skipUTF8LeadingBOM) try { /* skip the three-bytes leading BOM */
                   /* the leading BOM (U+FEFF), if present, is coded in UTF-8 as three
                    * bytes 0xEF, 0xBB, 0xBF; they are not part of a resource key. */
                   in.mark(3);
                   if (in.read() != 0xEF || in.read() != 0xBB || in.read() != 0xBF)
                       in.reset();
                } catch (java.io.IOException ioe) {
                }
                loadFile(langs, in, linkFileName, files[i], skipUTF8LeadingBOM);
            } catch (FileNotFoundException fnfe) {
                // oh well.
            }
        }
        return langs;
    }
    
    /**
     * Constructs a list of each line in the default English properties file.
     * @return a list of Line instances
     * @throws IOException
     * @see Line
     */
    List /* of Line */ getEnglishLines() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
            new FileInputStream(new File(lib, BUNDLE_NAME + PROPS_EXT)),
            "ISO-8859-1"));
            
        List lines = new LinkedList();
        String read;
        while( (read = reader.readLine()) != null)
            lines.add(new Line(read));
        
        return lines;
    }
        
    
    /**
     * Retrieves the default properties.
     * @return the loaded Properties
     * @throws IOException
     */
    Properties getDefaultProperties() throws java.io.IOException {
        Properties p = new Properties();
        InputStream in = new FileInputStream(new File(lib, BUNDLE_NAME + PROPS_EXT));
        p.load(in);
        in.close();
        return p;
    }
    
    /**
     * Retrieves the advanced keys.
     * @return a the Set of Strings for the key names of advanced properties. 
     * @throws IOException
     */
    Set getAdvancedKeys() throws java.io.IOException  {
        BufferedReader reader;
        reader = new BufferedReader(new InputStreamReader(
            new FileInputStream(new File(lib, BUNDLE_NAME + PROPS_EXT)),
            "ISO-8859-1"));
            
        String read;
        while ((read = reader.readLine()) != null &&
               !read.startsWith("## TRANSLATION OF ALL ADVANCED RESOURCE STRINGS AFTER THIS LIMIT IS OPTIONAL"));
        
        StringBuffer sb = new StringBuffer();
        while ((read = reader.readLine()) != null) {
            if (read.length() == 0 ||
                read.charAt(0) == '#')
            continue;
            sb.append(read).append("\n");
        }
        InputStream in = new ByteArrayInputStream(sb.toString().getBytes("ISO-8859-1"));
        Properties p = new Properties();
        p.load(in);
        
        in.close();
        reader.close();
        return p.keySet();
    }    

    /**
     * Extend variant resources from *already loaded* base languages.
     */
    void extendVariantLanguages() {
        /* Extends missing resources with those from the base language */
        for (Iterator i = langs.entrySet().iterator(); i.hasNext(); ) {
            final Map.Entry entry = (Map.Entry)i.next();
            //final String code = (String)entry.getKey();
            final LanguageInfo li = (LanguageInfo)entry.getValue();
            final Properties props = li.getProperties();
            if (li.isVariant()) {
                final LanguageInfo liBase = (LanguageInfo)langs.get(li.getBaseCode());
                if (liBase != null) {
                    /* Get a copy of base properties */
                    final Properties propsBase = new Properties();
                    propsBase.putAll(liBase.getProperties());
                    /* Remove properties already defined in the current locale */
                    propsBase.keySet().removeAll(props.keySet());
                    /* Add the remaining base properties to the current locale */
                    props.putAll(propsBase);
                }
            }
        }
    }
    
    /**
     * Iterates through all languages and retains only those within 'keys'.
     * @param keys a Set of String for key names to retain in properties.
     */
    void retainKeys(Set keys) {
        /* Extends missing resources with those from the base language */
        for (Iterator i = langs.entrySet().iterator(); i.hasNext(); ) {
            final Map.Entry entry = (Map.Entry)i.next();
            //final String code = (String)entry.getKey();
            final LanguageInfo li = (LanguageInfo)entry.getValue();
            final Properties props = li.getProperties();
            props.keySet().retainAll(keys);
        }
    }
    
    /**
     * Loads a single file into the languages map.
     */
    private LanguageInfo loadFile(Map newlangs, InputStream in, String filename, 
                                  String baseFileName, boolean isUTF8) {
        try {
            in = new BufferedInputStream(in);
            final Properties props = new Properties();
            props.load(in);
            /* note that the file is read in ISO-8859-1 only, even if it is encoded
             * with another charset. However, the Properties has its unique legacy parser
             * and we want to use it to make sure we use the same syntax. So we'll need to
             * correct the parsed values after the file is read and interpreted as a set of
             * properties (keys,values).
             */
            
            if (isUTF8) {
                // actually the file was UTF-8-encoded: convert bytes read incorrectly as
                // ISO-8859-1 characters, into actual Unicode UTF-16 code units.
                for (Iterator i = props.entrySet().iterator(); i.hasNext(); ) {
                    final Map.Entry entry = (Map.Entry)i.next();
                    final String key = (String)entry.getKey();
                    final String value = (String)entry.getValue();
                    byte[] bytes = null;
                    try {
                        bytes = value.getBytes("ISO-8859-1");
                    } catch (java.io.IOException ioe) {
                        //should not occur
                    }
                    try {
                        final String correctedValue = new String(bytes, "UTF-8");
                        if (!correctedValue.equals(value))
                            props.put(key, correctedValue);
                    } catch (java.io.IOException ioe) {
                        System.err.println(ioe); //should not occur
                    }
                }
            }
            String lc = props.getProperty("LOCALE_LANGUAGE_CODE", "");
            String cc = props.getProperty("LOCALE_COUNTRY_CODE", "");
            String vc = props.getProperty("LOCALE_VARIANT_CODE", "");
            String sc = props.getProperty("LOCALE_SCRIPT_CODE", "");
            String ln = props.getProperty("LOCALE_LANGUAGE_NAME", lc);
            String cn = props.getProperty("LOCALE_COUNTRY_NAME", cc);
            String vn = props.getProperty("LOCALE_VARIANT_NAME", vc);
            String sn = props.getProperty("LOCALE_SCRIPT_NAME", sc);
            String dn = props.getProperty("LOCALE_ENGLISH_LANGUAGE_NAME", ln);
            String nsisName = props.getProperty("LOCALE_NSIS_NAME", "");
            boolean rtl = props.getProperty("LAYOUT_RIGHT_TO_LEFT", "false").equals("true");
            
            LanguageInfo li = new LanguageInfo(lc, cc, vc, sc,
                                               ln, cn, vn, sn,
                                               dn, nsisName,
                                               rtl, filename, props,
                                               baseFileName);
            newlangs.put(li.getCode(), li);
            return li;
        } catch (IOException e) {
            // ignore.
        } finally {
            if (in != null)
                try {
                   in.close();
                } catch (IOException ioe) {}
        }
        return null;
    }
}
