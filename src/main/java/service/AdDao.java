package service;

import ad.Ad;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.Arrays;
import java.util.List;

public class AdDao {

    private  HibernateConnector hibernateConnector;
    private    Session currentSession;
    private    Transaction currentTransaction;

    public void setHibernateConnector(HibernateConnector hibernateConnector) {
        this.hibernateConnector = hibernateConnector;
    }

    public Session openCurrentSession() {
        currentSession = hibernateConnector.getSession();
        return currentSession;
    }

    public Session openCurrentSessionwithTransaction() {
        currentSession = hibernateConnector.getSession();
        currentTransaction = currentSession.beginTransaction();
        return currentSession;
    }
    public void closeCurrentSession(){
        currentSession.close();
    }

    public void closeCurrentSessionwithTransaction() {
        try {
            currentTransaction.commit();
            currentSession.close();
        }
        catch (Exception e){
            ;
        }
    }

    public void update(Ad ad) {
        getCurrentSession().update(ad);
    }

    private  Session getCurrentSession(){
        return currentSession;
    }

    public Ad findById(long id) {
        Ad book = (Ad) getCurrentSession().get(Ad.class, id);
        return book;
    }

    public void persist(Ad entity) {

        currentSession.saveOrUpdate(entity);

    }
    public List<Ad> findAll(){
        List<Ad> ads = (List<Ad>) getCurrentSession().createQuery("from ad").list();
        return ads;
    }
    public void  end(){
        hibernateConnector.end();
    }
}
