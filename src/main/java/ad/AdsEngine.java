package ad;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.*;
import service.AdService;
import service.GenericDaoHibernateImpl;

public class AdsEngine {
    private String mAdsDataFilePath;
    private String mBudgetFilePath;
    private IndexBuilder indexBuilder;
    private String mMemcachedServer;
    private int mMemcachedPortal;
    private AdService adService;
    private GenericDaoHibernateImpl<Campaign, Long> genericDaoHibernate;
    public AdsEngine(String adsDataFilePath, String budgetDataFilePath,String memcachedServer,int memcachedPortal, AdService adService, GenericDaoHibernateImpl genericDaoHibernate)
    {
        mAdsDataFilePath = adsDataFilePath;
        mBudgetFilePath = budgetDataFilePath;
        mMemcachedServer = memcachedServer;
        mMemcachedPortal = memcachedPortal;
        this.indexBuilder = new IndexBuilder(memcachedServer, memcachedPortal, adService, genericDaoHibernate);
        this.adService = adService;
        this.genericDaoHibernate = genericDaoHibernate;
    }

    public Boolean init()
    {
        //load ads data
        try {
            BufferedReader brAd = new BufferedReader(new FileReader(mAdsDataFilePath));
            String line;
            while ((line = brAd.readLine()) != null) {
                JSONObject obj = new JSONObject(line);
                Ad ad = new Ad();
                ad.detail_url = obj.getString("detail_url");
                ad.category = obj.getString("category");
                ad.price = obj.getDouble("price") == 0.0 ? Math.random() * 2000 : obj.getDouble("price");
                ad.title = obj.getString("title");
                ad.bidPrice = obj.getDouble("bidPrice");
                ad.campaignId = obj.getLong("campaignId");
                ad.brand = obj.getString("brand");
                ad.query = obj.getString("query");
                ad.query_group_id = obj.getInt("query_group_id");
                ad.thumbnail = obj.getString("thumbnail");
                ad.adId = obj.getLong("adId");;
                ad.keyWords = String.join(",",(obj.getString("title").split(" ")));
                indexBuilder.buildInvertIndex(ad);
                indexBuilder.buildForwardIndex(ad);

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("finish ad");

        //load budget data
        try  {
            BufferedReader brBudget = new BufferedReader(new FileReader(mBudgetFilePath));
            String line;
            while ((line = brBudget.readLine()) != null) {
                JSONObject campaignJson = new JSONObject(line);
                Long campaignId = campaignJson.getLong("campaignId");
                double budget = campaignJson.getDouble("budget");
                Campaign camp = new Campaign(campaignId, budget);

                if(!indexBuilder.updateBudget(camp))
                {
                    //log
                }
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("finish budget");
        return true;
    }

    public List<Ad> selectAds(String query, String device_id, String device_ip, String query_category)
    {
        //query understanding
        List<List<String>> rewrittenQuery = QueryParser.getInstance().QueryRewrite(query, mMemcachedServer, mMemcachedPortal);
        //select ads candidates
        Set<Long> uniquueAds = new HashSet<Long>();
        List<Ad> adsCandidates = new ArrayList<Ad>();

        //select ads candidates
        for (List<String> queryTerms : rewrittenQuery) {
            List<Ad> adsCandidates_temp = AdsSelector.getInstance(mMemcachedServer, mMemcachedPortal, adService).selectAds(queryTerms);
            for(Ad ad : adsCandidates_temp) {
                if (!uniquueAds.contains(ad.adId)) {
                    adsCandidates.add(ad);
                    uniquueAds.add(ad.adId);
                }
            }
        }
        //L0 filter by pClick, relevance score
        List<Ad> L0unfilteredAds = AdsFilter.getInstance().LevelZeroFilterAds(adsCandidates);
        System.out.println("L0unfilteredAds ads left = " + L0unfilteredAds.size());

        //L1 filter by relevance score : select top K ads
        int k = 20;
        List<Ad> unfilteredAds = AdsFilter.getInstance().LevelOneFilterAds(L0unfilteredAds,k);
        System.out.println("unfilteredAds ads left = " + unfilteredAds.size());

        //Dedupe ads per campaign
        List<Ad> dedupedAds = AdsCampaignManager.getInstance(genericDaoHibernate).DedupeByCampaignId(unfilteredAds);
        System.out.println("dedupedAds ads left = " + dedupedAds.size());


        List<Ad> ads = AdsCampaignManager.getInstance(genericDaoHibernate).ApplyBudget(dedupedAds);
        System.out.println("AdsCampaignManager ads left = " + ads.size());

        //allocation
        AdsAllocation.getInstance().AllocateAds(ads);
        return ads;
    }

}
