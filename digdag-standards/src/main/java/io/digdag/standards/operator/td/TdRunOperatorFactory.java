package io.digdag.standards.operator.td;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.treasuredata.client.model.TDJobSummary;
import io.digdag.client.config.Config;
import io.digdag.client.config.ConfigException;
import io.digdag.spi.Operator;
import io.digdag.spi.OperatorFactory;
import io.digdag.spi.SecretNotFoundException;
import io.digdag.spi.TaskExecutionContext;
import io.digdag.spi.TaskRequest;
import io.digdag.spi.TaskResult;
import io.digdag.util.BaseOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static io.digdag.standards.operator.td.TdOperatorFactory.buildStoreParams;
import static io.digdag.standards.operator.td.TdOperatorFactory.downloadJobResult;

public class TdRunOperatorFactory
        implements OperatorFactory
{
    private static Logger logger = LoggerFactory.getLogger(TdRunOperatorFactory.class);

    @Inject
    public TdRunOperatorFactory()
    { }

    public String getType()
    {
        return "td_run";
    }

    @Override
    public Operator newTaskExecutor(Path workspacePath, TaskRequest request)
    {
        return new TdRunOperator(workspacePath, request);
    }

    private class TdRunOperator
            extends BaseOperator
    {
        public TdRunOperator(Path workspacePath, TaskRequest request)
        {
            super(workspacePath, request);
        }

        @Override
        public List<String> secretSelectors()
        {
            return ImmutableList.of("td.apikey");
        }

        @Override
        public TaskResult runTask(TaskExecutionContext ctx)
        {
            Config params = request.getConfig().mergeDefault(
                    request.getConfig().getNestedOrGetEmpty("td"));

            // TODO: remove support for getting td apikey from params
            String apikey = ctx.secrets().getSecretOptional("td.apikey")
                    .or(() -> params.get("apikey", String.class).trim());

            String name = params.get("_command", String.class);
            Instant sessionTime = params.get("session_time", Instant.class);
            Optional<String> downloadFile = params.getOptional("download_file", String.class);
            boolean storeLastResults = params.get("store_last_results", boolean.class, false);
            boolean preview = params.get("preview", boolean.class, false);

            try (TDOperator op = TDOperator.fromConfig(params, apikey)) {
                TDJobOperator j = op.startSavedQuery(name, Date.from(sessionTime));
                logger.info("Started a saved query name={} with time={}", name, sessionTime);

                TDJobSummary summary = j.joinJob();
                downloadJobResult(j, workspace, downloadFile);

                if (preview) {
                    try {
                        TdOperatorFactory.downloadPreviewRows(j, "job id " + j.getJobId());
                    }
                    catch (Exception ex) {
                        logger.info("Getting rows for preview failed. Ignoring this error.", ex);
                    }
                }

                Config storeParams = buildStoreParams(request.getConfig().getFactory(), j, summary, storeLastResults);

                return TaskResult.defaultBuilder(request)
                        .storeParams(storeParams)
                        .build();
            }
        }
    }
}
