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
    MemcachedClient tfCacheClient;
    MemcachedClient dfCacheClient;
    private AdService adService;
    protected AdsSelector(String memcachedServer,int memcachedPortal, AdService adService, String mTFMemcachedPortal)
    {
        mMemcachedServer = memcachedServer;
        mMemcachedPortal = memcachedPortal;
        this.adService = adService;
        tfCacheClient = new MemcachedClient(new InetSocketAddress(mMemcachedServer,mTFMemcachedPortal));

        dfCacheClient = new MemcachedClient(new ConnectionFactoryBuilder().setDaemon(true).setFailureMode(FailureMode.Retry).build(), AddrUtil.getAddresses(df_address));
    }
    public static AdsSelector getInstance(String memcachedServer,int memcachedPortal, AdService adService) {
        if(instance == null) {
            instance = new AdsSelector(memcachedServer, memcachedPortal,adService);
        }
        return instance;
    }
    public List<Ad> selectAds(List<String> queryTerms,String device_id, String device_ip, String query_category)
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
                double relevanceScoreTFIDF = getRelevanceScoreByTFIDF(adId, ad.keyWords.split(",").length, queryTerms);
                double relevanceScore = (double) (matchedAds.get(adId) * 1.0 / ad.keyWords.split(",").length);
                ad.relevanceScore = relevanceScore;
                ad.pClick = 0.0;

                System.out.println("selectAds pClick = " + ad.pClick);
                System.out.println("selectAds relevanceScore = " + ad.relevanceScore);
                adList.add(ad);
            }
                //calculate pClick
                MemcachedClient featureCacheClient = new MemcachedClient(new InetSocketAddress(mMemcachedServer,mMemcachedPortal));
                for(Ad ad : adList) {
                    predictCTR(ad, queryTerms, device_id, device_ip, query_category, featureCacheClient);
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

    private double calculateTFIDF(Long adId, String term, int docLength) {
        String tfKey = adId.toString() + "_" + term;
        System.out.println("tfKey = " + tfKey);
        System.out.println("dfKey = " + term);

        String tf = (String)tfCacheClient.get(tfKey);
        System.out.println("tf = " + tf);

        String df =  (String)dfCacheClient.get(term);
        System.out.println("df=" + df);

        if(tf != null && df != null) {
            int tfVal = Integer.parseInt(tf);
            int dfVal = Integer.parseInt(df);
            double dfScore = Math.log10(numDocs * 1.0 / (dfVal + 1));
            double tfScore = Math.sqrt(tfVal);
            double norm = Math.sqrt(docLength);
            double tfidfScore = (dfScore*tfScore) / norm;
            return tfidfScore;
        }
        return 0.0;
    }
    private double getRelevanceScoreByTFIDF(Long adId, int numOfKeyWords, List<String> queryTerms) {
        double relevanceScore = 0.0;
        for(String term : queryTerms) {
            relevanceScore += calculateTFIDF(adId, term, numOfKeyWords);
        }
        return relevanceScore;
    }

    private void predictCTR(Ad ad, List<String> queryTerms, String device_id, String device_ip, String query_category, MemcachedClient featureCacheClient) {
        //construct features, note that the order of features must be as follows
        ArrayList<Double> features = new ArrayList<Double>();
        //device_ip_click
        String device_ip_click_key = "dipc_" + device_ip;
        @SuppressWarnings("unchecked")
        String device_ip_click_val_str = (String)featureCacheClient.get(device_ip_click_key);
        Double device_ip_click_val = 0.0;
        if (device_ip_click_val_str != null && device_ip_click_val_str!= "") {
            device_ip_click_val = Double.parseDouble(device_ip_click_val_str);
        }
        features.add(device_ip_click_val);

        System.out.println("device_ip_click_key = " + device_ip_click_key);
        System.out.println("device_ip_click_val = " + device_ip_click_val);

        //device_ip_impression
        String device_ip_impression_key = "dipi_" + device_ip;
        @SuppressWarnings("unchecked")
        String device_ip_impression_val_str = (String)featureCacheClient.get(device_ip_impression_key);
        Double device_ip_impression_val = 0.0;
        if (device_ip_impression_val_str != null && device_ip_impression_val_str!= "") {
            device_ip_impression_val = Double.parseDouble(device_ip_impression_val_str);
        }
        features.add(device_ip_impression_val);
        //System.out.println("device_ip_impression_key = " + device_ip_impression_key);
        System.out.println("device_ip_impression_val = " + device_ip_impression_val);


        //device_id_click
        String device_id_click_key = "didc_" + device_id;
        @SuppressWarnings("unchecked")
        String device_id_click_val_str = (String)featureCacheClient.get(device_id_click_key);
        Double device_id_click_val = 0.0;
        if (device_id_click_val_str != null && device_id_click_val_str!= "") {
            device_id_click_val = Double.parseDouble(device_id_click_val_str);
        }
        features.add(device_id_click_val);
        System.out.println("device_id_click_key = " + device_id_click_key);
        System.out.println("device_id_click_val = " + device_id_click_val);
        System.out.println("device_id_click_val_str = " + device_id_click_val_str);

        //device_id_impression
        String device_id_impression_key = "didi_" + device_id;
        @SuppressWarnings("unchecked")
        String device_id_impression_val_str = (String)featureCacheClient.get(device_id_impression_key);
        Double device_id_impression_val = 0.0;
        if (device_id_impression_val_str != null && device_id_impression_val_str!= "") {
            device_id_impression_val = Double.parseDouble(device_id_impression_val_str);
        }
        features.add(device_id_impression_val);
        System.out.println("device_id_impression_key = " + device_id_impression_key);
        System.out.println("device_id_impression_val = " + device_id_impression_val);

        //ad_id_click
        String ad_id_click_key = "aidc_" + ad.adId;
        @SuppressWarnings("unchecked")
        String ad_id_click_val_str = (String)featureCacheClient.get(ad_id_click_key);
        Double ad_id_click_val = 0.0;
        if (ad_id_click_val_str != null && ad_id_click_val_str!= "") {
            ad_id_click_val = Double.parseDouble(ad_id_click_val_str);
        }
        features.add(ad_id_click_val);

        //ad_id_impression
        String ad_id_impression_key = "aidi_" + ad.adId;
        @SuppressWarnings("unchecked")
        String ad_id_impression_val_str = (String)featureCacheClient.get(ad_id_impression_key);
        Double ad_id_impression_val = 0.0;
        if (ad_id_impression_val_str != null && ad_id_impression_val_str!= "") {
            ad_id_impression_val = Double.parseDouble(ad_id_impression_val_str);
        }
        features.add(ad_id_impression_val);

        String query = Utility.strJoin(queryTerms, "_");
        //query_campaign_id_click
        String query_campaign_id_click_key = "qcidc_" + query + "_" + ad.campaignId;
        @SuppressWarnings("unchecked")
        String query_campaign_id_click_val_str = (String)featureCacheClient.get(query_campaign_id_click_key);
        Double query_campaign_id_click_val = 0.0;
        if (query_campaign_id_click_val_str != null && query_campaign_id_click_val_str!= "") {
            query_campaign_id_click_val = Double.parseDouble(query_campaign_id_click_val_str);
        }
        features.add(query_campaign_id_click_val);

        //query_campaign_id_impression
        String query_campaign_id_impression_key = "qcidi_" + query + "_" + ad.campaignId;
        @SuppressWarnings("unchecked")
        String query_campaign_id_impression_val_str = (String)featureCacheClient.get(query_campaign_id_impression_key);
        Double query_campaign_id_impression_val = 0.0;
        if (query_campaign_id_impression_val_str != null && query_campaign_id_impression_val_str!= "") {
            query_campaign_id_impression_val = Double.parseDouble(query_campaign_id_impression_val_str);
        }
        features.add(query_campaign_id_impression_val);

        //query_ad_id_click
        String query_ad_id_click_key = "qaidc_" + query + "_" + ad.adId;
        @SuppressWarnings("unchecked")
        String query_ad_id_click_val_str = (String)featureCacheClient.get(query_ad_id_click_key);
        Double query_ad_id_click_val = 0.0;
        if (query_ad_id_click_val_str != null && query_ad_id_click_val_str!= "") {
            query_ad_id_click_val = Double.parseDouble(query_ad_id_click_val_str);
        }
        features.add(query_ad_id_click_val);

        //query_ad_id_impression
        String query_ad_id_impression_key = "qaidi_" + query + "_" + ad.adId;
        @SuppressWarnings("unchecked")
        String query_ad_id_impression_val_str = (String)featureCacheClient.get(query_ad_id_impression_key);
        Double query_ad_id_impression_val = 0.0;
        if (query_ad_id_impression_val_str != null && query_ad_id_impression_val_str!= "") {
            query_ad_id_impression_val = Double.parseDouble(query_ad_id_impression_val_str);
        }
        features.add(query_ad_id_impression_val);

        //query_ad_category_match scale to 1000000 if match
        double query_ad_category_match = 0.0;
        if(query_category == ad.category) {
            query_ad_category_match = 1000000.0;
        }
        features.add(query_ad_category_match);

        ad.pClick = CTRModel.getInstance("m_logistic_reg_model_file", "m_logistic_reg_model_file").predictCTRWithLogisticRegression(features);
        System.out.println("ad.pClick = " + ad.pClick);
    }
}
