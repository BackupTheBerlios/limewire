package com.limegroup.gnutella.gui.xml.editor;

import java.awt.Component;
import java.util.Iterator;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.text.JTextComponent;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.util.NameValue;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLReplyCollection;
import com.limegroup.gnutella.xml.LimeXMLSchema;
import com.limegroup.gnutella.xml.LimeXMLSchemaRepository;
import com.limegroup.gnutella.xml.SchemaReplyCollectionMapper;
import com.limegroup.gnutella.gui.xml.ComboBoxValue;

public abstract class MetaEditorTabbedPane extends JTabbedPane {
    
    protected final FileDesc fd;
    protected final LimeXMLDocument document;
    protected final LimeXMLSchema schema;
    
    public MetaEditorTabbedPane(FileDesc fd, String uri) {
        super();
        
        this.fd = fd;
        
        SchemaReplyCollectionMapper map = SchemaReplyCollectionMapper.instance();
        LimeXMLReplyCollection collection = map.getReplyCollection(uri);
        LimeXMLSchemaRepository rep = LimeXMLSchemaRepository.instance();
        schema = rep.getSchema(uri);
        LimeXMLDocument storedDoc = null;
        for(Iterator i = fd.getLimeXMLDocuments().iterator(); i.hasNext(); ) {
            LimeXMLDocument doc = (LimeXMLDocument)i.next();
            if(schema.equals(doc.getSchema())) {
                storedDoc = doc;
                break;
            }
        }
        document = storedDoc;
    }
    
        
    public FileDesc getFileDesc() {
        return fd;
    }
    
    public LimeXMLDocument getDocument() {
        return document;
    }
    
    public LimeXMLSchema getSchema() {
        return schema;
    }
    
    public String getInput() {
        java.util.ArrayList namValList = new java.util.ArrayList();

        final int count = getTabCount();
        for(int i = 0; i < count; i++) {
            
            Component tab = getComponentAt(i);
            if (tab instanceof MetaEditorPanel) {
                
                MetaEditorPanel panel = (MetaEditorPanel)tab;
                if (panel.hasChanged()) {
                    
                    panel.prepareSave();
                    
                    Iterator it = panel.getComponentIterator();
                    while(it.hasNext()) {
                        String name = (String)it.next();
                        JComponent comp = panel.getComponent(name);
                        String value = null;
                        
                        if (comp instanceof JTextComponent) {
                            value = ((JTextComponent)comp).getText().trim();
                        } else if (comp instanceof JComboBox) {
                            JComboBox box = (JComboBox)comp;
                            ComboBoxValue cbv = (ComboBoxValue)box.getSelectedItem();
                            if(cbv != null) {
                                String cbvalue = cbv.getValue();
                                if(cbvalue != null)
                                    value = cbvalue.trim();
                            }
                        }

                        if (value != null && !value.equals("")) {
                            NameValue namValue = new NameValue(name, value);
                            namValList.add(namValue);
                        }
                    }
                    
                    for(Iterator j = panel.getUneditedFieldsIterator(); j.hasNext(); )
                        namValList.add((NameValue)j.next());
                }
   
            }
        }
        
        if (namValList.isEmpty()) {
            return null;
        } else {
            return new LimeXMLDocument(namValList, getSchema().getSchemaURI()).getXMLString();
        }
    }
}
