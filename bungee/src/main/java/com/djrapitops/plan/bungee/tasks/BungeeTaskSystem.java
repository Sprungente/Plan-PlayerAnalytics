/*
 * License is provided in the jar as LICENSE also here:
 * https://github.com/Rsl1122/Plan-PlayerAnalytics/blob/master/Plan/src/main/resources/LICENSE
 */
package com.djrapitops.plan.bungee.tasks;

import com.djrapitops.plan.bungee.PlanBungee;
import com.djrapitops.plan.system.settings.Settings;
import com.djrapitops.plan.system.tasks.TaskSystem;
import com.djrapitops.plan.system.tasks.server.NetworkPageRefreshTask;
import com.djrapitops.plan.utilities.file.export.HtmlExport;
import com.djrapitops.plugin.api.TimeAmount;

/**
 * TaskSystem responsible for registering tasks for Bungee.
 *
 * @author Rsl1122
 */
public class BungeeTaskSystem extends TaskSystem {

    private final PlanBungee plugin;

    public BungeeTaskSystem(PlanBungee plugin) {
        super(plugin.getRunnableFactory(), new BungeeTPSCountTimer(plugin));
        this.plugin = plugin;
    }

    @Override
    public void enable() {
        registerTasks();
    }

    private void registerTasks() {
        registerTask("Enable Bungee Connector", new EnableConnectionTask()).runTaskAsynchronously();
        registerTask("TPS Count Timer", tpsCountTimer).runTaskTimerAsynchronously(1000, TimeAmount.SECOND.ticks());
        registerTask("Network Page Update", new NetworkPageRefreshTask()).runTaskTimerAsynchronously(1500, TimeAmount.MINUTE.ticks());
        if (Settings.ANALYSIS_EXPORT.isTrue()) {
            registerTask("Html Export", new HtmlExport(plugin)).runTaskAsynchronously();
        }
    }
}