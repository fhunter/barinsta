package awais.instagrabber.asyncs;

import java.util.ArrayList;
import java.util.List;

import awais.instagrabber.customviews.helpers.PostFetcher;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.repositories.responses.Caption;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.repositories.responses.PostsFetchResponse;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.webservices.FeedService;
import awais.instagrabber.webservices.ServiceCallback;
import zerrium.FilterKeywordsUtility;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class FeedPostFetchService implements PostFetcher.PostFetchService {
    private static final String TAG = "FeedPostFetchService";
    private final FeedService feedService;
    private String nextCursor;
    private boolean hasNextPage;

    public FeedPostFetchService() {
        feedService = FeedService.getInstance();
    }

    @Override
    public void fetch(final FetchListener<List<Media>> fetchListener) {
        final List<Media> feedModels = new ArrayList<>();
        final String cookie = settingsHelper.getString(Constants.COOKIE);
        final String csrfToken = CookieUtils.getCsrfTokenFromCookie(cookie);
        final String deviceUuid = settingsHelper.getString(Constants.DEVICE_UUID);
        feedModels.clear();
        feedService.fetch(csrfToken, deviceUuid, nextCursor, new ServiceCallback<PostsFetchResponse>() {
            @Override
            public void onSuccess(final PostsFetchResponse result) {
                if (result == null && feedModels.size() > 0) {
                    fetchListener.onResult(feedModels);
                    return;
                } else if (result == null) return;
                nextCursor = result.getNextCursor();
                hasNextPage = result.hasNextPage();

                //Skip adding (junk) post to Feed models
                for(final Media m:result.getFeedModels()){
                    final Caption c = m.getCaption();
                    if(c == null){
                        feedModels.add(m); //No caption
                        continue;
                    }
                    if(!FilterKeywordsUtility.filter(c.getText())){ //Check caption if it doesn't contain any specified keywords in filter_keywords.xml
                        feedModels.add(m);
                    }
                }
                //feedModels.addAll(result.getFeedModels());
                if (fetchListener != null) {
                    // if (feedModels.size() < 15 && hasNextPage) {
                    //     feedService.fetch(csrfToken, nextCursor, this);
                    // } else {
                    fetchListener.onResult(feedModels);
                    // }
                }
            }

            @Override
            public void onFailure(final Throwable t) {
                if (fetchListener != null) {
                    fetchListener.onFailure(t);
                }
            }
        });
    }

    @Override
    public void reset() {
        nextCursor = null;
    }

    @Override
    public boolean hasNextPage() {
        return hasNextPage;
    }
}
