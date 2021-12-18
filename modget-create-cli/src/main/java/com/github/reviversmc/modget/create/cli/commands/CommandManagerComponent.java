package com.github.reviversmc.modget.create.cli.commands;

import javax.inject.Singleton;

import com.github.reviversmc.modget.create.dependencies.DependencyModule;
import com.github.reviversmc.modget.create.github.TokenManagerModule;

import com.github.reviversmc.modget.create.manifests.ManifestModule;
import dagger.Component;
import dagger.Module;
import dagger.Reusable;

@Component(modules = {
        DependencyModule.class,
        ManifestModule.class,
        MGCCommandModule.class,
        MGCCommandModule.DefaultCommandNameModule.class,
        TokenManagerModule.class
})
@Singleton
public interface CommandManagerComponent {
    CommandManager getCommandManager();
}
