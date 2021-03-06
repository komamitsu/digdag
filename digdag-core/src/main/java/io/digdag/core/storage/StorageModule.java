package io.digdag.core.storage;

import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Binder;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import io.digdag.spi.StorageFactory;

public class StorageModule
        implements Module
{
    @Override
    public void configure(Binder binder)
    {
        binder.bind(StorageManager.class).in(Scopes.SINGLETON);
        binder.bind(ArchiveManager.class).in(Scopes.SINGLETON);
        Multibinder.newSetBinder(binder, StorageFactory.class);
    }
}
