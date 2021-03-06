package com.djrapitops.plan.utilities.html.pages;

import com.djrapitops.plan.PlanPlugin;
import com.djrapitops.plan.data.container.Session;
import com.djrapitops.plan.data.store.keys.SessionKeys;
import com.djrapitops.plan.data.store.mutators.formatting.Formatter;
import com.djrapitops.plan.data.store.mutators.formatting.Formatters;
import com.djrapitops.plan.data.store.objects.DateHolder;
import com.djrapitops.plan.system.cache.SessionCache;
import com.djrapitops.plan.system.database.databases.Database;
import com.djrapitops.plan.system.info.connection.ConnectionLog;
import com.djrapitops.plan.system.info.connection.ConnectionSystem;
import com.djrapitops.plan.system.info.server.Server;
import com.djrapitops.plan.system.info.server.ServerInfo;
import com.djrapitops.plan.system.info.server.properties.ServerProperties;
import com.djrapitops.plan.system.webserver.cache.ResponseCache;
import com.djrapitops.plan.utilities.html.Html;
import com.djrapitops.plan.utilities.html.HtmlStructure;
import com.djrapitops.plan.utilities.html.icon.Icon;
import com.djrapitops.plan.utilities.html.structure.TabsElement;
import com.djrapitops.plugin.api.Benchmark;
import com.djrapitops.plugin.api.utility.log.ErrorLogger;
import com.djrapitops.plugin.api.utility.log.Log;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.*;

/**
 * Html parsing for the Debug page.
 *
 * @author Rsl1122
 */
public class DebugPage implements Page {

    @Override
    public String toHtml() {
        StringBuilder preContent = new StringBuilder();

        String issueLink = Html.LINK_EXTERNAL.parse("https://github.com/Rsl1122/Plan-PlayerAnalytics/issues/new", "Create new issue on Github");
        String hastebinLink = Html.LINK_EXTERNAL.parse("https://hastebin.com/", "Create a new hastebin paste");

        preContent.append("<p>")
                .append(HtmlStructure.separateWithDots(issueLink, hastebinLink)).append("<br><br>")
                .append("This page contains debug information for an issue ticket. You can copy it directly into the issue, the info is pre-formatted.")
                .append("</p>");

        TabsElement.Tab info = new TabsElement.Tab(Icon.called("server") + " Server Information", createServerInfoContent());
        TabsElement.Tab errors = new TabsElement.Tab(Icon.called("exclamation-circle") + " Errors", createErrorContent());
        TabsElement.Tab debugLog = new TabsElement.Tab(Icon.called("bug") + " Debug Log", createDebugLogContent());
        TabsElement.Tab caches = new TabsElement.Tab(Icon.called("archive") + " Plan Caches", createCacheContent());

        TabsElement tabs = new TabsElement(info, errors, debugLog, caches);

        return preContent + tabs.toHtmlFull();
    }

    private String createCacheContent() {
        StringBuilder content = new StringBuilder();
        appendResponseCache(content);
        appendSessionCache(content);
        return content.toString();
    }

    private void appendResponseCache(StringBuilder content) {
        try {
            content.append("<pre>### Cached Responses:<br><br>");
            List<String> cacheKeys = new ArrayList<>(ResponseCache.getCacheKeys());
            if (cacheKeys.isEmpty()) {
                content.append("Empty");
            }
            Collections.sort(cacheKeys);
            for (String cacheKey : cacheKeys) {
                content.append("- ").append(cacheKey).append("<br>");
            }
            content.append("</pre>");
        } catch (Exception e) {
            Log.toLog(this.getClass(), e);
        }
    }

    private void appendSessionCache(StringBuilder content) {
        try {
            content.append("<pre>### Session Cache:<br><br>");
            content.append("UUID | Session Started <br>")
                    .append("-- | -- <br>");
            Formatter<Long> timeStamp = Formatters.yearLongValue();
            Set<Map.Entry<UUID, Session>> sessions = SessionCache.getActiveSessions().entrySet();
            if (sessions.isEmpty()) {
                content.append("Empty");
            }
            for (Map.Entry<UUID, Session> entry : sessions) {
                UUID uuid = entry.getKey();
                String start = entry.getValue().getValue(SessionKeys.START).map(timeStamp).orElse("Unknown");
                content.append(uuid.toString()).append(" | ").append(start).append("<br>");
            }
            content.append("</pre>");
        } catch (Exception e) {
            Log.toLog(this.getClass(), e);
        }
    }

    private String createDebugLogContent() {
        StringBuilder content = new StringBuilder();
        appendDebugLog(content);
        return content.toString();
    }

    private String createErrorContent() {
        StringBuilder content = new StringBuilder();
        appendLoggedErrors(content);
        return content.toString();
    }

    private String createServerInfoContent() {
        StringBuilder content = new StringBuilder();

        appendServerInformation(content);
        appendConnectionLog(content);
        appendBenchmarks(content);

        return content.toString();
    }

