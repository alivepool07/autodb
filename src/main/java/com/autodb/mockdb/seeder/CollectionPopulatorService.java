package com.autodb.mockdb.seeder;

public interface CollectionPopulatorService {
    void populateCollections() throws IllegalAccessException;
    void populateManyToManyRelations() throws IllegalAccessException;
}
