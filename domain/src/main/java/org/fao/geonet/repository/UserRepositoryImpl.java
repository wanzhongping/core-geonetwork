package org.fao.geonet.repository;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.fao.geonet.domain.User;
import org.fao.geonet.domain.User_;

/**
 * Implementation for all {@link User} queries that cannot be automatically generated by Spring-data.
 * 
 * @author Jesse
 */
public class UserRepositoryImpl implements UserRepositoryCustom {

    @PersistenceContext
    EntityManager _entityManager;
    @Override
    public User findOne(String userId) {
        return _entityManager.find(User.class, Integer.valueOf(userId));
    }
    @Override
    public List<User> findAllByEmail(String email) {
        CriteriaBuilder builder = _entityManager.getCriteriaBuilder();
        CriteriaQuery<User> query = builder.createQuery(User.class);
        Root<User> root = query.from(User.class);
        query.where(builder.in(root.get(User_.emailAddresses)));
        return _entityManager.createQuery(query).getResultList();
    }

}
