package com.github.reviversmc.modget.create.cli.commands;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;


@Module
public interface MGCCommandModule {
        @Binds
        @Singleton
        CommandManager commandManager(MGCCommandManager mgcCommandManager);

        @Binds
        @IntoSet
        Command createCommand(CreateCommand createCommand);

        @Binds
        @IntoSet
        Command exitCommand(ExitCommand exitCommand);

        @Binds
        @IntoSet
        Command helpCommand(HelpCommand helpCommand);

        @Binds
        @IntoSet
        Command loginCommand(LoginCommand loginCommand);

        @Binds
        @IntoSet
        Command versionCommand(VersionCommand versionCommand);

        @Module
        class DefaultCommandNameModule {
                @Provides
                @Named("default command name")
                public static String defaultCommandName() {
                        return "help";
                }
        }
}
