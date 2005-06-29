package com.limegroup.gnutella.gui.library;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.MouseInputListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileDetails;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.FileManagerEvent;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.SaveLocationException;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.CantResumeException;
import com.limegroup.gnutella.gui.ButtonRow;
import com.limegroup.gnutella.gui.FileDetailsProvider;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.LicenseWindow;
import com.limegroup.gnutella.gui.MessageService;
import com.limegroup.gnutella.gui.actions.ActionUtils;
import com.limegroup.gnutella.gui.actions.BitziLookupAction;
import com.limegroup.gnutella.gui.actions.CopyMagnetLinkToClipboardAction;
import com.limegroup.gnutella.gui.actions.SearchAction;
import com.limegroup.gnutella.gui.playlist.PlaylistMediator;
import com.limegroup.gnutella.gui.tables.AbstractTableMediator;
import com.limegroup.gnutella.gui.tables.DataLine;
import com.limegroup.gnutella.gui.tables.DragManager;
import com.limegroup.gnutella.gui.tables.LimeJTable;
import com.limegroup.gnutella.gui.themes.ThemeMediator;
import com.limegroup.gnutella.gui.util.CoreExceptionHandler;
import com.limegroup.gnutella.gui.xml.MetaEditorFrame;
import com.limegroup.gnutella.gui.xml.editor.MetaEditor;
import com.limegroup.gnutella.licenses.License;
import com.limegroup.gnutella.licenses.VerificationListener;
import com.limegroup.gnutella.settings.QuestionsHandler;
import com.limegroup.gnutella.util.Launcher;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.StringUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLUtils;

