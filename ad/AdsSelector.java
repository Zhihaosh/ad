package ad;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import net.spy.memcached.MemcachedClient;
import org.omg.Messaging.SYNC_WITH_TRANSPORT;
import service.AdService;

public class AdsSelector {
    private static AdsSelector instance = null;
    //private int EXP = 7200;
    private String mMemcachedServer;
    private int mMemcachedPortal;
    private String mysql_host;
    private String mysql_db;
    private String mysql_user;
    private String mysql_pass;
    private AdService adService;
    protected AdsSelector(String memcachedServer,int memcachedPortal, AdService adService)
    {
        mMemcachedServer = memcachedServer;
        mMemcachedPortal = memcachedPortal;
        this.adService = adService;
    }
    public static AdsSelector getInstance(String memcachedServer,int memcachedPortal, AdService adService) {
        if(instance == null) {
            instance = new AdsSelector(memcachedServer, memcachedPortal,adService);
        }
        return instance;
    }
    public List<Ad> selectAds(List<String> queryTerms)
    {
        List<Ad> adList = new ArrayList<Ad>();
        HashMap<Long,Integer> matchedAds = new HashMap<Long,Integer>();
        try {
            MemcachedClient cache = new MemcachedClient(new InetSocketAddress(mMemcachedServer,mMemcachedPortal));

            for(String queryTerm : queryTerms)
            {
                System.out.println("selectAds queryTerm = " + queryTerm);
                @SuppressWarnings("unchecked")
                Set<Long>  adIdList = (Set<Long>)cache.get(queryTerm);
                if(adIdList != null && adIdList.size() > 0)
                {
                    for(Object adId : adIdList)
                    {
                        Long key = (Long)adId;
                        if(matchedAds.containsKey(key))
                        {
                            int count = matchedAds.get(key) + 1;
                            matchedAds.put(key, count);
                        }
                        else
                        {
                            matchedAds.put(key, 1);
                        }
                    }
                }
            }
            for(Long adId:matchedAds.keySet())
            {
                System.out.println("DINF"+adId);
                Ad  ad = adService.findById(adId);
                if(ad == null)
                    continue;
                double relevanceScore = (double) (matchedAds.get(adId) * 1.0 / ad.keyWords.split(",").length);
                ad.relevanceScore = relevanceScore;
                System.out.println("selectAds pClick = " + ad.pClick);
                System.out.println("selectAds relevanceScore = " + ad.relevanceScore);
                adList.add(ad);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return adList;
    }
}
