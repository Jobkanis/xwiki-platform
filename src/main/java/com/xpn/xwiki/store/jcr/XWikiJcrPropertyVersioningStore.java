package com.xpn.xwiki.store.jcr;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiDocumentArchive;
import com.xpn.xwiki.monitor.api.MonitorPlugin;
import com.xpn.xwiki.store.XWikiVersioningStoreInterface;
import com.xpn.xwiki.util.Util;
import org.suigeneris.jrcs.rcs.Archive;
import org.suigeneris.jrcs.rcs.Version;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import java.lang.reflect.InvocationTargetException;

/** Versions store in jcr property '@archive' of xwiki:document */
public class XWikiJcrPropertyVersioningStore extends XWikiJcrBaseStore implements XWikiVersioningStoreInterface {
	public XWikiJcrPropertyVersioningStore(XWiki xwiki, XWikiContext context) throws SecurityException, NoSuchMethodException, ClassNotFoundException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		super(xwiki, context);
	}
	
	public XWikiJcrPropertyVersioningStore(XWikiContext context) throws SecurityException, NoSuchMethodException, ClassNotFoundException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
        this(context.getWiki(), context);
    }
	
	public void saveXWikiDocArchive(final XWikiDocumentArchive archivedoc, boolean bTransaction, XWikiContext context) throws XWikiException {
		try {
			executeWrite(context, new JcrCallBack() {
				public Object doInJcr(XWikiJcrSession session) throws Exception {
					Node docNode = getDocNodeById(session, archivedoc.getId());
					if (docNode==null) return null;
					String s = archivedoc.getArchive();
					docNode.setProperty("archive", s);
					session.save();
					return null;
				}
			});
        } catch (Exception e) {
            Object[] args = { new Long(archivedoc.getId()) };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_SAVING_OBJECT,
                    "Exception while saving archive {0}", e, args);
        }
	}
	public void loadXWikiDocArchive(final XWikiDocumentArchive archivedoc, boolean bTransaction, XWikiContext context) throws XWikiException {
		try {
			executeRead(context, new JcrCallBack() {
				public Object doInJcr(XWikiJcrSession session) throws Exception {
					Node docNode = getDocNodeById(session, archivedoc.getId());
					if (docNode==null) return null;
					try {
						Property prop = docNode.getProperty("archive");
						String s = prop.getString();
						archivedoc.setArchive( s );
					} catch (PathNotFoundException e) {};
					return null;
				}
			});
        } catch (Exception e) {
            Object[] args = { new Long(archivedoc.getId()) };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_LOADING_OBJECT,
                    "Exception while loading archive {0}", e, args);
        }
	}

	// From XWikiHibernateVersioningStore:
		
	public void resetRCSArchive(XWikiDocument doc, boolean bTransaction, XWikiContext context) throws XWikiException {
		try {
            XWikiDocumentArchive archivedoc = new XWikiDocumentArchive(doc.getId());
            loadXWikiDocArchive(archivedoc, bTransaction, context);
            archivedoc.resetArchive(doc.getFullName(), doc.getContent(), doc.getVersion());
            saveXWikiDocArchive(archivedoc, bTransaction, context);
        } catch (Exception e) {
            Object[] args = { doc.getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_LOADING_OBJECT,
                    "Exception while loading archive {0}", e, args);
        }
	}

	

	public void updateXWikiDocArchive(XWikiDocument doc, String text, boolean bTransaction, XWikiContext context) throws XWikiException {
		try {
			XWikiDocumentArchive archivedoc = getXWikiDocumentArchive(doc, context);
            archivedoc.updateArchive(doc.getFullName(), text);
            saveXWikiDocArchive(archivedoc, bTransaction, context);            
        } catch (Exception e) {
            Object[] args = { doc.getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_LOADING_OBJECT,
                    "Exception while loading archive {0}", e, args);
        }
	}

	public XWikiDocument loadXWikiDoc(XWikiDocument basedoc, String version, XWikiContext context) throws XWikiException {
		XWikiDocument doc = new XWikiDocument(basedoc.getSpace(), basedoc.getName());
		doc.setDatabase(basedoc.getDatabase());
        MonitorPlugin monitor = Util.getMonitorPlugin(context);
        try {
            // Start monitoring timer
            if (monitor!=null)
                monitor.startTimer("jcr");
            doc.setStore(basedoc.getStore());

            Archive archive = getXWikiDocumentArchive(doc, context).getRCSArchive();
            Version v = null;
            try {
                v = archive.getRevisionVersion(version);
            } catch (Exception e) {}

            if (v==null) {
                Object[] args = { doc.getFullName(), version.toString() };
                throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_UNEXISTANT_VERSION,
                        "Version {1} does not exist while reading document {0}", null,args);
            }

            if (!version.equals(v.toString())) {
                doc.setVersion(version);
                return doc;
            }
            Object[] text = (Object[]) archive.getRevision(version);
            StringBuffer content = new StringBuffer();
            for (int i=0;i<text.length;i++) {
                String line = text[i].toString();
                content.append(line);
                content.append("\n");
            }
            doc.fromXML(content.toString());
            // Make sure the document has the same name
            // as the new document (in case there was a name change
            doc.setName(basedoc.getName());
            doc.setSpace(basedoc.getSpace());
        } catch (Exception e) {
        	if (e instanceof XWikiException)
                throw (XWikiException) e;
            Object[] args = { doc.getFullName(), version.toString() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_READING_VERSION,
                    "Exception while reading document {0} version {1}", e, args);
        } finally {
            // End monitoring timer
            if (monitor!=null)
                monitor.endTimer("jcr");
        }
        return doc;
	}
	public Version[] getXWikiDocVersions(XWikiDocument doc, XWikiContext context) throws XWikiException {
		try {
            Archive archive = getXWikiDocumentArchive(doc, context).getRCSArchive();
            if (archive==null)
                return new Version[0];

            org.suigeneris.jrcs.rcs.Node[] nodes = archive.changeLog();
            Version[] versions = new Version[nodes.length];
            for (int i=0;i<nodes.length;i++) {
                versions[i] = nodes[i].getVersion();
            }
            return versions;
        } catch (Exception e) {
            Object[] args = { doc.getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_READING_REVISIONS,
                    "Exception while reading document {0} revisions", e, args);
        }
	}

	public XWikiDocumentArchive getXWikiDocumentArchive(XWikiDocument doc, XWikiContext context) throws XWikiException {
        String key = ((doc.getDatabase()==null)?"xwiki":doc.getDatabase()) + ":" + doc.getFullName();
        synchronized (key) {
            XWikiDocumentArchive archivedoc = (XWikiDocumentArchive) context.getDocumentArchive(key);
            if (archivedoc==null) {
                String db = context.getDatabase();
                try {
                    if (doc.getDatabase()!=null)
                        context.setDatabase(doc.getDatabase());
                    archivedoc = new XWikiDocumentArchive(doc.getId());
                    loadXWikiDocArchive(archivedoc, true, context);
                } finally {
                    context.setDatabase(db);
                }
                // This will also make sure that the Archive has a strong reference
                // and will not be discarded as long as the context exists.
                context.addDocumentArchive(key, archivedoc);
            }
            return archivedoc;
        }
    }
}
