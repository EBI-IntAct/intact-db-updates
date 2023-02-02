package uk.ac.ebi.intact.update.context;

import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.persistence.spi.PersistenceProvider;

public class IntactHibernateJpaVendorAdapter extends HibernateJpaVendorAdapter {

    @Override
    public PersistenceProvider getPersistenceProvider() {
        return new IntactUpdatePersistenceProvider();
    }
}
