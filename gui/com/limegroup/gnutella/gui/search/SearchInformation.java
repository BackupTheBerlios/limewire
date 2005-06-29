package com.limegroup.gnutella.gui.search;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

import com.limegroup.gnutella.MediaType;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.util.IpPort;

/**
 * Simple struct-like class containing information about a search.
 */
public class SearchInformation {
    
    /**
     * A keyword search.
     */
    public static final int KEYWORD = 0;
    
    /**
     * A what is new search.
     */
    public static final int WHATS_NEW = 1;
    
    /**
     * A browse host search.
     */
    public static final int BROWSE_HOST = 2;
    
    /**
     * The string to use to describe a what's new search.
     */
    public static final String WHATS_NEW_DESC =
        GUIMediator.getStringResource("SEARCH_WHATS_NEW_SMALL");
    
    /**
     * The kind of search this is.
     */
    private final int type;
    
    /**
     * The simple query string.
     */
    private final String query;
    
    /**
     * The XML string.
     */
    private final String xml;
    
    /**
     * The MediaType of the search.
     */
    private final MediaType media;

	/**
	 * The title of this search as it is displayed to the user. 
	 */
	private final String title;
    
    /**
     * Private constructor -- use factory methods instead.
     * @param title can be <code>null</code>, then the query is used.
     */
    private SearchInformation(int type, String query, String xml,
                              MediaType media, String title) {
        if(media == null)
            throw new NullPointerException("null media");
        if(query == null)
            throw new NullPointerException("null query");
        this.type = type;
        this.query = query.trim();
        this.xml = xml;
        this.media = media;
		this.title = title != null ? title : query; 
    }
	
	private SearchInformation(int type, String query, String xml,
			MediaType media) {
		this(type, query, xml, media, null);
	}
    
    /**
     * Creates a keyword search.
     */
    public static SearchInformation createKeywordSearch(String query,
                                                 String xml,
                                                 MediaType media) {
        return new SearchInformation(KEYWORD, query, xml, media);
    }
	
	/**
	 * Creates a keyword search with a title different from the query string.
	 * @param query
	 * @param xml
	 * @param media
	 * @param title
	 * @return
	 */
	public static SearchInformation createTitledKeyWordSearch(String query,
			String xml, MediaType media, String title) {
		return new SearchInformation(KEYWORD, query, xml, media, title);
	}
    
    /**
     * Creates a what's new search.
     */
    public static SearchInformation createWhatsNewSearch(String name, MediaType type){
        return new SearchInformation(WHATS_NEW, 
            WHATS_NEW_DESC + " - " + name, null, type);
    }
    
    /**
     * Create's a browse host search.
     */
    public static SearchInformation createBrowseHostSearch(String desc) {
        return new SearchInformation(BROWSE_HOST, desc, null, 
            MediaType.getAnyTypeMediaType());
    }
    
    /**
     * Retrieves the basic query of the search.
     */
    public String getQuery() {
        return query;
    }
    
    /**
     * Retrieves the XML portion of the search.
     */
    public String getXML() {
        return xml;
    }
    
    /**
     * Retrieves the MediaType of the search.
     */
    public MediaType getMediaType() {
        return media;
    }
    
	public String getTitle() {
		return title;
	}
	
    /**
     * Gets the IP/Port if this is a browse-host.
     */
    IpPort getIpPort() {
        if(!isBrowseHostSearch())
            throw new IllegalStateException();

        StringTokenizer st = new StringTokenizer(getQuery(), ":");
        String host = null;
        int port = 6346;
        if (st.hasMoreTokens())
            host = st.nextToken();
        if (st.hasMoreTokens()) {
            try {
                port = Integer.parseInt(st.nextToken());
            } catch(NumberFormatException ignored) {}
        }
        InetAddress addr = null;
        try {
            addr = InetAddress.getByName(host);
        } catch(UnknownHostException ignored) {}
        
        final String _host = host;
        final int _port = port;
        final InetAddress _addr = addr;
        return new IpPort() {
            public InetAddress getInetAddress() { return _addr; }
            public String getAddress() { return _host; }
            public int getPort() { return _port; }
        };
    }
    
    /**
     * Determines whether or not this is an XML search.
     */
    public boolean isXMLSearch() {
        return xml != null && !xml.equals("");
    }
    
    /**
     * Determines if this is a keyword search.
     */
    public boolean isKeywordSearch() {
        return type == KEYWORD;
    }
    
    /**
     * Determines if this is a what's new search.
     */
    public boolean isWhatsNewSearch() {
        return type == WHATS_NEW;
    }
    
    /**
     * Determines if this is a browse host search.
     */
    public boolean isBrowseHostSearch() {
        return type == BROWSE_HOST;
    }
}
