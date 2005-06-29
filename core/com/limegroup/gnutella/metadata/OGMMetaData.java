package com.limegroup.gnutella.metadata;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.jcraft.jorbis.Comment;
import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.util.IOUtils;

public class OGMMetaData extends VideoMetaData {

	public static final String TITLE_TAG = "title";

	public static final String COMMENT_TAG = "comment";

	public static final String LICENSE_TAG = "license";

	private static final String DATE_TAG = "date";

	private static final String LANGUAGE_TAG = "language";

	public OGMMetaData(File f) throws IOException {
		super(f);
	}

	protected void parseFile(File file) throws IOException {
		InputStream is = null;
		try {
			is = new FileInputStream(file);
			DataInputStream dis = new DataInputStream(is);
			Set set = readMetaData(dis);
			parseMetaData(set);
		} finally {
			IOUtils.close(is);
		}
	}

	/**
	 * reads the first pages of the Ogg container, extracts all Vorbis comments
	 * 
	 * @param dis
	 *            a DataInputStream
	 * @return Set of String containing Vorbis comments
	 * @throws IOException
	 */
	private Set readMetaData(DataInputStream dis) throws IOException {
		Set set = new HashSet();
		boolean shouldStop = false;
		do {
			int pageSize = readHeader(dis);
			shouldStop = parseCommentBlock(pageSize, dis, set);
		} while (!shouldStop);
		return set;
	}

	/**
	 * Reads the header of an Ogg page
	 * 
	 * @param dis
	 *            the DataInputStream to read from
	 * @return size of the rest of the page.
	 * @throws IOException
	 */
	private int readHeader(DataInputStream dis) throws IOException {
		// read pageHeader
		if (dis.readByte() != 'O')
			throw new IOException("not an ogg file");
		if (dis.readByte() != 'g')
			throw new IOException("not an ogg file");
		if (dis.readByte() != 'g')
			throw new IOException("not an ogg file");
		if (dis.readByte() != 'S')
			throw new IOException("not an ogg file");

		// boring data
		IOUtils.ensureSkip(dis, 22);

		// number of page segments
		int segments = dis.readUnsignedByte();
		int size = 0;
		for (int i = 0; i < segments; i++) {
			size += dis.readUnsignedByte();
		}

		return size;
	}

	/*
	 * parse what we hope is a comment block. If that's not the case, we mostly
	 * skip the data.
	 */
	private boolean parseCommentBlock(int pageSize, DataInputStream dis,
			Set comments) throws IOException {
		int type = dis.readByte();
		pageSize--;

		if ((type & 1) != 1) {
			// we are reading a data block, stop.
			IOUtils.ensureSkip(dis, pageSize);
			return true;
		} else if (type != 3) {
			IOUtils.ensureSkip(dis, pageSize);
			// reading some header block
			return false;
		}

		byte[] vorbis = new byte[6];
		dis.readFully(vorbis);
		pageSize -= 6;

		if (vorbis[0] != 'v' || vorbis[1] != 'o' || vorbis[2] != 'r'
				|| vorbis[3] != 'b' || vorbis[4] != 'i' || vorbis[5] != 's') {
			// not a vorbis comment
			IOUtils.ensureSkip(dis, pageSize);
			return true;
		}

		// read size of vendor string
		byte[] dword = new byte[4];
		dis.readFully(dword);
		int vendorStringSize = ByteOrder.leb2int(dword, 0);

		// read vendor string
		byte[] vendorString = new byte[vendorStringSize];
		dis.readFully(vendorString);

		// read number of comments
		dis.readFully(dword);
		int numComments = ByteOrder.leb2int(dword, 0);

		// read comments
		for (int i = 0; i < numComments; i++) {
			dis.readFully(dword);
			int commentSize = ByteOrder.leb2int(dword, 0);
			byte[] comment = new byte[commentSize];
			dis.readFully(comment);
			comments.add(new String(comment, "UTF-8"));
		}
		// last bit marker missing -> error
		if ((dis.readByte() & 1) != 1)
			return true;
		return false;
	}

	/**
	 * extracts usable information from a Set of Vorbis comments
	 * 
	 * @param data
	 *            a Set of String containing Vorbis comments
	 */
	private void parseMetaData(Set data) {
		for (Iterator iter = data.iterator(); iter.hasNext();) {
			String comment = iter.next().toString();
			int index = comment.indexOf('=');
			if (index <= 0)
				continue;
			String key = comment.substring(0, index);
			String value = comment.substring(index + 1);

			if (key.equalsIgnoreCase(COMMENT_TAG)) {
				if(getComment() != null)
				    setComment(getComment() + "\n" + value);
                else
                    setComment(value);
			} else if (key.equalsIgnoreCase(LANGUAGE_TAG)) {
			    if(getLanguage() != null)
			        setLanguage(getLanguage() + ";" + value);
			    else
			        setLanguage(value);
			} else if (key.equalsIgnoreCase(LICENSE_TAG)) {
			    setLicense(value);
			} else if (key.equalsIgnoreCase(TITLE_TAG)) {
			    setTitle(value);
			} else if (key.equalsIgnoreCase(DATE_TAG)) {
			    setYear(value);
			}
		}
	}
}