/**
 * This class wraps the JTable that displays files in the library,
 * controlling access to the table and the various table properties.
 * It is the Mediator to the Table part of the Library display.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
final class LibraryTableMediator extends AbstractTableMediator
	implements VerificationListener, FileDetailsProvider {

    private static final Log LOG = LogFactory.getLog(LibraryTableMediator.class);
	
	/**
     * Variables so the PopupMenu & ButtonRow can have the same listeners
     */
    public static ActionListener LAUNCH_LISTENER;
    public static ActionListener ADD_PLAY_LIST_LISTENER;
    public static Action ANNOTATE_LISTENER;
    private Action BITZI_LOOKUP_ACTION;
    private Action MAGNET_LOOKUP_ACTION;
    public static ActionListener RESUME_LISTENER;
    private Action LICENSE_ACTION;
    public static ActionListener RENAME_LISTENER;
	private Action COPY_MAGNET_TO_CLIPBOARD_ACTION;
	public static Action SHARE_ACTION;
	public static Action UNSHARE_ACTION;
	public static Action SHARE_FOLDER_ACTION;
	public static Action UNSHARE_FOLDER_ACTION;

    /**
     * Whether or not the incomplete directory is selected.
     */
    private boolean _isIncomplete;

	/**
	 * Annotation can be turned on once XML is set up.
	 */
	private boolean _annotateEnabled = false;
	
    /**
     * instance, for singelton access
     */
    private static LibraryTableMediator _instance = new LibraryTableMediator();

    public static LibraryTableMediator instance() { return _instance; }

    /**
     * Build some extra listeners
     */
    protected void buildListeners() {
        super.buildListeners();
        LAUNCH_LISTENER = new LaunchListener();
        ADD_PLAY_LIST_LISTENER = new AddPLFileListener();
        ANNOTATE_LISTENER = new AnnotateAction();
        BITZI_LOOKUP_ACTION = new BitziLookupAction(this);
        MAGNET_LOOKUP_ACTION = new MagnetLookupAction();
        RESUME_LISTENER = new ResumeListener();
        LICENSE_ACTION = new LicenseAction();
        RENAME_LISTENER = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                startRename();
            }
        };
		COPY_MAGNET_TO_CLIPBOARD_ACTION = new CopyMagnetLinkToClipboardAction(this);
		SHARE_ACTION = new ShareFileAction();
		UNSHARE_ACTION = new UnshareFileAction();
		SHARE_FOLDER_ACTION = new ShareFolderAction();
		UNSHARE_FOLDER_ACTION = new UnshareFolderAction();
    }

    /**
     * Set up the constants
     */
    protected void setupConstants() {
		MAIN_PANEL = null;
		DATA_MODEL = new LibraryTableModel();
		TABLE = new LimeJTable(DATA_MODEL);
		((LibraryTableModel)DATA_MODEL).setTable(TABLE);
		BUTTON_ROW = (new LibraryTableButtons(this)).getComponent();
    }

    // inherit doc comment
    protected JPopupMenu createPopupMenu() {
        JPopupMenu menu = (new LibraryTablePopupMenu(this)).getComponent();
        
		if (TABLE.getSelectionModel().isSelectionEmpty())
			return null;
        
		DataLine[] dls = TABLE.getSelectedDataLines();
		LICENSE_ACTION.setEnabled(dls != null && dls[0] !=null && ((LibraryTableDataLine)dls[0]).isLicensed());

        menu.getComponent(LibraryTablePopupMenu.RENAME_INDEX).setEnabled(!_isIncomplete);
        menu.getComponent(LibraryTablePopupMenu.RESUME_INDEX).setEnabled(_isIncomplete);
        menu.getComponent(LibraryTablePopupMenu.LAUNCH_INDEX).setEnabled(true);
		menu.addSeparator();
		
		boolean dirSelected = false;
		boolean fileSelected = false;
		for (int i = 0; i < dls.length; i++) {
			if (((LibraryTableDataLine)dls[i]).getFile().isDirectory())
				dirSelected = true;
			else
				fileSelected = true;
			
			if (dirSelected && fileSelected)
				break;
		}
		if (dirSelected) {
	        if (GUIMediator.isPlaylistVisible())
	            menu.getComponent(LibraryTablePopupMenu.PLAYLIST_INDEX).setEnabled(false);
	        menu.getComponent(LibraryTablePopupMenu.DELETE_INDEX).setEnabled(false);
	        menu.getComponent(LibraryTablePopupMenu.RENAME_INDEX).setEnabled(false);
			if (fileSelected) {
				JMenu sharingMenu = new JMenu(GUIMediator.getStringResource("LIBRARY_TABLE_SHARING_SUB_MENU"));
				sharingMenu.add(new JMenuItem(SHARE_ACTION));
				sharingMenu.add(new JMenuItem(UNSHARE_ACTION));
				sharingMenu.addSeparator();
				sharingMenu.add(new JMenuItem(SHARE_FOLDER_ACTION));
				sharingMenu.add(new JMenuItem(UNSHARE_FOLDER_ACTION));
				menu.add(sharingMenu);
			} else { 
				menu.add(new JMenuItem(SHARE_FOLDER_ACTION));
				menu.add(new JMenuItem(UNSHARE_FOLDER_ACTION));
			}
		} else {
	        if (GUIMediator.isPlaylistVisible())
	            menu.getComponent(LibraryTablePopupMenu.PLAYLIST_INDEX).setEnabled(true);
	        menu.getComponent(LibraryTablePopupMenu.DELETE_INDEX).setEnabled(true);
	        menu.getComponent(LibraryTablePopupMenu.RENAME_INDEX).setEnabled(!_isIncomplete);
			menu.add(new JMenuItem(SHARE_ACTION));
			menu.add(new JMenuItem(UNSHARE_ACTION));
		}
		menu.addSeparator();
		menu.add(createSearchSubMenu((LibraryTableDataLine)dls[0]));
		menu.add(createAdvancedMenu((LibraryTableDataLine)dls[0]));

		return menu;
    }

	private JMenu createAdvancedMenu(LibraryTableDataLine dl) {
		JMenu menu = new JMenu(GUIMediator.getStringResource("GENERAL_ADVANCED_SUB_MENU"));
		if (dl != null) {
			menu.add(new JMenuItem(LICENSE_ACTION));
			menu.add(new JMenuItem(BITZI_LOOKUP_ACTION));
			menu.add(new JMenuItem(MAGNET_LOOKUP_ACTION));
			menu.add(new JMenuItem(COPY_MAGNET_TO_CLIPBOARD_ACTION));
			File file = getFile(TABLE.getSelectedRow());
			menu.setEnabled(RouterService.getFileManager().isFileShared(file)); 
		}
		
        if (menu.getItemCount() == 0)
            menu.setEnabled(false);

		return menu;
	}
	
    private JMenu createSearchSubMenu(LibraryTableDataLine dl) {
		JMenu menu = new JMenu(GUIMediator.getStringResource("LIBRARY_TABLE_SEARCH_POPUP_MENU"));
        
        if(dl != null) {
            File f = (File)dl.getInitializeObject();
    		String keywords = StringUtils.createQueryString(f.getName());
            if (keywords.length() > 2)
    			menu.add(new JMenuItem(new SearchAction(keywords)));
    		
    		LimeXMLDocument doc = dl.getXMLDocument();
    		if(doc != null) {
                Action[] actions = ActionUtils.createSearchActions(doc);
        		for (int i = 0; i < actions.length; i++)
        			menu.add(new JMenuItem(actions[i]));
            }
        }
        
        if(menu.getItemCount() == 0)
            menu.setEnabled(false);
            
        return menu;
	}

	/**
     * Upgrade getScrolledTablePane to public access.
     */
    public JComponent getScrolledTablePane() {
        return super.getScrolledTablePane();
    }

    /* Don't display anything for this.  The LibraryMediator will do it. */
	protected void updateSplashScreen() {}

    /**
     * Note: This is set up for this to work.
     * Polling is not needed though, because updates
     * already generate update events.
     */
    private LibraryTableMediator() {
        super("LIBRARY_TABLE");
        //GUIMediator.addRefreshListener(this);
        ThemeMediator.addThemeObserver(this);
    }
    
    /**
     * Sets up drag & drop for the table.
     */
    protected void setupDragAndDrop() {
        DragManager.install(TABLE);
    }

	/**
	 * there is no actual component that holds all of this table.
	 * The LibraryMediator is real the holder.
	 */
	public JComponent getComponent() {
		return null;
	}
	
    /**
     * Sets the default editors.
     */
    protected void setDefaultEditors() {
        TableColumnModel model = TABLE.getColumnModel();
        TableColumn tc = model.getColumn(LibraryTableDataLine.NAME_IDX);
        tc.setCellEditor(new LibraryTableCellEditor(this));
    }


	/**
	 * Cancels all editing of fields in the tree and table.
	 */
	void cancelEditing() {
		if(TABLE.isEditing()) {
			TableCellEditor editor = TABLE.getCellEditor();
			editor.cancelCellEditing();
		}	    
	}

	/**
	 * Adds the mouse listeners to the wrapped <tt>JTable</tt>.
	 *
	 * @param listener the <tt>MouseInputListener</tt> that handles mouse events
	 *                 for the library
	 */
	void addMouseInputListener(final MouseInputListener listener) {
        TABLE.addMouseListener(listener);
        TABLE.addMouseMotionListener(listener);
	}

	/**
	 * Allows annotation once XML is set up
	 *
	 * @param enabled whether or not annotation is allowed
	 */
	public void setAnnotateEnabled(boolean enabled) {
		_annotateEnabled = enabled;
		
	    LibraryTableDataLine.setXMLEnabled(enabled);
	    DATA_MODEL.refresh();
		
	    //  disable the annotate buttons if we are turning annotation off
	    if (!enabled) {
		    setButtonEnabled(LibraryTableButtons.ANNOTATE_BUTTON, false);
			ANNOTATE_LISTENER.setEnabled(false);
	    } else if (!_isIncomplete && TABLE.getSelectedRowCount() == 1
				&& ((LibraryTableModel)DATA_MODEL).getFileDesc(TABLE.getSelectedRow()) != null) {
			//  if one non-incomplete item is selected, enable the annotate button
			ANNOTATE_LISTENER.setEnabled(true);
            setButtonEnabled(LibraryTableButtons.ANNOTATE_BUTTON, true);
        }        
	}

	/**
	 * Notification that the incomplete directory is selected (or not)
	 *
	 * @param enabled whether or not incomplete is showing
	 */
	void setIncompleteSelected(boolean enabled) {
		if (enabled == _isIncomplete)
			return;
	    _isIncomplete = enabled;
	    //  enable/disable the resume buttons if we're not incomplete
	    if (!enabled) {
	        setButtonEnabled(LibraryTableButtons.RESUME_BUTTON, false);
	    } else if (!TABLE.getSelectionModel().isSelectionEmpty()) {
	        setButtonEnabled(LibraryTableButtons.RESUME_BUTTON, true);
	    }
	}
	
	/**
	 * Updates the Table based on the selection of the given table.
	 */
    void updateTableFiles(DirectoryHolder dirHolder) {
		if (dirHolder == null)
			return;
		clearTable();
		setIncompleteSelected(LibraryMediator.incompleteDirectoryIsSelected());
		File[] files = dirHolder.getFiles();
		for (int i = 0; i < files.length; i++)
			addUnsorted(files[i]);
		forceResort();
    }
	
	/**
	 * Handles events created by the FileManager.  Adds or removes rows from
	 * the table as necessary. 
	 */
    void handleFileManagerEvent(final FileManagerEvent evt, DirectoryHolder holder) {
		//  Need to update table only if one of the files in evt
		//  is contained in the current directory.
		if (evt == null || holder == null)
			return;
			
		FileDesc[] fds = evt.getFileDescs();
		if (fds == null || fds.length <= 0)
			return;
			
        if(LOG.isDebugEnabled())
            LOG.debug("Handling event: " + evt);
        switch(evt.getKind()) {
        case FileManagerEvent.REMOVE:
            File f = fds[0].getFile();
            if(holder.accept(f)) {
                ((LibraryTableModel)DATA_MODEL).reinitialize(f);
                handleSelection(-1);
            } else if(DATA_MODEL.contains(f)) {
                DATA_MODEL.remove(f);
                handleSelection(-1);
            }
            break;
        case FileManagerEvent.ADD:
            if(holder.accept(fds[0].getFile())) {
                add(fds[0].getFile());
                handleSelection(-1);
            }
            break;
        case FileManagerEvent.RENAME:
            File old = fds[0].getFile();
            File now = fds[1].getFile();
            if(holder.accept(now)) {
                ((LibraryTableModel)DATA_MODEL).reinitialize(old, now);
                handleSelection(-1);
            }
            break;
        }
    }

	/**
	 * Returns the <tt>File</tt> stored at the specified row in the list.
	 *
	 * @param row the row of the desired <tt>File</tt> instance in the
	 *            list
	 *
	 * @return a <tt>File</tt> instance associated with the specified row
	 *         in the table
	 */
    File getFile(int row) {
		return ((LibraryTableModel)DATA_MODEL).getFile(row);
    }
	
	/**
	 * Returns the file desc object for the given row or <code>null</code> if
	 * there is none.
	 * @param row
	 * @return
	 */
	private FileDesc getFileDesc(int row) {
		return ((LibraryTableModel)DATA_MODEL).getFileDesc(row);
	}
	
	/**
	 * Implements the {@link FileDescProvider} interface by returning all the
	 * selected filedescs.
	 */
	public FileDetails[] getFileDetails() {
		int[] sel = TABLE.getSelectedRows();
		ArrayList files = new ArrayList(sel.length);
		for (int i = 0; i < sel.length; i++) {
			FileDesc desc = getFileDesc(sel[i]);
			if (desc != null) {
				files.add(desc);
			}
		}
		if (files.isEmpty()) {
			return new FileDetails[0];
		}
		return (FileDetails[])files.toArray(new FileDetails[0]);
	}
    
    /**
	 * Accessor for the table that this class wraps.
	 *
	 * @return The <tt>JTable</tt> instance used by the library.
	 */
    JTable getTable() {
        return TABLE;
    }

    ButtonRow getButtonRow() {
        return BUTTON_ROW;
    }

	/**
	 * Accessor for the <tt>ListSelectionModel</tt> for the wrapped
	 * <tt>JTable</tt> instance.
	 */
	ListSelectionModel getSelectionModel() {
		return TABLE.getSelectionModel();
	}


    /**
     * shows the user a meta-data for the file(if any) and allow the user
     * to edit it.
     */
    void editMeta(){
        //get the selected file. If there are more than 1 we just use the
        // last one.
        int[] rows = TABLE.getSelectedRows();
        int k = rows.length;
        if(k == 0)
            return;
        int index = rows[k-1];//this is the index of the last row selected
        FileDesc fd = ((LibraryTableModel)DATA_MODEL).getFileDesc(index);
        if( fd == null ) // oh well
            return;

        String fullName = "";
        try{
            fullName = fd.getFile().getCanonicalPath();
        }catch(IOException ee){//if there is an exception return
            return;
        }
        //Now docsOfFile has all LimeXMLDocuments pertainint to selected file
        Frame mainFrame = GUIMediator.getAppFrame();
        
        if (LimeXMLUtils.isSupportedAudioFormat(fullName)) {
            MetaEditor metaEditor = new MetaEditor(fd, fullName, mainFrame);	
			metaEditor.setLocationRelativeTo(MessageService.getParentComponent());
            metaEditor.setVisible(true);
        } else {
            MetaEditorFrame metaEditor = new MetaEditorFrame(fd, fullName, mainFrame);
			metaEditor.setLocationRelativeTo(MessageService.getParentComponent());
            metaEditor.setVisible(true);
        }
    }

	/**
	 * Performs the Bitzi lookup for the selected files in the library.
	 */
    void doBitziLookup() {
        // get the selected file. If there are more than 1 we just use the
        // last one.
        int[] rows = TABLE.getSelectedRows();
        int k = rows.length;
        if(k == 0)
            return;
        int index = rows[k-1];//this is the index of the last row selected
        FileDesc fd = ((LibraryTableModel)DATA_MODEL).getFileDesc(index);
        if (fd==null) {
            // noop
            return;
        }
        URN urn = fd.getSHA1Urn();
		if(urn==null) {
            // unable to do lookup -- perhaps SHA1 not calculated yet?;
            // TODO could show dialog suggesting user try again later but won't for now
            return;
        }
        String urnStr = urn.toString();
        int hashstart = 1+urnStr.indexOf(":",4);
        // TODO: grab this lookup URL from a template somewhere
        String lookupUrl = "http://bitzi.com/lookup/"+urnStr.substring(hashstart)+"?ref=limewire";
        try {
            Launcher.openURL(lookupUrl);
        } catch (IOException ioe) {
            // do nothing
        }
    }
    
    /**
     * Programatically starts a rename of the selected item.
     */
    void startRename() {
        int row = TABLE.getSelectedRow();
        if(row == -1)
            return;
        int viewIdx = TABLE.convertColumnIndexToView(LibraryTableDataLine.NAME_IDX);
        TABLE.editCellAt(row, viewIdx, LibraryTableCellEditor.EVENT);
    }
    
    /**
     * Shows the license window.
     */
    void showLicenseWindow() {
        DataLine dl = TABLE.getSelectedDataLine();
        if(dl == null)
            return;

        LibraryTableDataLine ldl = (LibraryTableDataLine)dl;
        FileDesc fd = ldl.getFileDesc();
        License license = fd.getLicense();
        URN urn = fd.getSHA1Urn();
        LimeXMLDocument doc = ldl.getXMLDocument();
        LicenseWindow window = LicenseWindow.create(license, urn, doc, this);
        window.setVisible(true);
    }
    
    public void licenseVerified(License license) {
        DATA_MODEL.refresh();
    }

    /**
     * Prepare a detail page of magnet link info for selected files 
     * in the library.
     */
    void doMagnetLookup() {
        doMagnetCommand("/magcmd/detail?");
    }
	
    /**
     * Fire a local lookup with file/magnet details
     */
    void doMagnetCommand(String cmd) {
        // get the selected files.  Build up a url to display details.
        int[] rows = TABLE.getSelectedRows();
        int k = rows.length;
        if(k == 0)
            return;

        boolean haveValidMagnet = false;

        int    count     = 0;
        int    port      = RouterService.getHTTPAcceptor().getPort();
        int    eport     = RouterService.getAcceptor().getPort(true);
        byte[] eaddr     = RouterService.getAcceptor().getAddress(true);
        String lookupUrl = "http://localhost:"+port+
          cmd+
          "addr="+NetworkUtils.ip2string(eaddr)+":"+eport;
        for(int i=0; i<k; i++) {
            FileDesc fd = ((LibraryTableModel)DATA_MODEL).getFileDesc(rows[i]);
            if (fd==null) {
                // Only report valid files
                continue;
            }
            URN urn = fd.getSHA1Urn();
			if(urn == null) {
                // Only report valid sha1s
                continue;
            }
            String urnStr = urn.toString();
            int hashstart = 1 + urnStr.indexOf(":", 4);
             
            String sha1 = urnStr.substring(hashstart);
            lookupUrl +=
              "&n"+count+"="+URLEncoder.encode(fd.getFileName())+
              "&u"+count+"="+sha1;
            count++;
            haveValidMagnet = true;
        }
        if (haveValidMagnet) {
            try {
                Launcher.openURL(lookupUrl);
            } catch (IOException ioe) {
                // do nothing
            }
        }
    }

    /**
     * Override the default removal so we can actually stop sharing
     * and delete the file.
	 * Deletes the selected rows in the table.
	 * CAUTION: THIS WILL DELETE THE FILE FROM THE DISK.
	 */
	public void removeSelection() {
		String msgKey = "MESSAGE_CONFIRM_FILE_DELETE";
		int response = GUIMediator.showYesNoMessage(msgKey);
		if(response != GUIMediator.YES_OPTION) return;

        int[] rows = TABLE.getSelectedRows();
		if(rows.length <= 0) return;

		Arrays.sort(rows);

		if(TABLE.isEditing()) {
			TableCellEditor editor = TABLE.getCellEditor();
			editor.cancelCellEditing();
		}
		
		ArrayList errors = new ArrayList();

		for(int i = rows.length - 1; i >= 0; i--) {
			File file = ((LibraryTableModel)DATA_MODEL).getFile(rows[i]);
            RouterService.getFileManager().removeFileIfShared(file);
            boolean removed = file.delete();
            if(!removed) {
                // try again, telling UploadManager to kill any uploads
                FileDesc fd = ((LibraryTableModel)DATA_MODEL).getFileDesc(rows[i]);
                if(fd != null) {
                    RouterService.getUploadManager().killUploadsForFileDesc(fd);
                    removed = file.delete();
                }
            }
            
			if(removed)
			    DATA_MODEL.remove(rows[i]);
			else
			    errors.add(file.getName());
		}

		clearSelection();		
		
		// go through the errors and tell them what couldn't be deleted.
		for(int i = 0; i < errors.size(); i++) {
		    String name = (String)errors.get(i);
			final String key1 = "MESSAGE_UNABLE_TO_DELETE_FILE_START";
			final String key2 = "MESSAGE_UNABLE_TO_DELETE_FILE_END";
			final String msg = "'" + name + "'.";
			// notify the user that deletion failed
			GUIMediator.showError(key1, msg, key2);
		}
    }
    
	/**
	 * Handles a name change of one of the files displayed.
	 *
	 * @param newName The new name of the file
	 *
	 * @return A <tt>String</tt> that is the name of the file
	 *         after this method is called. This is the new name if
	 *         the name change succeeded, and the old name otherwise.
	 */
	String handleNameChange(String newName) {
		int row = TABLE.getEditingRow();
		LibraryTableModel ltm = (LibraryTableModel)DATA_MODEL;
		
		
		File oldFile = ltm.getFile(row);
		String parent = oldFile.getParent();
		String nameWithExtension = newName + "." + ltm.getType(row);
		File newFile = new File(parent, nameWithExtension);
		if(!ltm.getName(row).equals(newName)) {
			if(!newFile.exists() && oldFile.renameTo(newFile)) {
				RouterService.getFileManager().renameFileIfShared(oldFile, newFile);
				// Ideally, renameFileIfShared should immediately send RENAME or REMOVE
				// callbacks.  But, if it doesn't, it should atleast have immediately
				// internally removed the file from being shared.  So, we immediately
				// do a reinitialize on the oldFile to mark it as being not shared.
				((LibraryTableModel)DATA_MODEL).reinitialize(oldFile);
				return newName;
			}
			// notify the user that renaming failed
			GUIMediator.showError("MESSAGE_UNABLE_TO_RENAME_FILE_START",
					"'" + ltm.getName(row) + "'.",
			"MESSAGE_UNABLE_TO_RENAME_FILE_END");
			return ltm.getName(row);
		}
		return newName; // newName == currentName
	}

    public void handleActionKey() {
        int[] rows = TABLE.getSelectedRows();
		LibraryTableModel ltm = (LibraryTableModel)DATA_MODEL;
		File file;
		for (int i = 0; i < rows.length; i++) {
			file = ltm.getFile(rows[i]);
			if (RouterService.getFileManager().isCompletelySharedDirectory(file)) {
				LibraryMediator.setSelectedDirectory(file);
				return;
			}
		}
		launch();
    }

    /**
     * Resume incomplete downloads
     */    
    void resumeIncomplete() {        
        //For each selected row...
        int[] rows = TABLE.getSelectedRows();
        boolean startedDownload=false;
        ArrayList errors = new ArrayList();
        for (int i=0; i<rows.length; i++) {
            //...try to download the incomplete
            File incomplete=((LibraryTableModel)DATA_MODEL).getFile(rows[i]);
            try {
                RouterService.download(incomplete);
                startedDownload=true;
            } catch (SaveLocationException e) { 
                // we must cache errors to display later so we don't wait
                // while the table might change in the background.
                errors.add(e);
            } catch(CantResumeException e) {
                errors.add(e);
            }
        }
        
        // traverse back through the errors and show them.
        for(int i = 0; i < errors.size(); i++) {
            Exception e = (Exception)errors.get(i);
            if(e instanceof SaveLocationException) {
				SaveLocationException sle = (SaveLocationException)e;
				if (sle.getErrorCode() == SaveLocationException.FILE_ALREADY_DOWNLOADING) {
					GUIMediator.showFormattedError("FORMATTED_ERROR_ALREADY_DOWNLOADING",
		                    new Object[] { sle.getFile() },
		                    QuestionsHandler.ALREADY_DOWNLOADING);
				}
				else {
					String msg = CoreExceptionHandler.getSaveLocationErrorString(sle);
					GUIMediator.showTranslatedError(msg);
				}
            } else if ( e instanceof CantResumeException ) {
                GUIMediator.showError("ERROR_CANT_RESUME_START",
                    "\""+((CantResumeException)e).getFilename()+"\"",
                    "ERROR_CANT_RESUME_END",
                    QuestionsHandler.CANT_RESUME);
            }
        }       

        //Switch to download tab (if we actually started anything).
        if (startedDownload)
            GUIMediator.instance().setWindow(GUIMediator.SEARCH_INDEX);
    }

    

    /**
	 * Launches the associated applications for each selected file
	 * in the library if it can.
	 */
    void launch() {
        int[] rows = TABLE.getSelectedRows();
        boolean audioLaunched = false;
		for(int i = 0, l = rows.length; i < l; i++) {
            File currFile = ((LibraryTableModel)DATA_MODEL).getFile(rows[i]);
            if (!audioLaunched && GUIMediator.isPlaylistVisible() && PlaylistMediator.isPlayableFile(currFile)) {
					GUIMediator.instance().launchAudio(currFile);
					audioLaunched = true;
            } else {
                try {
                    GUIMediator.launchFile(currFile);
                } catch(IOException ignored) {}
            }
		}
    }

	/**
	 * Handles the selection of the specified row in the library window,
	 * enabling or disabling buttons and chat menu items depending on
	 * the values in the row.
	 *
	 * @param row the selected row
	 */
	public void handleSelection(int row) {
		if (TABLE.getSelectedRowCount() <= 0) {
			handleNoSelection();
			return;
		}

		//  always turn on Launch, Delete, Magnet Lookup, Bitzi Lookup
		setButtonEnabled(LibraryTableButtons.LAUNCH_BUTTON, true);
		setButtonEnabled(LibraryTableButtons.DELETE_BUTTON, true);
		
		//  turn on Enqueue if play list is visible and a selected item is playable
		int[] sel = TABLE.getSelectedRows();
		if (GUIMediator.isPlaylistVisible()) {
			boolean found = false;
			for (int i = 0; i < sel.length; i++)
	            if (PlaylistMediator.isPlayableFile(((LibraryTableModel)DATA_MODEL).getFile(sel[i]))) {
					found = true;
					break;
	            }
			setButtonEnabled(LibraryTableButtons.PLAYLIST_BUTTON, found);
        }

		//  turn on Describe... for complete files when single selection
		if (!_isIncomplete && _annotateEnabled && TABLE.getSelectedRowCount() == 1 &&
			((LibraryTableModel)DATA_MODEL).getFileDesc(TABLE.getSelectedRow()) != null) {
		    setButtonEnabled(LibraryTableButtons.ANNOTATE_BUTTON, true);
			ANNOTATE_LISTENER.setEnabled(true);
		} else {
		    setButtonEnabled(LibraryTableButtons.ANNOTATE_BUTTON, false);
			ANNOTATE_LISTENER.setEnabled(false);
		}

		//  turn on Resume button if Incomplete folder is currently selected
		setButtonEnabled(LibraryTableButtons.RESUME_BUTTON, _isIncomplete);
		
		//  enable Share File action when any selected file is not shared
		boolean shareAllowed = false;
		boolean unshareAllowed = false;
		boolean shareFolderAllowed = false;
		boolean unshareFolderAllowed = false;
		boolean foundDir = false;
		for (int i = 0; i < sel.length; i++) {
			File file = getFile(sel[i]);
			if (file.isDirectory()) {
				//  turn off delete (only once) if directory found
				if (!foundDir){
					setButtonEnabled(LibraryTableButtons.DELETE_BUTTON, false);
					foundDir = true;
				}
				if (!RouterService.getFileManager().isCompletelySharedDirectory(file))
					shareFolderAllowed = true;
				else
					unshareFolderAllowed = true;
			} else {
				if (!RouterService.getFileManager().isFileShared(file)) {
					if (!FileManager.isFilePhysicallyShareable(file) || _isIncomplete)
						continue;
					shareAllowed = true;
				} else {
					unshareAllowed = true;
				}
				
				if (shareAllowed && unshareAllowed && shareFolderAllowed && unshareFolderAllowed)
					break;
			}
		}
		SHARE_ACTION.setEnabled(shareAllowed);
		UNSHARE_ACTION.setEnabled(unshareAllowed);
		SHARE_FOLDER_ACTION.setEnabled(shareFolderAllowed);
		UNSHARE_FOLDER_ACTION.setEnabled(unshareFolderAllowed);
		
		//  enable / disable advanced items if file shared / not shared
		File file = getFile(sel[0]);
		boolean firstShared = RouterService.getFileManager().isFileShared(file);
		MAGNET_LOOKUP_ACTION.setEnabled(firstShared);
		BITZI_LOOKUP_ACTION.setEnabled(firstShared);
		COPY_MAGNET_TO_CLIPBOARD_ACTION.setEnabled
			(!_isIncomplete && getFileDesc(sel[0]) != null);
	}

	/**
	 * Handles the deselection of all rows in the library table,
	 * disabling all necessary buttons and menu items.
	 */
	public void handleNoSelection() {
		setButtonEnabled(LibraryTableButtons.LAUNCH_BUTTON, false);
		setButtonEnabled(LibraryTableButtons.DELETE_BUTTON, false);

        if (GUIMediator.isPlaylistVisible())
            setButtonEnabled(LibraryTableButtons.PLAYLIST_BUTTON, false);

		ANNOTATE_LISTENER.setEnabled(false);

        setButtonEnabled(LibraryTableButtons.ANNOTATE_BUTTON, false);
		setButtonEnabled(LibraryTableButtons.RESUME_BUTTON, false);
		
		COPY_MAGNET_TO_CLIPBOARD_ACTION.setEnabled(false);
		MAGNET_LOOKUP_ACTION.setEnabled(false);
		BITZI_LOOKUP_ACTION.setEnabled(false);
		LICENSE_ACTION.setEnabled(false);
		SHARE_ACTION.setEnabled(false);
		UNSHARE_ACTION.setEnabled(false);
	}

    ///////////////////////
    // A collection of private listeners
    //////////////////////

	
    private final class AnnotateAction extends AbstractAction {
		
		public AnnotateAction() {
			putValue(Action.NAME, 
					 GUIMediator.getStringResource("LIBRARY_ANNOTATE_BUTTON_LABEL"));
		}
		
    	public void actionPerformed(ActionEvent ae) {
    		editMeta();
    	}
    }

	private class ShareFileAction extends AbstractAction {
		
		public ShareFileAction() {
			putValue(Action.NAME, GUIMediator.getStringResource
					 ("SHARE_FILE_POPUP_MENU_LABEL"));
		}
		
		public void actionPerformed(ActionEvent e) {
			int[] sel = TABLE.getSelectedRows();
			final File[] files = new File[sel.length];
			for (int i = 0; i < sel.length; i++) {
				files[i] = getFile(sel[i]);
			}
			
			GUIMediator.instance().schedule(new Runnable() {
			    public void run() {
        			for (int i = 0; i < files.length; i++) {
						File file = files[i];
						if (file == null || file.isDirectory())
							continue;
        				RouterService.getFileManager().addFileAlways(file);
        			}
                }
            });
		}
	}
	
	private class UnshareFileAction extends AbstractAction {
		
		public UnshareFileAction() {
			putValue(Action.NAME, GUIMediator.getStringResource
					 ("UNSHARE_FILE_POPUP_MENU_LABEL"));
		}
		
		public void actionPerformed(ActionEvent e) {
			int[] sel = TABLE.getSelectedRows();
			final File[] files = new File[sel.length];
			for (int i = sel.length - 1; i >= 0; i--) {
				files[i] = getFile(sel[i]);
			}
			
			GUIMediator.instance().schedule(new Runnable() {
			    public void run() {
        			for (int i = 0; i < files.length; i++) {
						File file = files[i];
						if (file == null || file.isDirectory())
							continue;
        				RouterService.getFileManager().stopSharingFile(file);
        			}
                }
            });
		}
	}
	
	private class ShareFolderAction extends AbstractAction {
		
		public ShareFolderAction() {
			putValue(Action.NAME, GUIMediator.getStringResource
					 ("SHARE_FOLDER_POPUP_MENU_LABEL"));
		}
		
		public void actionPerformed(ActionEvent e) {
			int[] sel = TABLE.getSelectedRows();
			final File[] files = new File[sel.length];
			for (int i = 0; i < sel.length; i++) {
				files[i] = getFile(sel[i]);
			}
			
			GUIMediator.instance().schedule(new Runnable() {
			    public void run() {
        			for (int i = 0; i < files.length; i++) {
						File file = files[i];
						if (file == null || !file.isDirectory())
							continue;
        				RouterService.getFileManager().addSharedFolder(file);
					}        			
                }
            });
		}
	}
	
	private class UnshareFolderAction extends AbstractAction {
		
		public UnshareFolderAction() {
			putValue(Action.NAME, GUIMediator.getStringResource
					 ("UNSHARE_FOLDER_POPUP_MENU_LABEL"));
		}
		
		public void actionPerformed(ActionEvent e) {
			int[] sel = TABLE.getSelectedRows();
			final File[] files = new File[sel.length];
			for (int i = sel.length - 1; i >= 0; i--) {
				files[i] = getFile(sel[i]);
			}
			
			GUIMediator.instance().schedule(new Runnable() {
			    public void run() {
        			for (int i = 0; i < files.length; i++) {
						File file = files[i];
						if (file == null || !file.isDirectory())
							continue;
        				RouterService.getFileManager().removeFolderIfShared(file);
        			}
                }
            });
		}
	}
	
    private final class ResumeListener implements ActionListener {
    	public void actionPerformed(ActionEvent ae) {
            resumeIncomplete();
    	}
    }

    private final class MagnetLookupAction extends AbstractAction {
		
		public MagnetLookupAction() {
			putValue(Action.NAME, GUIMediator.getStringResource
					("SEARCH_PUBLIC_MAGNET_LOOKUP_STRING"));
		}
		
        public void actionPerformed(ActionEvent e){
            doMagnetLookup();
        }
    }
	
	private class LicenseAction extends AbstractAction {

		public LicenseAction() {
			putValue(Action.NAME, GUIMediator.getStringResource
					("LICENSE_VIEW_LICENSE"));
		}
		
		public void actionPerformed(ActionEvent e) {
			showLicenseWindow();
		}
	}

    private final class LaunchListener implements ActionListener {

    	public void actionPerformed(ActionEvent ae) {
    		launch();
    	}
    }


    private final class AddPLFileListener implements ActionListener{
        public void actionPerformed(ActionEvent ae){
			//get the selected file. If there are more than 1 we add all
			int[] rows = TABLE.getSelectedRows();
			for (int i = 0; i < rows.length; i++) {
				int index = rows[i]; // current index to add
				File file = ((LibraryTableModel)DATA_MODEL).getFile(index);
				if (GUIMediator.isPlaylistVisible() && PlaylistMediator.isPlayableFile(file))
					LibraryMediator.instance().addFileToPlayList(file);
			}
        }
    }
}
