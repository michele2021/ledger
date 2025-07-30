package com.accounting.core.config.guice;

import com.accounting.commons.objectstorage.*;
import com.accounting.core.config.AccountingCoreAppConfig;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class ObjectStorageModule extends AbstractModule {

    private final AccountingCoreAppConfig config;

    public ObjectStorageModule(AccountingCoreAppConfig config) {
        this.config = config;
    }

    @Override
    protected void configure() {
        // Bind interfaces to implementations
        bind(FileObjectStorage.class)
                .to(FileObjectStorageImpl.class)
                .in(Scopes.SINGLETON);

        bind(TextNoteStorage.class)
                .to(JournalNoteStorageImpl.class)
                .in(Scopes.SINGLETON);

        if (config.getRepositoryConfig().isUseAWSS3()) {
            bind(ObjectStorage.class).to(S3FilesManagerImpl.class);
        } else {
            bind(ObjectStorage.class).to(DatabaseObjectStorageImpl.class);
        }

        // Bind utility classes
        bind(HashCalculator.class).in(Scopes.SINGLETON);
    }
}