package searchengine.dto.indexing;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IndexingResponse {
    private boolean result;
    private String error = "";

    public IndexingResponse(boolean result, String error) {
    }
}
