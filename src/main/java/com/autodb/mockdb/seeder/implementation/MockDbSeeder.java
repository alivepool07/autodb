package com.autodb.mockdb.seeder.implementation;


import com.autodb.mockdb.config.MockDbProperties;
import com.autodb.mockdb.provider.ValueProvider;
import com.autodb.mockdb.seeder.CollectionPopulatorService;
import com.autodb.mockdb.seeder.EntityCreatorService;
import com.autodb.mockdb.seeder.ReferenceResolverService;
import com.autodb.mockdb.seeder.Seeder;
import jakarta.persistence.EntityManager;

public class MockDbSeeder implements Seeder {

    private final EntityCreatorService entityCreator;
    private final ReferenceResolverService referenceResolver;
    private final CollectionPopulatorService collectionPopulator;

    public MockDbSeeder(EntityManager em, MockDbProperties props, ValueProvider provider) {
        EntityCreator creator = new EntityCreator(em, props, provider);
        this.entityCreator = creator;
        this.referenceResolver = new ReferenceResolver(em, props, creator.getCreatedEntities());
        this.collectionPopulator = new CollectionPopulator(em, creator.getCreatedEntities());
    }

    @Override
    public void seedAll() throws IllegalAccessException {

        entityCreator.createAll();

        referenceResolver.fixMissingReferences();

        collectionPopulator.populateCollections();

        collectionPopulator.populateManyToManyRelations();


    }
}
