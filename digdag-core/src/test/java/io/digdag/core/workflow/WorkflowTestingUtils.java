package io.digdag.core.workflow;

import com.google.inject.Inject;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import io.digdag.core.LocalSecretAccessPolicy;
import io.digdag.core.SecretCrypto;
import io.digdag.core.SecretCryptoProvider;
import io.digdag.core.database.DatabaseSecretStoreManager;
import io.digdag.spi.CommandExecutor;
import io.digdag.spi.SchedulerFactory;
import io.digdag.spi.OperatorFactory;
import io.digdag.core.DigdagEmbed;
import io.digdag.core.database.DatabaseConfig;
import io.digdag.spi.SecretAccessPolicy;
import io.digdag.spi.SecretStoreManager;

import static io.digdag.core.database.DatabaseTestingUtils.cleanDatabase;
import static io.digdag.core.database.DatabaseTestingUtils.getEnvironmentDatabaseConfig;

public class WorkflowTestingUtils
{
    private WorkflowTestingUtils() { }

    public static DigdagEmbed setupEmbed()
    {
        DigdagEmbed embed = new DigdagEmbed.Bootstrap()
            .withExtensionLoader(false)
            .addModules((binder) -> {
                binder.bind(CommandExecutor.class).to(SimpleCommandExecutor.class).in(Scopes.SINGLETON);

                binder.bind(SecretCrypto.class).toProvider(SecretCryptoProvider.class).in(Scopes.SINGLETON);
                binder.bind(SecretStoreManager.class).to(DatabaseSecretStoreManager.class).in(Scopes.SINGLETON);
                binder.bind(SecretAccessPolicy.class).to(LocalSecretAccessPolicy.class);

                Multibinder<SchedulerFactory> schedulerFactoryBinder = Multibinder.newSetBinder(binder, SchedulerFactory.class);

                Multibinder<OperatorFactory> operatorFactoryBinder = Multibinder.newSetBinder(binder, OperatorFactory.class);
                operatorFactoryBinder.addBinding().to(NoopOperatorFactory.class).in(Scopes.SINGLETON);
                operatorFactoryBinder.addBinding().to(EchoOperatorFactory.class).in(Scopes.SINGLETON);
                operatorFactoryBinder.addBinding().to(FailOperatorFactory.class).in(Scopes.SINGLETON);
            })
            .overrideModulesWith((binder) -> {
                binder.bind(DatabaseConfig.class).toInstance(getEnvironmentDatabaseConfig());
            })
            .initializeWithoutShutdownHook();
        cleanDatabase(embed);
        return embed;
    }
}
