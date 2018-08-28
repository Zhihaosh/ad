package service;

import ad.Campaign;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.io.Serializable;

public class GenericDaoHibernateImpl <T, PK extends Serializable>
        implements GenericDao<T, PK> {
    private Class<T> type;
    private HibernateConnector hibernateConnector;
    private Transaction transaction;
    public GenericDaoHibernateImpl(Class<T> type) {
        this.type = type;
    }

    public void create(T o) {
        Session session = getSession();
        transaction = session.beginTransaction();
        session.saveOrUpdate(o);
        System.out.println("save before");
        transaction.commit();
        System.out.println("save after");
        session.close();
        System.out.println("close");

    }

    public T read(PK id) {
        Session session = getSession();
        System.out.println("asd");
        Campaign val = session.get(Campaign.class, id);
        session.close();
        return (T) val;
    }

    public void update(T o) {
        Session session = getSession();
        transaction = session.beginTransaction();
        getSession().update(o);
        transaction.commit();
        session.close();

    }

    public void delete(T o) {
        Session session = getSession();

        transaction = session.beginTransaction();
        session.delete(o);
        transaction.commit();
        session.close();
    }
    public  void end(){
        hibernateConnector.end();
    }
    public Session getSession(){
        return this.hibernateConnector.getSession();
    }

    public void setSessionFactory(HibernateConnector hibernateConnector) {
        this.hibernateConnector = hibernateConnector;
    }
}