package ad;


import java.io.IOException;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.ConnectionFactoryBuilder;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.KStemFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import net.spy.memcached.MemcachedClient;
import service.AdService;
import service.GenericDao;
import service.GenericDaoHibernateImpl;

public class IndexBuilder {
    private int EXP = 72000; //0: never expire
    private String mMemcachedServer;
    private int mMemcachedPortal;
    private AdService addAdData;
    private GenericDaoHibernateImpl<Campaign, Long> genericDaoHibernate;
    private static int number = 1;

    public IndexBuilder(String memcachedServer,int memcachedPortal,AdService addAdData, GenericDaoHibernateImpl genericDaoHibernate)
    {
        mMemcachedServer = memcachedServer;
        mMemcachedPortal = memcachedPortal;
        this.addAdData = addAdData;
        this.genericDaoHibernate = genericDaoHibernate;
    }
    public Boolean buildInvertIndex(Ad ad)
    {
        System.out.println(number++);
        try
        {
            String keyWords = ad.keyWords;
            MemcachedClient cache  = new MemcachedClient(new ConnectionFactoryBuilder()
                    .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT).setOpTimeout(3000).build(),
                    AddrUtil.getAddresses("127.0.0.1:11211"));
            List<String> tokens = Utility.cleanedTokenize(keyWords);

            for(int i = 0; i < tokens.size();i++)
            {
                String key = tokens.get(i);
                try {
                    Object object = cache.get(key);
                    if(object != null)
                    {
                        @SuppressWarnings("unchecked")
                        Set<Long>  adIdList = (Set<Long>)cache.get(key);
                        adIdList.add(ad.adId);
                        cache.set(key, EXP, adIdList);
                    }
                    else
                    {
                        Set<Long>  adIdList = new HashSet<Long>();
                        adIdList.add(ad.adId);
                        cache.set(key, EXP, adIdList);
                    }
                }
                catch (Exception e){
                    i++;
                    continue;
                }


            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
        return true;
    }
    public Boolean buildForwardIndex(Ad ad)
    {
        try
        {
            addAdData.persist(ad);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
        return true;
    }
    public Boolean updateBudget(Campaign camp)
    {
        try
        {
            genericDaoHibernate.create(camp);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
