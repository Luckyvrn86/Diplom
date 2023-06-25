package searchengine.config;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SiteConfig {
    private String url;
    private String name;

    public SiteConfig(String url, String name) {
        this.url = url;
        this.name = name;
    }
}
