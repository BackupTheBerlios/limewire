package com.limegroup.gnutella.gui.xml.editor;

import java.io.File;
import java.util.HashMap;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.xml.LimeXMLUtils;

/**
 * 
 */
public final class MetaEditorUtil {
   
    public static final String AUDIO_SCHEMA = "http://www.limewire.com/schemas/audio.xsd";
    
    public static final String AUDIO = "audios__audio";
    public static final String AUDIO_TITLE = "audios__audio__title__";
    public static final String AUDIO_TRACK = "audios__audio__track__";
    public static final String AUDIO_ARTIST = "audios__audio__artist__";
    public static final String AUDIO_ALBUM = "audios__audio__album__";
    public static final String AUDIO_GENRE = "audios__audio__genre__";
    public static final String AUDIO_COMMENTS = "audios__audio__comments__";
    public static final String AUDIO_YEAR = "audios__audio__year__";
    public static final String AUDIO_TYPE = "audios__audio__type__";
    public static final String AUDIO_LANGUAGE = "audios__audio__language__";
    public static final String AUDIO_SECONDS = "audios__audio__seconds__";
    public static final String AUDIO_SHA1 = "audios__audio__SHA1__";
    public static final String AUDIO_BITRATE = "audios__audio__bitrate__";
    public static final String AUDIO_PRICE = "audios__audio__price__";
    public static final String AUDIO_LINK = "audios__audio__link__";
    public static final String AUDIO_ACTION = "audios__audio__action__";
    public static final String AUDIO_LICENSE = "audios__audio__license__";
    
    
    private static final HashMap XSD_MESSAGEBUNDLE_BRIDGE = new HashMap();
    
    static {
        XSD_MESSAGEBUNDLE_BRIDGE.put(AUDIO, "META_EDITOR_AUDIO_LABEL");
        XSD_MESSAGEBUNDLE_BRIDGE.put(AUDIO_TITLE, "META_EDITOR_TITLE_LABEL");
        XSD_MESSAGEBUNDLE_BRIDGE.put(AUDIO_ARTIST, "META_EDITOR_ARTIST_LABEL");
        XSD_MESSAGEBUNDLE_BRIDGE.put(AUDIO_ALBUM, "META_EDITOR_ALBUM_LABEL");
        XSD_MESSAGEBUNDLE_BRIDGE.put(AUDIO_GENRE, "META_EDITOR_GENRE_LABEL");
        XSD_MESSAGEBUNDLE_BRIDGE.put(AUDIO_YEAR, "META_EDITOR_YEAR_LABEL");
        XSD_MESSAGEBUNDLE_BRIDGE.put(AUDIO_TYPE, "META_EDITOR_TYPE_LABEL");
        XSD_MESSAGEBUNDLE_BRIDGE.put(AUDIO_TRACK, "META_EDITOR_TRACK_LABEL");
        XSD_MESSAGEBUNDLE_BRIDGE.put(AUDIO_LANGUAGE, "META_EDITOR_LANGUAGE_LABEL");
        XSD_MESSAGEBUNDLE_BRIDGE.put(AUDIO_SECONDS, "META_EDITOR_SECONDS_LABEL");
        XSD_MESSAGEBUNDLE_BRIDGE.put(AUDIO_BITRATE, "META_EDITOR_BITRATE_LABEL");
        XSD_MESSAGEBUNDLE_BRIDGE.put(AUDIO_COMMENTS, "META_EDITOR_COMMENTS_LABEL");
        XSD_MESSAGEBUNDLE_BRIDGE.put(AUDIO_SHA1, "META_EDITOR_SHA1_LABEL");
        XSD_MESSAGEBUNDLE_BRIDGE.put(AUDIO_PRICE, "META_EDITOR_PRICE_LABEL");
        XSD_MESSAGEBUNDLE_BRIDGE.put(AUDIO_LINK, "META_EDITOR_LINK_LABEL");
        XSD_MESSAGEBUNDLE_BRIDGE.put(AUDIO_ACTION, "META_EDITOR_ACTION_LABEL");
        XSD_MESSAGEBUNDLE_BRIDGE.put(AUDIO_LICENSE, "META_EDITOR_LICENSE_LABEL");
    }
    
    public static boolean contains(String resource) {
        return XSD_MESSAGEBUNDLE_BRIDGE.containsKey(resource);
    }
    
    /**
     * 
     */
    public static String getStringResource(String resourceKey) {
        String rscKey = (String)XSD_MESSAGEBUNDLE_BRIDGE.get(resourceKey);
        Assert.that(rscKey != null, "Unknown resourceKey: " + resourceKey);
        return GUIMediator.getStringResource(rscKey);
    }
    
    /**
     * 
     */
    public static String getKind(File file) {
        String name = file.getName();
        
        if (LimeXMLUtils.isMP3File(name)) {
            return GUIMediator.getStringResource("META_EDITOR_MP3_KIND_LABEL");
        } else if (LimeXMLUtils.isM4AFile(name)) {
            return GUIMediator.getStringResource("META_EDITOR_MP4_KIND_LABEL");
        } else if (LimeXMLUtils.isOGGFile(name)) {
            return GUIMediator.getStringResource("META_EDITOR_OGG_KIND_LABEL");
        } else {
            return null;
        }
    }
    
    private MetaEditorUtil() {
    }
}
