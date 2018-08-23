package com.djrapitops.plan.modules.server.bukkit;

import com.djrapitops.plan.system.database.BukkitDBSystem;
import com.djrapitops.plan.system.database.DBSystem;
import com.djrapitops.plan.system.listeners.BukkitListenerSystem;
import com.djrapitops.plan.system.listeners.ListenerSystem;
import com.djrapitops.plan.system.settings.config.BukkitConfigSystem;
import com.djrapitops.plan.system.settings.config.ConfigSystem;
import com.djrapitops.plan.system.tasks.BukkitTaskSystem;
import com.djrapitops.plan.system.tasks.TaskSystem;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

/**
 * Module for binding Bukkit specific classes to the interface implementations.
 *
 * @author Rsl1122
 */
@Module
public class BukkitSuperClassBindingModule {

    @Provides
    @Singleton
    DBSystem provideBukkitDatabaseSystem(BukkitDBSystem dbSystem) {
        return dbSystem;
    }

    @Provides
    @Singleton
    ConfigSystem provideBukkitConfigSystem(BukkitConfigSystem bukkitConfigSystem) {
        return bukkitConfigSystem;
    }

    @Provides
    @Singleton
    TaskSystem provideBukkitTaskSystem(BukkitTaskSystem bukkitTaskSystem) {
        return bukkitTaskSystem;
    }

    @Provides
    @Singleton
    ListenerSystem provideBukkitListenerSystem(BukkitListenerSystem bukkitListenerSystem) {
        return bukkitListenerSystem;
    }

}