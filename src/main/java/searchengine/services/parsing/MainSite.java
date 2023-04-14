package searchengine.services.parsing;


import searchengine.services.IndexingServiceImpl;

import java.util.concurrent.CopyOnWriteArraySet;

public class MainSite {
    private final String url;
    private volatile MainSite parent;
    private final IndexingServiceImpl indexingService;
    private final CopyOnWriteArraySet<MainSite> child;

    public MainSite(String url, IndexingServiceImpl indexingService) {
        this.url = url;
        child = new CopyOnWriteArraySet<>();
        parent = null;
        this.indexingService = indexingService;
    }

    public void addChild(MainSite child) {
        if (!this.child.contains(child) && child.getUrl().startsWith(url)) {
            this.child.add(child);
            child.setParent(this);
        }
    }

    private void setParent(MainSite siteMapMainSite) {
        synchronized (this) {
            this.parent = siteMapMainSite;
        }
    }

    public String getUrl() {
        return url;
    }

    public CopyOnWriteArraySet<MainSite> getChild() {
        return child;
    }

    public IndexingServiceImpl getIndexingService() {
        return indexingService;
    }
}