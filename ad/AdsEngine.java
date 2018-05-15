package ad;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

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

    public List<Ad> selectAds(String query)
    {
        //query understanding
        List<String> queryTerms = QueryParser.getInstance().QueryUnderstand(query);
        //select ads candidates
        List<Ad> adsCandidates = AdsSelector.getInstance(mMemcachedServer, mMemcachedPortal, adService).selectAds(queryTerms);
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
