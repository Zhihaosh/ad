package ad;


import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import service.GenericDaoHibernateImpl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


public class AdsCampaignManager {
    private static AdsCampaignManager instance = null;
    private GenericDaoHibernateImpl<Campaign,Long> genericDaoHibernate;
    private static double minPriceThreshold = 0.0;
    protected AdsCampaignManager(GenericDaoHibernateImpl genericDaoHibernate)
    {
        ApplicationContext context = new ClassPathXmlApplicationContext("WEB-INF/services.xml");
        this.genericDaoHibernate = (GenericDaoHibernateImpl) context.getBean("CampaignDao");
    }
    public static AdsCampaignManager getInstance(GenericDaoHibernateImpl genericDaoHibernate) {
        if(instance == null) {
            instance = new AdsCampaignManager(genericDaoHibernate);
        }
        return instance;
    }

    public  List<Ad> DedupeByCampaignId(List<Ad> adsCandidates)
    {
        List<Ad> dedupedAds = new ArrayList<Ad>();
        HashSet<Long> campaignIdSet = new HashSet<Long>();
        for(Ad ad : adsCandidates)
        {
            if(!campaignIdSet.contains(ad.campaignId))
            {
                dedupedAds.add(ad);
                campaignIdSet.add((long)ad.campaignId);
            }
        }
        return dedupedAds;
    }

    public List<Ad> ApplyBudget(List<Ad> adsCandidates)
    {
        List<Ad> ads = new ArrayList<Ad>();
        try
        {
            for(int i = 0; i < adsCandidates.size()  - 1;i++)
            {
                Ad ad = adsCandidates.get(i);
                Long campaignId = ad.campaignId;
                System.out.println("campaignId: " + campaignId);
                Campaign campaign = (Campaign)genericDaoHibernate.read(ad.campaignId);
                if(campaign==null)
                    continue;
                Double budget = campaign.budget;
                System.out.println("AdsCampaignManager ad.costPerClick= " + ad.costPerClick);
                System.out.println("AdsCampaignManager campaignId= " + campaignId);
                System.out.println("AdsCampaignManager budget left = " + budget);

                if(ad.costPerClick <= budget && ad.costPerClick >= minPriceThreshold)
                {
                    ads.add(ad);
                    budget = budget - ad.costPerClick;
                    genericDaoHibernate.update(new Campaign(campaignId, budget));
                }
            }
        }catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return ads;
    }

}
