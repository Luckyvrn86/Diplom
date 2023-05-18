package searchengine.services.parsing;


import searchengine.services.indexing.IndexingServiceImpl;

import java.util.concurrent.CopyOnWriteArraySet;

public class SiteParser {
    private final String url;
    private volatile SiteParser parent;
    private final IndexingServiceImpl indexingService;
    private final CopyOnWriteArraySet<SiteParser> child;

    public SiteParser(String url, IndexingServiceImpl indexingService) {
        this.url = url;
        child = new CopyOnWriteArraySet<>();
        parent = null;
        this.indexingService = indexingService;
    }

    public void addChild(SiteParser child) {
        if (!this.child.contains(child) && child.getUrl().startsWith(url)) {
            this.child.add(child);
            child.setParent(this);
        }
    }

    private void setParent(SiteParser siteMapSiteParser) {
        synchronized (this) {
            this.parent = siteMapSiteParser;
        }
    }

    public String getUrl() {
        return url;
    }

    public CopyOnWriteArraySet<SiteParser> getChild() {
        return child;
    }

    public IndexingServiceImpl getIndexingService() {
        return indexingService;
    }
}