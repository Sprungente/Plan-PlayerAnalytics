/*
 * Licence is provided in the jar as license.yml also here:
 * https://github.com/Rsl1122/Plan-PlayerAnalytics/blob/master/Plan/src/main/resources/license.yml
 */
package com.djrapitops.pluginbridge.plan.jobs;

import com.djrapitops.plan.data.element.AnalysisContainer;
import com.djrapitops.plan.data.element.InspectContainer;
import com.djrapitops.plan.data.element.TableContainer;
import com.djrapitops.plan.data.plugin.ContainerSize;
import com.djrapitops.plan.data.plugin.PluginData;
import com.djrapitops.plan.utilities.FormatUtils;
import com.djrapitops.plan.utilities.html.icon.Color;
import com.djrapitops.plan.utilities.html.icon.Icon;
import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.dao.JobsDAOData;

import java.util.*;
import java.util.stream.Collectors;

/**
 * PluginData for Jobs plugin.
 *
 * @author Rsl1122
 */
public class JobsData extends PluginData {

    public JobsData() {
        super(ContainerSize.THIRD, "Jobs");
        setPluginIcon(Icon.called("suitcase").of(Color.BROWN).build());
    }

    @Override
    public InspectContainer getPlayerData(UUID uuid, InspectContainer inspectContainer) {
        List<JobsDAOData> playersJobs = Jobs.getDBManager().getDB().getAllJobs(null, uuid);

        TableContainer jobTable = new TableContainer(
                getWithIcon("Job", Icon.called("suitcase")),
                getWithIcon("Level", Icon.called("plus")));
        for (JobsDAOData job : playersJobs) {
            jobTable.addRow(job.getJobName(), job.getLevel());
        }
        if (playersJobs.isEmpty()) {
            jobTable.addRow("No Jobs");
        }
        inspectContainer.addTable("jobTable", jobTable);

        return inspectContainer;
    }

    @Override
    public AnalysisContainer getServerData(Collection<UUID> collection, AnalysisContainer analysisContainer) {
        List<JobsDAOData> allJobs = Jobs.getDBManager().getDB().getAllJobs()
                .values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        TableContainer jobTable = new TableContainer(
                getWithIcon("Job", Icon.called("suitcase")),
                getWithIcon("Workers", Icon.called("users")),
                getWithIcon("Total Level", Icon.called("plus")),
                getWithIcon("Average Level", Icon.called("plus"))
        );

        if (allJobs.isEmpty()) {
            jobTable.addRow("No Jobs with Workers");
        } else {
            Map<String, Integer> workers = new HashMap<>();
            Map<String, Long> totals = new HashMap<>();
            for (JobsDAOData data : allJobs) {
                String job = data.getJobName();
                int level = data.getLevel();
                workers.put(job, workers.getOrDefault(job, 0) + 1);
                totals.put(job, totals.getOrDefault(job, 0L) + level);
            }

            List<String> order = new ArrayList<>(workers.keySet());
            Collections.sort(order);

            for (String job : order) {
                int amountOfWorkers = workers.getOrDefault(job, 0);
                long totalLevel = totals.getOrDefault(job, 0L);
                jobTable.addRow(
                        job,
                        amountOfWorkers,
                        totalLevel,
                        amountOfWorkers != 0 ? FormatUtils.cutDecimals(totalLevel / amountOfWorkers) : "-"
                );
            }
        }
        analysisContainer.addTable("jobTable", jobTable);

        return analysisContainer;
    }
}