    private void appendConnectionLog(StringBuilder content) {
        try {
            Map<String, Map<String, ConnectionLog.Entry>> logEntries = ConnectionLog.getLogEntries();

            content.append("<pre>### Connection Log:<br><br>");
            content.append("Server Address | Request Type | Response | Sent<br>")
                    .append("-- | -- | -- | --<br>");

            Formatter<DateHolder> formatter = Formatters.second();

            if (logEntries.isEmpty()) {
                content.append("**No Connections Logged**<br>");
            }
            for (Map.Entry<String, Map<String, ConnectionLog.Entry>> entry : logEntries.entrySet()) {
                String address = entry.getKey();
                Map<String, ConnectionLog.Entry> requests = entry.getValue();
                for (Map.Entry<String, ConnectionLog.Entry> requestEntry : requests.entrySet()) {
                    String infoRequest = requestEntry.getKey();
                    ConnectionLog.Entry logEntry = requestEntry.getValue();

                    content.append(address).append(" | ")
                            .append(infoRequest).append(" | ")
                            .append(logEntry.getResponseCode()).append(" | ")
                            .append(formatter.apply(logEntry)).append("<br>");
                }

            }
            content.append("</pre>");

            content.append("<pre>### Servers:<br><br>");
            List<Server> servers = ConnectionSystem.getInstance().getBukkitServers();
            content.append("Server Name | Address | UUID <br>")
                    .append("-- | -- | --<br>");
            for (Server server : servers) {
                content.append(server.getName()).append(" | ")
                        .append(server.getWebAddress()).append(" | ")
                        .append(server.getUuid()).append("<br>");
            }
            content.append("</pre>");

        } catch (Exception e) {
            Log.toLog(this.getClass(), e);
        }
    }

    private void appendServerInformation(StringBuilder content) {
        PlanPlugin plugin = PlanPlugin.getInstance();
        ServerProperties variable = ServerInfo.getServerProperties();

        content.append("<pre>### Server Information<br>")
                .append("**Plan Version:** ").append(plugin.getVersion()).append("<br>");

        content.append("**Server:** ");
        content.append(variable.getName())
                .append(" ").append(variable.getImplVersion())
                .append(" ").append(variable.getVersion());
        content.append("<br>");

        Database database = Database.getActive();
        content.append("**Database:** ").append(database.getName());
        content.append("<br><br>");

        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        Properties properties = System.getProperties();

        String osName = properties.getProperty("os.name");
        String osVersion = properties.getProperty("os.version");
        String osArch = properties.getProperty("os.arch");

        String javaVendor = properties.getProperty("java.vendor");
        String javaVersion = properties.getProperty("java.version");

        String javaVMVendor = properties.getProperty("java.vm.vendor");
        String javaVMName = properties.getProperty("java.vm.name");
        String javaVMVersion = properties.getProperty("java.vm.version");
        List<String> javaVMFlags = runtimeMxBean.getInputArguments();

        content.append("**Operating SubSystem:** ").append(osName).append(" (").append(osArch)
                .append(") version ").append(osVersion).append("<br>");

        content.append("**Java Version:** ").append(javaVersion).append(", ").append(javaVendor).append("<br>");
        content.append("**Java VM Version:** ").append(javaVMName).append(" version ").append(javaVMVersion)
                .append(", ").append(javaVMVendor).append("<br>");
        content.append("**Java VM Flags:** ").append(javaVMFlags).append("<br>");

        content.append("</pre>");
    }

    private void appendBenchmarks(StringBuilder content) {
        content.append("<pre>### Benchmarks<br>&#96;&#96;&#96;<br>");
        try {
            for (String line : Benchmark.getAverages().asStringArray()) {
                content.append(line).append("<br>");
            }
        } catch (Exception e) {
            content.append("Exception on Benchmark.getAverages().asStringArray()");
        }
        content.append("&#96;&#96;&#96;</pre>");
    }

    private void appendLoggedErrors(StringBuilder content) {
        try {
            content.append("<pre>### Logged Errors<br>");

            SortedMap<String, List<String>> errors = ErrorLogger.getLoggedErrors(PlanPlugin.getInstance());

            if (!errors.isEmpty()) {
                List<String> errorLines = new ArrayList<>();
                for (Map.Entry<String, List<String>> entry : errors.entrySet()) {
                    StringBuilder errorLineBuilder = new StringBuilder();
                    for (String line : entry.getValue()) {
                        errorLineBuilder.append(line).append("<br>");
                    }
                    String error = errorLineBuilder.toString();
                    if (!errorLines.contains(error)) {
                        errorLines.add(error);
                    }
                }
                for (String error : errorLines) {
                    content.append("</pre><pre>&#96;&#96;&#96;<br>")
                            .append(error)
                            .append("&#96;&#96;&#96;");
                }
            } else {
                content.append("**No Errors logged.**<br>");
            }
            content.append("</pre>");
        } catch (IOException e) {
            Log.toLog(this.getClass(), e);
        }
    }

    private void appendDebugLog(StringBuilder content) {
        content.append("<pre>### Debug Log<br>&#96;&#96;&#96;<br>");
        for (String line : Log.getDebugLogInMemory()) {
            content.append(line).append("<br>");
        }
        content.append("&#96;&#96;&#96;</pre>");
    }
}